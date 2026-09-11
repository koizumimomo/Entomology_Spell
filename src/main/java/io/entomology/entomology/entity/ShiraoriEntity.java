package io.entomology.entomology.entity;

import io.entomology.entomology.entity.goal.ShiraoriCombatGoal;
import io.entomology.entomology.registries.EntityRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.NeutralWizard;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericHurtByTargetGoal;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import javax.annotation.Nullable;

/**
 * Shiraori the Spider Mother (白织·蜘蛛之母) — a 魔物娘 wizard mob summoned by the
 * Summon Shiraori spell. She is a {@link NeutralWizard} that fights for her
 * summoner for 5 minutes (6000 ticks, managed by {@link SummonManager#initSummon}),
 * casting web-entangle roots via {@link ShiraoriCombatGoal}, while
 * {@link io.entomology.entomology.entity.goal.ShiraoriBroodGoal} drives her
 * brood behaviour: two startup guard nests, periodic insect egg laying and a
 * Guardian Spider summon every 30 seconds.
 *
 * <p>Lifecycle / ownership mirrors {@link SummonedButterflyPrincessEntity}:
 * <ul>
 *   <li>Ownership is tracked through {@link SummonManager#setOwner}, so damage
 *       she deals with her spells is attributed back to the player summoner via
 *       {@link SummonManager#getOwner}.</li>
 *   <li>She never attacks her summoner or other same-owner summons (target
 *       goals exclude them, see {@link #isValidTarget}).</li>
 *   <li>The {@link IMagicSummon} defaults drive the un-summon / death / remove
 *       cleanup hooks that keep SummonManager's bookkeeping in sync.</li>
 * </ul>
 *
 * <p>She reuses the ISS wizard GeckoLib pipeline, but is drawn with the
 * shiraori geometry via {@link ShiraoriModel}.
 */
public class ShiraoriEntity extends NeutralWizard implements IMagicSummon
{
    public ShiraoriEntity(EntityType<? extends io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob> entityType, Level level)
    {
        super(entityType, level);
        this.setPersistenceRequired();
    }

    public ShiraoriEntity(Level level, @Nullable LivingEntity owner)
    {
        this(EntityRegistry.SHIRAORI.get(), level);
        if (owner != null)
        {
            SummonManager.setOwner(this, owner);
        }
        this.setHealth(this.getMaxHealth());
    }

    /**
     * Persists whether the two startup guard nests have already been spawned,
     * so reloading the chunk does not let Shiraori summon another pair.
     */
    private boolean broodNestsSummoned = false;

    public boolean isBroodNestsSummoned()
    {
        return this.broodNestsSummoned;
    }

