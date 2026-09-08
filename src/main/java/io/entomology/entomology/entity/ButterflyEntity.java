package io.entomology.entomology.entity;

import io.entomology.entomology.registries.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nonnull;
import java.util.EnumSet;

/**
 * Butterfly entity. Uses the group model (15 butterflies in one entity) for
 * the Summon Butterfly spell. Also doubles as the passive wild mob that
 * spawns near flowers. When a target is set (via spell), the butterfly flies
 * to the target's head and circles; otherwise it wanders peacefully.
 */
public class ButterflyEntity extends Mob implements GeoEntity
{
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(ButterflyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_WILD = SynchedEntityData.defineId(ButterflyEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.butterfly.idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int lifetimeTicks = -1; // -1 = infinite (wild), >0 = countdown (spell-summoned)

    public ButterflyEntity(EntityType<? extends ButterflyEntity> type, Level level)
    {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    public ButterflyEntity(Level level, @Nullable LivingEntity caster, boolean isWild)
    {
        this(EntityRegistry.BUTTERFLY.get(), level);
        this.entityData.set(IS_WILD, isWild);
        if (caster != null)
        {
            this.moveTo(caster.getX(), caster.getY() + 2.0D, caster.getZ(), this.getYRot(), 0.0F);
        }
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.FLYING_SPEED, 0.6D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D);
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        this.entityData.define(TARGET_ID, -1);
        // Wild by default: butterflies spawned naturally (biome modifier / spawn
        // egg) are wild and respond to royal-jelly / insect-crystal interactions.
        // The Summon Butterfly spell explicitly sets this to false.
        this.entityData.define(IS_WILD, true);
    }

    @Override
    protected PathNavigation createNavigation(Level level)
    {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    protected void registerGoals()
    {
        this.goalSelector.addGoal(0, new FlyToTargetHeadGoal(this));
        this.goalSelector.addGoal(1, new ButterflyWanderGoal(this));
    }

    /**
     * Sets the entity this butterfly should swarm around (the spell target).
     */
    public void setTargetEntity(@Nullable Entity target)
    {
        this.entityData.set(TARGET_ID, target == null ? -1 : target.getId());
    }

    @Nullable
    public Entity getTargetEntity()
    {
        int id = this.entityData.get(TARGET_ID);
        if (id == -1)
            return null;
        return this.level().getEntity(id);
    }

    public boolean isWild()
    {
        return this.entityData.get(IS_WILD);
    }

    public void setLifetime(int ticks)
    {
        this.lifetimeTicks = ticks;
    }

    @Override
    public void tick()
    {
        super.tick();
        if (!this.level().isClientSide && lifetimeTicks > 0)
        {
            lifetimeTicks--;
            if (lifetimeTicks <= 0)
            {
                this.discard();
            }
        }
    }

    @Override
    public boolean isPersistenceRequired()
    {
        return !isWild();
    }

    @Override
    public boolean removeWhenFarAway(double distance)
    {
        return isWild();
    }

    @Override
    public boolean shouldBeSaved()
    {
        return isWild();
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos)
    {
        // Butterflies fly — no fall damage
    }

    // ---- GeckoLib ----

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers)
    {
        controllers.add(new AnimationController<>(this, "butterfly_controller", 0, this::animationPredicate));
    }

    private PlayState animationPredicate(AnimationState<ButterflyEntity> state)
    {
        state.setAnimation(IDLE);
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache()
    {
        return this.cache;
    }

    // ---- NBT ----

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("IsWild", isWild());
        tag.putInt("LifetimeTicks", lifetimeTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        this.entityData.set(IS_WILD, tag.getBoolean("IsWild"));
        this.lifetimeTicks = tag.getInt("LifetimeTicks");
    }

    // ---- AI Goals ----

    /** Flies toward the target entity's head and circles around it. */
    static class FlyToTargetHeadGoal extends Goal
    {
        private final ButterflyEntity butterfly;
        private double orbitAngle = 0;

        FlyToTargetHeadGoal(ButterflyEntity butterfly)
        {
            this.butterfly = butterfly;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse()
        {
            return butterfly.getTargetEntity() != null && butterfly.getTargetEntity().isAlive();
        }

        @Override
        public void tick()
        {
            Entity target = butterfly.getTargetEntity();
            if (target == null)
                return;

            // Circle around the target's head — direct velocity, no pathfinding.
            orbitAngle += 0.15D;
            double radius = 1.5D;
            double targetX = target.getX() + Math.cos(orbitAngle) * radius;
            double targetY = target.getY() + target.getEyeHeight() + 0.5D;
            double targetZ = target.getZ() + Math.sin(orbitAngle) * radius;

            Vec3 desired = new Vec3(
                    targetX - butterfly.getX(),
                    targetY - butterfly.getY(),
                    targetZ - butterfly.getZ());
            double dist = desired.length();
            if (dist > 0.01D)
            {
                double speed = Math.min(dist * 0.15D, 0.4D);
                butterfly.setDeltaMovement(desired.normalize().scale(speed));
            }
        }
    }

    /** Gentle wandering for wild butterflies (and summoned ones with no target). */
    static class ButterflyWanderGoal extends Goal
    {
        private final ButterflyEntity butterfly;
        private Vec3 wanderTarget;
        private int cooldown = 0;

        ButterflyWanderGoal(ButterflyEntity butterfly)
        {
            this.butterfly = butterfly;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse()
        {
            return butterfly.getTargetEntity() == null;
        }

        @Override
        public void tick()
        {
            if (cooldown > 0)
            {
                cooldown--;
                // Keep drifting gently toward the last target so movement is smooth
                if (wanderTarget != null)
                {
                    Vec3 desired = wanderTarget.subtract(butterfly.position());
                    double dist = desired.length();
                    if (dist > 0.5D)
                    {
                        butterfly.setDeltaMovement(desired.normalize().scale(0.1D));
                    }
                }
                return;
            }

            if (wanderTarget == null || butterfly.distanceToSqr(wanderTarget) < 4.0D)
            {
                double angle = butterfly.getRandom().nextDouble() * Math.PI * 2;
                double dist = 5.0D + butterfly.getRandom().nextDouble() * 10.0D;
                wanderTarget = butterfly.position().add(
                        Math.cos(angle) * dist,
                        (butterfly.getRandom().nextDouble() - 0.5) * 4.0D,
                        Math.sin(angle) * dist);
                cooldown = 20 + butterfly.getRandom().nextInt(40);
            }

            // Direct velocity — skip the A* pathfinder entirely for flying mobs.
            Vec3 desired = wanderTarget.subtract(butterfly.position());
            double dist = desired.length();
            if (dist > 0.01D)
            {
                butterfly.setDeltaMovement(desired.normalize().scale(0.12D));
            }
        }
    }
}
