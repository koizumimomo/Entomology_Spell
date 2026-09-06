package io.entomology.entomology.entity;

import io.entomology.entomology.registries.EntityRegistry;
import io.entomology.entomology.registries.SoundRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.spells.root.PreventDismount;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Collections;
import java.util.UUID;

/**
 * Swarm school version of iron's Root entity: web strands burst out of the
 * ground and entangle the target by making it ride this entity.
 * Unlike Root, it can entangle any creature (no boss immunity) and it has
 * health, so it can be killed to free the target early.
 */
public class WebRootEntity extends LivingEntity implements GeoEntity, PreventDismount
{
    @Nullable
    private LivingEntity owner;
    @Nullable
    private UUID ownerUUID;
    private int duration;
    private boolean playSound = true;
    private LivingEntity target;
    private boolean played = false;
    private final RawAnimation ANIMATION = RawAnimation.begin().thenPlay("emerge");
    private final AnimationController controller = new AnimationController(this, "web_root_controller", 0, this::animationPredicate);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public float getScale()
    {
        return this.target == null ? 1.0f : this.target.getScale();
    }

    public WebRootEntity(EntityType<? extends WebRootEntity> pEntityType, Level pLevel)
    {
        super(pEntityType, pLevel);
    }

    public WebRootEntity(Level level, LivingEntity owner)
    {
        this(EntityRegistry.WEB_ROOT.get(), level);
        this.setOwner(owner);
    }

    public LivingEntity getTarget()
    {
        return this.target;
    }

    public void setTarget(LivingEntity target)
    {
        this.target = target;
    }

    public void setOwner(@Nullable LivingEntity pOwner)
    {
        this.owner = pOwner;
        this.ownerUUID = pOwner == null ? null : pOwner.getUUID();
    }

    @Nullable
    public LivingEntity getOwner()
    {
        if (this.owner == null && this.ownerUUID != null && this.level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(this.ownerUUID) instanceof LivingEntity living)
        {
            this.owner = living;
        }
        return this.owner;
    }

    public void setDuration(int duration)
    {
        this.duration = duration;
    }

    public void removeRoot()
    {
        if (this.level().isClientSide)
        {
            for (int i = 0; i < 5; ++i)
            {
                this.level().addParticle(ParticleHelper.ROOT_FOG, this.getX() + Utils.getRandomScaled(0.1f), this.getY() + Utils.getRandomScaled(0.1f), this.getZ() + Utils.getRandomScaled(0.1f),
                        Utils.getRandomScaled(2.0), -this.random.nextFloat() * 0.5f, Utils.getRandomScaled(2.0));
            }
        }
        if (this.getFirstPassenger() instanceof Mob mob && mob.getTarget() == this)
        {
            mob.setTarget(null);
        }
        this.ejectPassengers();
        this.discard();
    }

    @Override
    public void tick()
    {
        super.tick();
        if (this.playSound)
        {
            this.refreshDimensions();
            this.playSound(SoundRegistry.INSECT_CAST.get(), 2.0f, 1.0f);
            this.playSound = false;
        }
        if (!this.level().isClientSide)
        {
            if (this.tickCount % 20 == 0 && this.random.nextFloat() < 0.5f && this.getFirstPassenger() instanceof Mob mob)
            {
                mob.setTarget(this);
            }
            if (this.tickCount > this.duration || this.target != null && this.target.isDeadOrDying() || !this.isVehicle())
            {
                this.removeRoot();
            }
        }
        else if (this.tickCount < 20)
        {
            this.clientDiggingParticles(this);
        }
    }

    protected void clientDiggingParticles(LivingEntity livingEntity)
    {
        for (int i = 0; i < 15; ++i)
        {
            double d0 = livingEntity.getX() + (livingEntity.getRandom().nextDouble() - 0.5);
            double d1 = livingEntity.getY();
            double d2 = livingEntity.getZ() + (livingEntity.getRandom().nextDouble() - 0.5);
            livingEntity.level().addParticle((ParticleOptions) ParticleHelper.ROOT_FOG, d0, d1, d2, 0.0, 0.0, 0.0);
        }
    }