    public void setBroodNestsSummoned(boolean value)
    {
        this.broodNestsSummoned = value;
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        // Boss-level: 200 health (100 hearts), 4 armor, 0.35 movement speed, 8 attack damage.
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals()
    {
        this.goalSelector.addGoal(2, new ShiraoriCombatGoal(this));
        // Always-running brood logic: 2 startup nests, then eggs + 30s guardian spider
        this.goalSelector.addGoal(3, new io.entomology.entomology.entity.goal.ShiraoriBroodGoal(this));
        // No follow/teleport goal: Shiraori is a stationary nest-mother — she
        // stays where she was summoned and guards the area with her brood.
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 32.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(0, new GenericHurtByTargetGoal(this, this::isAlly).setAlertOthers());
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Mob.class, 20, true, false, this::isValidTarget));
    }

    /**
     * Ally check used by every targeting path. A target is an ally (never
     * attacked) when it is:
     * <ul>
     *   <li>Shiraori herself,</li>
     *   <li>her summoner (the player who summoned her),</li>
     *   <li>any summon whose SummonManager owner is the summoner — this covers
     *       every mob hatched from her own insect eggs (the eggs inherit her
     *       summoner's UUID), her guardian spiders and the player's other
     *       summons — or whose owner is Shiraori directly (eggs fall back to
     *       her when the summoner is unavailable),</li>
     *   <li>a tamed pet of the same owner, or a scoreboard ally.</li>
     * </ul>
     * Comparison is done by UUID (not entity identity) so the check stays
     * correct across entity lookups / reloads.
     */
    public boolean isAlly(@Nullable LivingEntity target)
    {
        if (target == null || target == this)
        {
            return true;
        }
        if (io.entomology.entomology.util.SwarmCreatures.isShiraoriAttendant(target))
        {
            return true;
        }
        java.util.UUID targetId = target.getUUID();
        if (targetId.equals(this.getUUID()))
        {
            return true;
        }
        Entity owner = this.getSummoner();
        java.util.UUID ownerId = owner != null ? owner.getUUID() : null;
        if (ownerId != null && targetId.equals(ownerId))
        {
            return true;
        }
        // Magic summons owned by the summoner (egg hatchlings) or directly by Shiraori
        Entity targetOwner = SummonManager.getOwner(target);
        if (targetOwner != null
                && (targetOwner.getUUID().equals(this.getUUID())
                        || (ownerId != null && targetOwner.getUUID().equals(ownerId))))
        {
            return true;
        }
        if (target instanceof net.minecraft.world.entity.OwnableEntity ownable)
        {
            java.util.UUID petOwnerId = ownable.getOwnerUUID();
            if (petOwnerId != null
                    && (petOwnerId.equals(this.getUUID())
                            || (ownerId != null && petOwnerId.equals(ownerId))))
            {
                return true;
            }
        }
        return owner != null && (owner.isAlliedTo(target) || target.isAlliedTo(owner));
    }

    /**
     * Target predicate for the nearest-target goal: hostile mobs (or swarm
     * exemption marked entities) that are not allies. Keeps Shiraori from
     * turning on her summoner, her eggs' hatchlings and same-owner summons.
     */
    private boolean isValidTarget(@Nullable LivingEntity target)
    {
        if (target == null || !target.isAlive() || isAlly(target))
        {
            return false;
        }
        // Go after genuinely hostile mobs (creepers, zombies, enemy players via PvP, etc.)
        // or any entity the caster marked with Swarm Exemption (swarm_exemption rallies
        // every summon against the marked target).
        return target instanceof Enemy
                || target.hasEffect(io.entomology.entomology.registries.EffectRegistry.SWARM_EXEMPTION.get());
    }

    /**
     * Hard gate for every AI path that can assign a target (nearest-target,
     * hurt retaliation, alert): allies are refused unconditionally, so even a
     * stray hit from an egg hatchling can never make Shiraori web it.
     */
    @Override
    public void setTarget(@Nullable LivingEntity target)
    {
        if (target != null && isAlly(target))
        {
            return;
        }
        super.setTarget(target);
    }

    // ---- GeckoLib animation ----
    // shiraori.geo.json has: animation, animation2, animation3, Sit, jump_start, walk, idle
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.shiraori.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.shiraori.walk");
    private static final RawAnimation CAST = RawAnimation.begin().thenPlay("animation.shiraori.animation2");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers)
    {
        controllers.add(new AnimationController<>(this, "shiraori_controller", 5, state ->
        {
            if (this.isCasting())
            {
                state.setAnimation(CAST);
            }
            else if (state.isMoving())
            {
                state.setAnimation(WALK);
            }
            else
            {
                state.setAnimation(IDLE);
            }
            return PlayState.CONTINUE;
        }));
    }

    // ---- Summon lifecycle ----

    @Override
    public void onUnSummon()
    {
        if (!this.level().isClientSide)
        {
            io.redspace.ironsspellbooks.capabilities.magic.MagicManager.spawnParticles(
                    this.level(), ParticleTypes.HAPPY_VILLAGER,
                    this.getX(), this.getY() + 0.5D, this.getZ(),
                    24, 0.4D, 0.5D, 0.4D, 0.05D, false);
            this.discard();
        }
    }

    @Override
    public void die(DamageSource damageSource)
    {
        this.onDeathHelper();
        super.die(damageSource);
    }

    @Override
    public void onRemovedFromWorld()
    {
        this.onRemovedHelper(this);
        super.onRemovedFromWorld();
    }

    @Override
    public boolean removeWhenFarAway(double distance)
    {
        return false;
    }

    @Override
    public boolean canBeLeashed(Player player)
    {
        return false;
    }

    // ---- Cross-world restore handling ----
    // Persistent summons follow the player across worlds (Iron's Spellbooks
    // saves them on logout and rebuilds them on login), but should vanish
    // immediately once restored, matching vanilla Iron's behaviour.

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("BroodNestsSummoned", this.broodNestsSummoned);
        SummonedRestorationHelper.writeRestoreFlag(this, tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        this.broodNestsSummoned = tag.getBoolean("BroodNestsSummoned");
        SummonedRestorationHelper.readRestoreFlag(this, tag);
    }

    @Override
    public void tick()
    {
        super.tick();
        SummonedRestorationHelper.handleRestoredDespawn(this);
    }
}
