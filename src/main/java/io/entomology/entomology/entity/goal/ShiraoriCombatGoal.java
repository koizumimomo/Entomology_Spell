package io.entomology.entomology.entity.goal;

import io.entomology.entomology.registries.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Combat goal for Shiraori the Spider Mother. While she has a visible living
 * target, she casts <b>Web Entangle</b> on it every 3-5 seconds (80 ticks +
 * 0-40 jitter), rooting the target in place for 1 second.
 *
 * <p>Her non-combat brood behaviour (startup guard nests, egg laying and the
 * 30-second guardian spider summon) lives in {@link ShiraoriBroodGoal}, which
 * runs independently of combat.
 *
 * <p>Casts are gated by {@code isCasting()} so spells never overlap; the goal
 * plants Shiraori while active (MOVE flag), matching Iron's Spellbooks'
 * WizardAttackGoal behaviour.
 */
public class ShiraoriCombatGoal extends Goal
{
    private static final int CAST_INTERVAL_TICKS = 80; // ~4s between web entangle casts
    private final AbstractSpellCastingMob mob;
    private final int spellLevel;
    private int webEntangleCooldown;

    public ShiraoriCombatGoal(AbstractSpellCastingMob mob)
    {
        this(mob, 1);
    }

    public ShiraoriCombatGoal(AbstractSpellCastingMob mob, int spellLevel)
    {
        this.mob = mob;
        this.spellLevel = Math.max(1, spellLevel);
        // MOVE flag so Shiraori plants while casting (matches WizardAttackGoal behaviour)
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
        if (this.webEntangleCooldown > 0)
        {
            this.webEntangleCooldown--;
        }

        // Never overlap casts: the mob's own animation/cast pipeline handles the active spell.
        if (this.mob.isCasting())
        {
            return;
        }

        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive())
        {
            return;
        }

        // Cast Web Entangle on target (no damage, just root for 1s)
        if (this.webEntangleCooldown <= 0)
        {
            AbstractSpell webEntangle = SpellRegistry.WEB_ENTANGLE_SPELL.get();
            if (webEntangle != null)
            {
                this.mob.initiateCastSpell(webEntangle, this.spellLevel);
            }
            this.webEntangleCooldown = CAST_INTERVAL_TICKS + this.mob.getRandom().nextInt(40); // 3-5s
        }
    }

    @Override
    public void stop()
    {
        this.webEntangleCooldown = 0;
    }
}