    @Override
    public void die(@NotNull DamageSource damageSource)
    {
        this.removeRoot();
    }

    @Override
    public boolean hurt(DamageSource source, float amount)
    {
        return false; // invulnerable: only the duration ends the entanglement
    }

    @Override
    public boolean isAttackable()
    {
        return false; // mobs will not pick the web as a target (creepers stay calm)
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound)
    {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("Age", this.tickCount);
        if (this.ownerUUID != null)
        {
            pCompound.putUUID("Owner", this.ownerUUID);
        }
        pCompound.putInt("Duration", this.duration);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound)
    {
        super.readAdditionalSaveData(pCompound);
        this.tickCount = pCompound.getInt("Age");
        if (pCompound.hasUUID("Owner"))
        {
            this.ownerUUID = pCompound.getUUID("Owner");
        }
        this.duration = pCompound.getInt("Duration");
    }

    @Override
    public boolean hasIndirectPassenger(Entity pEntity)
    {
        return true;
    }

    @Override
    public HumanoidArm getMainArm()
    {
        return HumanoidArm.RIGHT;
    }

    @Override
    public boolean isPushable()
    {
        return false;
    }

    @Override
    public boolean isPickable()
    {
        return false;
    }

    @Override
    public boolean showVehicleHealth()
    {
        return false;
    }

    @Override
    public void knockback(double pStrength, double pX, double pZ)
    {
    }

    @Override
    public void positionRider(Entity passenger, Entity.MoveFunction pFunction)
    {
        double dx = this.getX() - passenger.getX();
        double dy = this.getY() - passenger.getY();
        double dz = this.getZ() - passenger.getZ();
        if (dx * dx + dy * dy + dz * dz > 25)
        {
            this.removeRoot();
        }
        else
        {
            passenger.setPos(this.getX(), this.getY(), this.getZ());
        }
    }

    @Override
    protected boolean isImmobile()
    {
        return true;
    }

    @Override
    public boolean isAffectedByPotions()
    {
        return false;
    }

    @Override
    public boolean isPushedByFluid(FluidType type)
    {
        return false;
    }

    @Override
    public boolean canCollideWith(@NotNull Entity pEntity)
    {
        return false;
    }

    @Override
    public boolean canBeCollidedWith()
    {
        return false;
    }

    @Override
    protected void doPush(@NotNull Entity pEntity)
    {
    }

    @Override
    public void push(@NotNull Entity pEntity)
    {
    }

    @Override
    protected void pushEntities()
    {
    }

    @Override
    public boolean dismountsUnderwater()
    {
        return false;
    }

    @Override
    public boolean shouldRiderSit()
    {
        return false;
    }

    @Override
    public double getPassengersRidingOffset()
    {
        return 0.0;
    }

    @Override
    public boolean shouldRiderFaceForward(@NotNull Player player)
    {
        return false;
    }

    @Override
    public EntityDimensions getDimensions(Pose pPose)
    {
        Entity rooted = this.getFirstPassenger();
        if (rooted != null)
        {
            return EntityDimensions.fixed(rooted.getBbWidth() * 1.25f, 0.35f);
        }
        return super.getDimensions(pPose);
    }

    @Override
    public boolean canRiderInteract()
    {
        return true;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource damageSource)
    {
        return SoundEvents.AZALEA_LEAVES_PLACE;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound()
    {
        return SoundRegistry.INSECT_CAST.get();
    }

    @Override
    public Iterable<ItemStack> getArmorSlots()
    {
        return Collections.singleton(ItemStack.EMPTY);
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot pSlot)
    {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot pSlot, ItemStack pStack)
    {
    }

    private PlayState animationPredicate(AnimationState<WebRootEntity> event)
    {
        AnimationController<WebRootEntity> controller = event.getController();
        if (!this.played && controller.getAnimationState() == AnimationController.State.STOPPED)
        {
            controller.forceAnimationReset();
            controller.setAnimation(this.ANIMATION);
            this.played = true;
        }
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar)
    {
        controllerRegistrar.add(this.controller);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache()
    {
        return this.cache;
    }
}
