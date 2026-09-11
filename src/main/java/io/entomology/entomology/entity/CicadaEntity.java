package io.entomology.entomology.entity;

import io.entomology.entomology.registries.EntityRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class CicadaEntity extends PathfinderMob implements software.bernie.geckolib.animatable.GeoEntity
{
    private static final EntityDataAccessor<Boolean> IS_ALT_COLOR =
            SynchedEntityData.defineId(CicadaEntity.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<java.util.Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(CicadaEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.cicada.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.cicada.walk");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int debuffTimer = 0;

    public CicadaEntity(EntityType<? extends CicadaEntity> type, Level level)
    {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        // Roll the alternate color variant once at creation. This is done in the
        // constructor (rather than in aiStep()) because tickCount resets to 0 every
        // time the entity is reloaded from NBT: checking tickCount == 1 there would
        // re-roll the color of every non-alt cicada on each world load. The saved
        // value restored by readAdditionalSaveData always wins on reload.
        if (!level.isClientSide && this.getRandom().nextFloat() < 0.3F)
        {
            this.entityData.set(IS_ALT_COLOR, true);
        }
    }

    public CicadaEntity(Level level, @Nullable LivingEntity owner)
    {
        this(EntityRegistry.CICADA.get(), level);
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
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.FLYING_SPEED, 0.6D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D);
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        this.entityData.define(IS_ALT_COLOR, false);
        this.entityData.define(OWNER_UUID, java.util.Optional.empty());
    }

    @Override
    protected PathNavigation createNavigation(Level level)
    {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void registerGoals()
    {
        this.goalSelector.addGoal(0, new CicadaWanderGoal(this));
        this.goalSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));
    }

    @Nullable
    public UUID getOwnerUUID()
    {
        return ((java.util.Optional<UUID>) this.entityData.get(OWNER_UUID)).orElse(null);
    }

    public boolean isAltColor()
    {
        return this.entityData.get(IS_ALT_COLOR);
    }

    @Override
    public void tick()
    {
        super.tick();
        if (!this.level().isClientSide)
        {
            // Area debuff every 20 ticks (1 second)
            if (++this.debuffTimer >= 20)
            {
                this.debuffTimer = 0;
                applyAreaDebuff();
            }
        }
    }

    private void applyAreaDebuff()
    {
        if (!(this.level() instanceof ServerLevel serverLevel))
            return;

        AABB aabb = this.getBoundingBox().inflate(6.0D);
        List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(LivingEntity.class, aabb,
                e -> e != this && e.isAlive() && !e.isSpectator());

        UUID ownerUUID = getOwnerUUID();
        for (LivingEntity target : nearby)
        {
            // Skip owner
            if (ownerUUID != null && target.getUUID().equals(ownerUUID))
                continue;

            // Skip same-owner summons
            if (ownerUUID != null)
            {
                LivingEntity owner = (LivingEntity) SummonManager.getOwner(this);
                if (owner != null && SummonManager.getOwner(target) == owner)
                    continue;
            }

            // Skip entities with Insect Kinship (insect affinity)
            if (target.hasEffect(io.entomology.entomology.registries.EffectRegistry.INSECT_KINSHIP.get()))
                continue;

            // Apply Slowness III and Darkness for 3 seconds (60 ticks)
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, true, true));
            // Darkness requires 1.19.4+ (1.20.1 has it)
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, true, true));
        }

        // Sound wave particle effect
        serverLevel.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 0.5, this.getZ(), 0, 0, 0, 0, 0);
    }

    // GeckoLib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers)
    {
        controllers.add(new AnimationController<>(this, "cicada_controller", 0, this::animationPredicate));
    }

    private PlayState animationPredicate(AnimationState<CicadaEntity> state)
    {
        if (state.isMoving())
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
        tag.putBoolean("AltColor", isAltColor());
        UUID ownerUUID = getOwnerUUID();
        if (ownerUUID != null)
            tag.putUUID("OwnerUUID", ownerUUID);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        this.entityData.set(IS_ALT_COLOR, tag.getBoolean("AltColor"));
        if (tag.hasUUID("OwnerUUID"))
            this.entityData.set(OWNER_UUID, java.util.Optional.of(tag.getUUID("OwnerUUID")));
    }

    protected void checkFallDamage(double y, boolean onGround, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {}

    @Override
    public boolean removeWhenFarAway(double distance)
    {
        return getOwnerUUID() == null;
    }

    // Flying wander goal (copy of ButterflyWanderGoal)
    static class CicadaWanderGoal extends Goal
    {
        private final CicadaEntity cicada;
        private Vec3 wanderTarget;
        private int cooldown = 0;

        CicadaWanderGoal(CicadaEntity cicada)
        {
            this.cicada = cicada;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse()
        {
            return true;
        }

        @Override
        public void tick()
        {
            if (cooldown > 0)
            {
                cooldown--;
                if (wanderTarget != null)
                {
                    Vec3 desired = wanderTarget.subtract(cicada.position());
                    double dist = desired.length();
                    if (dist > 0.5D)
                        cicada.setDeltaMovement(desired.normalize().scale(0.1D));
                }
                return;
            }

            if (wanderTarget == null || cicada.distanceToSqr(wanderTarget) < 4.0D)
            {
                double angle = cicada.getRandom().nextDouble() * Math.PI * 2;
                double dist = 5.0D + cicada.getRandom().nextDouble() * 10.0D;
                wanderTarget = cicada.position().add(
                        Math.cos(angle) * dist,
                        (cicada.getRandom().nextDouble() - 0.5) * 4.0D,
                        Math.sin(angle) * dist);
                cooldown = 20 + cicada.getRandom().nextInt(40);
            }

            Vec3 desired = wanderTarget.subtract(cicada.position());
            double dist = desired.length();
            if (dist > 0.01D)
                cicada.setDeltaMovement(desired.normalize().scale(0.12D));
        }
    }
}
