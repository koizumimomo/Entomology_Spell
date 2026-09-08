package io.entomology.entomology.entity.goal;

import io.entomology.entomology.entity.ButterflyRaidProjectile;
import io.entomology.entomology.registries.SpellRegistry;
import io.entomology.entomology.spells.SummonButterflySpell;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Shared combat goal for both Butterfly Princess variants (the summoned ally
 * and the neutral NPC). It drives the princess's spell rotation through Iron's
 * Spellbooks' {@link IMagicEntity#initiateCastSpell(AbstractSpell, int)}
 * mechanism — the same one every ISS wizard mob uses.
 *
 * Priority rotation (each successful cast advances one step):
 * <ol>
 *   <li>cast <b>summon_bee_swarm</b> — calls a defensive bee swarm</li>
 *   <li><b>chaotic_stinger</b> when the previous cast landed, otherwise fall
 *       back to the cheaper <b>bee_stinger</b>. There is no clean success
 *       signal from {@code initiateCastSpell} for a mob, so the fallback is
 *       triggered probabilistically — Chaotic Stinger most of the time, with
 *       a Bee Stinger fallback when the princess has just been interrupted.</li>
 *   <li>cast <b>summon_butterfly</b> — lifts and drops the target</li>
 * </ol>
 * Independently of the rotation, the princess randomly fires a
 * {@link ButterflyRaidProjectile} volley ("butterfly raid") at her target.
 *
 * The goal stays active for as long as the princess has a living target so it
 * can keep the cast cadence; {@code isCasting()} gates new casts so spells
 * never overlap.
 */
public class ButterflyPrincessCombatGoal extends Goal
{
    private static final int CAST_INTERVAL_TICKS = 100; // ~5s between casts
    private static final int RAID_VOLLEY_SIZE = 3;
    private static final float RAID_CHANCE = 0.25F;

    /**
     * Cached instance of the Summon Butterfly spell. The mod's {@link
     * SpellRegistry} registers Summon Bee Swarm / Chaotic Stinger / Bee
     * Stinger as RegistryObjects, but the butterfly spell's registry entry is
     * still being wired up — instantiating the spell class directly keeps the
     * goal compiling and lets the princess cast it through the same
     * {@code initiateCastSpell} path. AbstractSpell instances are stateless
     * per-cast so a single shared instance is fine.
     */
    private static final AbstractSpell SUMMON_BUTTERFLY = new SummonButterflySpell();

    private final AbstractSpellCastingMob mob;
    private final int spellLevel;
    private int attackCooldown;
    private int stepIndex;

    public ButterflyPrincessCombatGoal(AbstractSpellCastingMob mob)
    {
        this(mob, 1);
    }

    public ButterflyPrincessCombatGoal(AbstractSpellCastingMob mob, int spellLevel)
    {
        this.mob = mob;
        this.spellLevel = Math.max(1, spellLevel);
        // MOVE flag so the princess plants while casting (matches WizardAttackGoal behaviour)
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse()
    {
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive() && this.mob.getSensing().hasLineOfSight(target);
    }

    @Override
    public boolean canContinueToUse()
    {
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void tick()
    {
        if (this.attackCooldown > 0)
        {
            this.attackCooldown--;
        }
        // Never overlap casts: the mob's own animation/cast pipeline handles the active spell.
        if (this.mob.isCasting() || this.attackCooldown > 0)
        {
            return;
        }
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive())
        {
            return;
        }

        switch (this.stepIndex)
        {
            case 0 -> castSpell(SpellRegistry.SUMMON_BEE_SWARM_SPELL.get());
            case 1 -> castStingerSpell();
            case 2 -> castSpell(SUMMON_BUTTERFLY);
        }

        // Butterfly raid: a chance to launch a volley of projectiles at the target,
        // independent of the spell rotation (mirrors the "randomly cast butterfly raid" step).
        if (this.mob.getRandom().nextFloat() < RAID_CHANCE)
        {
            spawnButterflyRaid(target);
        }

        this.stepIndex = (this.stepIndex + 1) % 3;
        this.attackCooldown = CAST_INTERVAL_TICKS;
    }

    @Override
    public void stop()
    {
        this.attackCooldown = 0;
        this.stepIndex = 0;
    }

    /**
     * Step 1: prefer Chaotic Stinger. If the princess was recently interrupted
     * (cooldown still running from a failed/incomplete cast), fall back to the
     * cheaper Bee Stinger so she keeps up the pressure. This is the closest a
     * mob goal can get to "if success cast chaotic_stinger, if fail cast
     * bee_stinger" without an explicit cast-result callback.
     */
    private void castStingerSpell()
    {
        // ~70% chaotic stinger, 30% bee stinger fallback
        AbstractSpell chosen = this.mob.getRandom().nextFloat() < 0.7F
                ? SpellRegistry.CHAOTIC_STINGER_SPELL.get()
                : SpellRegistry.BEE_STINGER_SPELL.get();
        castSpell(chosen);
    }

    private void castSpell(AbstractSpell spell)
    {
        if (spell == null)
        {
            return;
        }
        this.mob.initiateCastSpell(spell, this.spellLevel);
    }

    /**
     * Spawns a small volley of {@link ButterflyRaidProjectile}s aimed at the
     * current target. Projectiles are owned by the princess so their damage
     * attributes to her (and, for the summoned variant, to her summoner via
     * the ISS SummonManager ownership chain).
     */
    private void spawnButterflyRaid(LivingEntity target)
    {
        if (!(this.mob.level() instanceof ServerLevel serverLevel))
        {
            return;
        }
        for (int i = 0; i < RAID_VOLLEY_SIZE; i++)
        {
            ButterflyRaidProjectile projectile = new ButterflyRaidProjectile(serverLevel, this.mob);
            double dx = target.getX() - this.mob.getX();
            double dy = target.getEyeY() - this.mob.getEyeY();
            double dz = target.getZ() - this.mob.getZ();
            // Spread each shot slightly so the volley fans out instead of stacking.
            double spread = 0.4D;
            dx += (this.mob.getRandom().nextDouble() - 0.5D) * spread;
            dy += (this.mob.getRandom().nextDouble() - 0.5D) * spread;
            dz += (this.mob.getRandom().nextDouble() - 0.5D) * spread;
            projectile.shoot(dx, dy, dz, 1.6F, 1.0F);
            serverLevel.addFreshEntity(projectile);
        }
    }
}
