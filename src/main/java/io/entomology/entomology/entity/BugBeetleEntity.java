package io.entomology.entomology.entity;

import io.entomology.entomology.registries.EntityRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.UUID;

// Extends PathfinderMob (not Mob) because MeleeAttackGoal / RandomStrollGoal
// require a PathfinderMob in 1.20.1. PathfinderMob extends Mob, so all Mob APIs
// (createMobAttributes, synched data, save/load) remain available.
public class BugBeetleEntity extends PathfinderMob implements software.bernie.geckolib.animatable.GeoEntity
{
    protected static final EntityDataAccessor<java.util.Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(BugBeetleEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.bug_beetle.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.bug_beetle.walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.bug_beetle.attack");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlay("animation.bug_beetle.death");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int attackAnimTimer = 0;

    public BugBeetleEntity(EntityType<? extends BugBeetleEntity> type, Level level)
    {
        super(type, level);
    }

    public BugBeetleEntity(Level level, @Nullable LivingEntity owner)
    {
        this(EntityRegistry.BUG_BEETLE.get(), level);
        if (owner != null)
        {
            SummonManager.setOwner(this, owner);
            this.entityData.set(OWNER_UUID, java.util.Optional.of(owner.getUUID()));
        }
        this.setHealth(this.getMaxHealth());
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        this.entityData.define(OWNER_UUID, java.util.Optional.empty());
    }

    @Override
    protected void registerGoals()
    {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Wild beetles attack players without swarm_exemption, excluding creative mode
        // Summoned beetles attack enemies but not same-owner entities
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, this::isValidTarget));
    }

    private boolean isValidTarget(@Nullable LivingEntity target)
    {
        if (target == null || !target.isAlive() || target == this)
            return false;

        // Never target the owner
        UUID ownerUUID = getOwnerUUID();
        if (ownerUUID != null && target.getUUID().equals(ownerUUID))
            return false;

        // Don't target other summons with the same owner
        if (ownerUUID != null)
        {
            LivingEntity owner = (LivingEntity) SummonManager.getOwner(this);
            if (owner != null && SummonManager.getOwner(target) == owner)
                return false;
        }

        // Skip players with Insect Kinship (insect affinity) effect
        if (target.hasEffect(io.entomology.entomology.registries.EffectRegistry.INSECT_KINSHIP.get()))
            return false;

        // Wild beetles only attack players (not creative) without swarm_exemption,
        // or any Enemy mob. Summoned beetles attack any Enemy.
        if (target instanceof Enemy)
            return true;

        if (target instanceof Player player)
        {
            if (player.isCreative())
                return false;
            // Wild beetles attack players unless they have swarm_exemption mark
            // (swarm_exemption means "marked for insect attack" so they SHOULD be attacked)
            return true;
        }

        return target.hasEffect(io.entomology.entomology.registries.EffectRegistry.SWARM_EXEMPTION.get());
    }

    @Nullable
    public UUID getOwnerUUID()
    {
        return ((java.util.Optional<UUID>) this.entityData.get(OWNER_UUID)).orElse(null);
    }

    public boolean isSummoned()
    {
        return getOwnerUUID() != null;
    }

    @Override
    public boolean doHurtTarget(Entity target)
    {
        boolean result = super.doHurtTarget(target);
        if (result)
        {
            this.attackAnimTimer = 10; // 0.5s attack animation
        }
        return result;
    }

    @Override
    public void tick()
    {
        super.tick();
        if (this.attackAnimTimer > 0)
            this.attackAnimTimer--;
    }

    // GeckoLib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers)
    {
        controllers.add(new AnimationController<>(this, "bug_beetle_controller", 5, this::animationPredicate));
    }

    private PlayState animationPredicate(AnimationState<BugBeetleEntity> state)
    {
        if (this.attackAnimTimer > 0)
        {
            state.setAnimation(ATTACK);
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
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        UUID ownerUUID = getOwnerUUID();
        if (ownerUUID != null)
            tag.putUUID("OwnerUUID", ownerUUID);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OwnerUUID"))
            this.entityData.set(OWNER_UUID, java.util.Optional.of(tag.getUUID("OwnerUUID")));
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, BlockState block) {}

    @Override
    public boolean removeWhenFarAway(double distance)
    {
        return !isSummoned();
    }
}
