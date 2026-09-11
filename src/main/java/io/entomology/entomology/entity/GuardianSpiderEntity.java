package io.entomology.entomology.entity;

import io.entomology.entomology.registries.EntityRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class GuardianSpiderEntity extends PathfinderMob implements software.bernie.geckolib.animatable.GeoEntity
{
    private static final EntityDataAccessor<Boolean> BERSERK = SynchedEntityData.defineId(GuardianSpiderEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<java.util.Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(GuardianSpiderEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private int berserkTimer = 0;
    private static final int BERSERK_DURATION = 600; // 30 seconds

    // Brood-guarding behaviour: while a Shiraori lives within this radius the
    // spider stays near her and protects her; otherwise it roams and hunts.
    private static final double GUARD_SEARCH_RADIUS = 32.0D;
    private static final double GUARD_FOLLOW_DIST_SQR = 256.0D;  // walk back once farther than 16 blocks
    private static final double GUARD_TELEPORT_DIST_SQR = 1024.0D; // teleport back past 32 blocks
    @Nullable
    private ShiraoriEntity guardedShiraori;
    private int shiraoriSearchDelay = 0;

    // Animations: guardian_spider.animation.json has: animation, walk, animation2, animation3, Sit, jump_start, idle, death
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.guardian_spider.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.guardian_spider.walk");
    private static final RawAnimation JUMP = RawAnimation.begin().thenPlay("animation.guardian_spider.jump_start");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.guardian_spider.animation2");
    private static final RawAnimation BERSERK_ANIM = RawAnimation.begin().thenLoop("animation.guardian_spider.animation3");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GuardianSpiderEntity(EntityType<? extends GuardianSpiderEntity> type, Level level)
    {
        super(type, level);
    }

    public GuardianSpiderEntity(Level level, @Nullable LivingEntity owner)
    {
        this(EntityRegistry.GUARDIAN_SPIDER.get(), level);
        if (owner != null)
        {
            SummonManager.setOwner(this, owner);
            this.entityData.set(OWNER_UUID, java.util.Optional.of(owner.getUUID()));
        }
        this.setHealth(this.getMaxHealth());
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        this.entityData.define(BERSERK, false);
        this.entityData.define(OWNER_UUID, java.util.Optional.empty());
    }

    @Override
    protected void registerGoals()
    {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.8F));
        // Local wandering between fights: when guarding, this keeps it pacing
        // around Shiraori; when she is gone, it roams looking for hostiles.
        this.goalSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.RandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 16, true, false, this::isValidTarget));
    }

    private boolean isValidTarget(@Nullable LivingEntity target)
    {
        if (target == null || !target.isAlive() || target == this)
            return false;
        if (target instanceof Player p && (p.isCreative() || p.isSpectator()))
            return false;
        if (isAlly(target))
            return false;
        if (target instanceof Enemy)
            return true;
        return target.hasEffect(io.entomology.entomology.registries.EffectRegistry.SWARM_EXEMPTION.get());
    }

    /**
     * UUID-based ally check (entity identity comparisons are unreliable after
     * reloads/dimension changes): the spider's brood mother Shiraori, the
     * summoning player, other guardian spiders, Shiraori's nests brood, and any
     * other summon sharing one of those owners are all friendly.
     */
    public boolean isAlly(LivingEntity target)
    {
        if (io.entomology.entomology.util.SwarmCreatures.isShiraoriAttendant(target))
            return true;
        LivingEntity broodMother = getOwnerEntity();
        Entity casterEntity = SummonManager.getOwner(this);
        LivingEntity caster = casterEntity instanceof LivingEntity le ? le : null;
        if (broodMother != null && target.getUUID().equals(broodMother.getUUID()))
            return true;
        if (caster != null && target.getUUID().equals(caster.getUUID()))
            return true;
        if (target instanceof ShiraoriEntity || target instanceof GuardianSpiderEntity)
            return true;
        Entity targetOwnerEntity = SummonManager.getOwner(target);
        LivingEntity targetOwner = targetOwnerEntity instanceof LivingEntity le2 ? le2 : null;
        if (targetOwner != null)
        {
            if (caster != null && targetOwner.getUUID().equals(caster.getUUID()))
                return true;
            if (broodMother != null && targetOwner.getUUID().equals(broodMother.getUUID()))
                return true;
        }
        return this.isAlliedTo(target);
    }

    // Hard gate: no target-acquisition path (retaliation, brood defence sync,
    // hurt-by-target alerts) may ever lock onto an ally.
    @Override
    public void setTarget(@Nullable LivingEntity target)
    {
        if (target != null && isAlly(target))
            return;
        super.setTarget(target);
    }

    /**
     * Returns the Shiraori this spider guards while she is alive nearby.
     * Prefer its own brood mother (set in the constructor), otherwise the
     * nearest Shiraori. Cached for one second between area scans.
     */
    @Nullable
    private ShiraoriEntity getGuardedShiraori()
    {
        if (this.guardedShiraori != null
                && (!this.guardedShiraori.isAlive() || this.guardedShiraori.level() != this.level()
                        || this.distanceToSqr(this.guardedShiraori) > GUARD_SEARCH_RADIUS * GUARD_SEARCH_RADIUS * 4.0D))
        {
            this.guardedShiraori = null;
        }
        if (this.guardedShiraori == null)
        {
            if (--this.shiraoriSearchDelay > 0)
                return null;
            this.shiraoriSearchDelay = 20;
            LivingEntity mother = getOwnerEntity();
            ShiraoriEntity nearest = null;
            double nearestDist = Double.MAX_VALUE;
            AABB area = this.getBoundingBox().inflate(GUARD_SEARCH_RADIUS);
            for (ShiraoriEntity shiraori : this.level().getEntitiesOfClass(ShiraoriEntity.class, area, e -> e.isAlive()))
            {
                if (shiraori == mother)
                {
                    nearest = shiraori;
                    break;
                }
                double dist = shiraori.distanceToSqr(this);
                if (dist < nearestDist)
                {
                    nearest = shiraori;
                    nearestDist = dist;
                }
            }
            this.guardedShiraori = nearest;
        }
        return this.guardedShiraori;
    }

    @Nullable
    public UUID getOwnerUUID()
    {
        return ((java.util.Optional<UUID>) this.entityData.get(OWNER_UUID)).orElse(null);
    }

    @Nullable
    public LivingEntity getOwnerEntity()
    {
        UUID id = getOwnerUUID();
        if (id == null || !(this.level() instanceof ServerLevel sl))
            return null;
        Entity e = sl.getEntity(id);
        return e instanceof LivingEntity le ? le : null;
    }

    public boolean isBerserk()
    {
        return this.entityData.get(BERSERK);
    }

    public void triggerBerserk()
    {
        this.berserkTimer = BERSERK_DURATION;
        this.entityData.set(BERSERK, true);
        if (!this.level().isClientSide)
        {
            // Particles + sound for berserk activation
            this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 1.0F, 0.8F);
        }
    }

    @Override
    public void tick()
    {
        super.tick();
        if (!this.level().isClientSide)
        {
            if (berserkTimer > 0)
            {
                berserkTimer--;
                if (berserkTimer <= 0)
                {
                    this.entityData.set(BERSERK, false);
                }
            }
            else if (this.getTarget() == null)
            {
                // Shiraori alive nearby: protect her — copy her current target
                // and stay close, wandering locally. Once she despawns, fall
                // back to following the summoning player; with neither, the
                // target goals + RandomStrollGoal let it roam and hunt alone.
                LivingEntity anchor = null;
                ShiraoriEntity shiraori = getGuardedShiraori();
                if (shiraori != null)
                {
                    anchor = shiraori;
                    LivingEntity herTarget = shiraori.getTarget();
                    if (herTarget != null && herTarget.isAlive() && isValidTarget(herTarget))
                    {
                        this.setTarget(herTarget);
                    }
                }
                else
                {
                    // Brood mother gone: trail the summoning player like the
                    // other summons instead of idling where she despawned.
                    if (SummonManager.getOwner(this) instanceof LivingEntity caster)
                        anchor = caster;
                }
                if (anchor != null)
                {
                    double dist = this.distanceToSqr(anchor);
                    if (dist > GUARD_TELEPORT_DIST_SQR)
                    {
                        double dx = (this.getRandom().nextDouble() - 0.5D) * 2.0D;
                        double dz = (this.getRandom().nextDouble() - 0.5D) * 2.0D;
                        this.moveTo(anchor.getX() + dx, anchor.getY(), anchor.getZ() + dz,
                                this.getYRot(), this.getXRot());
                    }
                    else if (dist > GUARD_FOLLOW_DIST_SQR)
                    {
                        this.getNavigation().moveTo(anchor, 1.2D);
                    }
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target)
    {
        boolean result = super.doHurtTarget(target);
        if (result && isBerserk() && !this.level().isClientSide)
        {
            // Area knockback during berserk
            AABB aabb = target.getBoundingBox().inflate(3.0D);
            List<LivingEntity> nearby = this.level().getEntitiesOfClass(LivingEntity.class, aabb,
                    e -> e != this && e != this.getOwnerEntity() && e.isAlive() && !e.isSpectator()
                            && SummonManager.getOwner(e) == null);
            for (LivingEntity le : nearby)
            {
                le.knockback(0.5D, this.getX() - le.getX(), this.getZ() - le.getZ());
            }
            // Web entangle particle effect (no actual web)
            this.level().addParticle(ParticleTypes.CLOUD, target.getX(), target.getY() + 1, target.getZ(), 0, 0.1, 0);
        }
        return result;
    }

    // GeckoLib animation
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers)
    {
        controllers.add(new AnimationController<>(this, "guardian_spider_controller", 5, this::animationPredicate));
    }

    private PlayState animationPredicate(AnimationState<GuardianSpiderEntity> state)
    {
        if (this.isBerserk())
        {
            state.setAnimation(BERSERK_ANIM);
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
        tag.putBoolean("Berserk", isBerserk());
        tag.putInt("BerserkTimer", berserkTimer);
        UUID ownerUUID = getOwnerUUID();
        if (ownerUUID != null)
            tag.putUUID("OwnerUUID", ownerUUID);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        this.berserkTimer = tag.getInt("BerserkTimer");
        this.entityData.set(BERSERK, tag.getBoolean("Berserk"));
        if (tag.hasUUID("OwnerUUID"))
            this.entityData.set(OWNER_UUID, java.util.Optional.of(tag.getUUID("OwnerUUID")));
    }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }
}
