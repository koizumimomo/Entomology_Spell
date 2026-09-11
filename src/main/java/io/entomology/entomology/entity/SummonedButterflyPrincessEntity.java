package io.entomology.entomology.entity;

import io.entomology.entomology.entity.goal.ButterflyPrincessCombatGoal;
import io.entomology.entomology.registries.EntityRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.NeutralWizard;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericFollowOwnerGoal;
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
 * Summoned Butterfly Princess — the spell-summoned variant of the Butterfly
 * Princess. She is a {@link NeutralWizard} that fights for her summoner for
 * 5 minutes (6000 ticks, managed by {@link SummonManager#initSummon}), casting
 * the swarm-school insect spells defined in {@link ButterflyPrincessCombatGoal}.
 *
 * <p>Lifecycle / ownership:
 * <ul>
 *   <li>Ownership is tracked through {@link SummonManager#setOwner}, so damage
 *       she deals with her spells is attributed back to the player summoner
 *       via {@link SummonManager#getOwner}.</li>
 *   <li>She never attacks her summoner or other same-owner summons (target
 *       goals exclude them, see {@link #isValidTarget}).</li>
 *   <li>The {@link IMagicSummon} defaults drive the un-summon / death / remove
 *       cleanup hooks that keep SummonManager's bookkeeping in sync.</li>
 * </ul>
 *
 * <p>She reuses the ISS wizard GeckoLib pipeline (model/animation/controllers
 * are inherited from {@link io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob}),
 * but is drawn with the butterfly_princess geometry via {@link ButterflyPrincessModel}.
 */
public class SummonedButterflyPrincessEntity extends NeutralWizard implements IMagicSummon
{
    public SummonedButterflyPrincessEntity(EntityType<? extends io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob> entityType, Level level)
    {
        super(entityType, level);
        this.setPersistenceRequired();
    }

    public SummonedButterflyPrincessEntity(Level level, @Nullable LivingEntity owner)
    {
        this(EntityRegistry.SUMMONED_BUTTERFLY_PRINCESS.get(), level);
        if (owner != null)
        {
            SummonManager.setOwner(this, owner);
        }
        this.setHealth(this.getMaxHealth());
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        // 36 health (18 hearts), 4 armor (like wearing a full iron chestpiece).
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 36.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.45D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals()
    {
        // Combat rotation: summon_bee_swarm -> chaotic/bee stinger -> summon_butterfly + butterfly raid
        this.goalSelector.addGoal(2, new ButterflyPrincessCombatGoal(this));
        // Stay near the summoner when idle
        this.goalSelector.addGoal(4, new GenericFollowOwnerGoal(this, this::getSummoner, 1.0, 9.0F, 4.0F, true, 20.0F));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 32.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        // Retaliate when hurt, but never against the summoner or their allies.
        this.targetSelector.addGoal(0, new GenericHurtByTargetGoal(this, entity -> entity == this.getSummoner()).setAlertOthers());
        // Seek out nearby hostile mobs (never the summoner or same-owner summons).
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Mob.class, 20, true, false, this::isValidTarget));
    }

    /**
     * Target predicate: any living hostile mob that is not the summoner, not a
     * same-owner summon, and not scoreboard-allied with the summoner. This is
     * what keeps the princess from turning on her owner's other summons.
     */
    private boolean isValidTarget(@Nullable LivingEntity target)
    {
        if (target == null || !target.isAlive() || target == this)
        {
            return false;
        }
        if (io.entomology.entomology.util.SwarmCreatures.isShiraoriAttendant(target))
        {
            return false;
        }
        Entity owner = this.getSummoner();
        if (owner == null || target == owner)
        {
            return false;
        }
        if (target instanceof IMagicSummon summon && summon.getSummoner() == owner)
        {
            return false;
        }
        if (target instanceof net.minecraft.world.entity.OwnableEntity ownable && ownable.getOwner() == owner)
        {
            return false;
        }
        if (owner.isAlliedTo(target) || target.isAlliedTo(owner))
        {
            return false;
        }
        // Go after genuinely hostile mobs (creepers, zombies, enemy players via PvP, etc.)
        // or any entity the caster marked with Swarm Exemption (swarm_exemption rallies
        // every summon against the marked target).
        return target instanceof Enemy
                || target.hasEffect(io.entomology.entomology.registries.EffectRegistry.SWARM_EXEMPTION.get());
    }

    // ---- GeckoLib animation ----
    // The inherited AbstractSpellCastingMob registers Iron's own cast controllers,
    // whose animation names don't exist in our butterfly_princess animation file —
    // so the model would just stand in a bind-pose T-pose. Register one controller
    // that picks idle / walk / cast from our own animations.

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.butterfly_princess.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.butterfly_princess.walk");
    private static final RawAnimation FLYING = RawAnimation.begin().thenLoop("animation.butterfly_princess.flying");
    private static final RawAnimation CAST = RawAnimation.begin().thenPlay("animation.butterfly_princess.attack1");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers)
    {
        controllers.add(new AnimationController<>(this, "butterfly_princess_controller", 5, state ->
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
                state.setAnimation(FLYING);
            }
            return PlayState.CONTINUE;
        }));
    }

    // Note: getSummoner() is inherited from IMagicSummon's default, which
    // resolves the owner through SummonManager#getOwner — exactly the source
    // this entity populates in its (Level, LivingEntity) constructor. The
    // GenericFollowOwnerGoal constructor takes a Supplier<Entity>, so the
    // default Entity-returning signature is what we want here (matching the
    // SummonedBeeEntity pattern). Do not narrow the return type to
    // LivingEntity: that would change `this::getSummoner` into a
    // Supplier<LivingEntity>, which isn't assignable to Supplier<Entity>.

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
        SummonedRestorationHelper.writeRestoreFlag(this, tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        SummonedRestorationHelper.readRestoreFlag(this, tag);
    }

    @Override
    public void tick()
    {
        super.tick();
        SummonedRestorationHelper.handleRestoredDespawn(this);
    }
}
