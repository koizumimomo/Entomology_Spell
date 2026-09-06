package io.entomology.entomology.entity;

import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * A healing spider similar to Goety's Wartling. After attacking, it returns
 * to heal either the nest or the caster, prioritizing the nest when threatened.
 */
public class WartlingEntity extends Spider implements OwnableEntity, GeoEntity
{
    private SpiderNestEntity nest;
    private LivingEntity owner;
    private boolean defendingNest = false;
    private int healingCooldown = 0;
    private static final int HEAL_COOLDOWN_TICKS = 100; // 5 seconds
    private static final float HEAL_AMOUNT = 2.0F;

    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("animation.wartling.idle");
    private static final RawAnimation ANIM_ATTACK = RawAnimation.begin().thenPlay("animation.wartling.attack");
    private static final RawAnimation ANIM_HEAL = RawAnimation.begin().thenLoop("animation.wartling.heal");
    private final AnimationController<WartlingEntity> animController =
            new AnimationController<>(this, "wartling_controller", 0, this::animationPredicate);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public WartlingEntity(EntityType<? extends WartlingEntity> entityType, Level level)
    {
        super(entityType, level);
        this.setPersistenceRequired();
    }

    @Override
    protected void registerGoals()
    {
        // Custom AI goals will be set via setupAI()
    }

    public void setupAI(LivingEntity owner)
    {
        this.owner = owner;
        
        this.goalSelector.getAvailableGoals().clear();
        this.targetSelector.getAvailableGoals().clear();
        
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SpiderAttackGoal(this));
        this.goalSelector.addGoal(3, new WartlingHealGoal(this));
        this.goalSelector.addGoal(4, new FollowNestOrOwnerGoal(this));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, LivingEntity.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new WartlingOwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new WartlingOwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                target -> target != owner && target.isAlive()));
    }

    public void setNest(SpiderNestEntity nest)
    {
        this.nest = nest;
    }

    public void setDefendingNest(boolean defending)
    {
        this.defendingNest = defending;
    }

    @Override
    public void tick()
    {
        super.tick();

        if (this.level().isClientSide)
            return;

        if (owner != null && !owner.isAlive())
        {
            this.discard();
            return;
        }
        
        if (healingCooldown > 0)
            healingCooldown--;

        if (nest != null && nest.getHealth() < nest.getMaxHealth() * 0.5)
        {
            defendingNest = true;
        }
    }

    public void performHealing()
    {
        if (healingCooldown > 0)
            return;

        if (nest != null && (defendingNest || nest.getHealth() < nest.getMaxHealth()))
        {
            float currentHealth = nest.getHealth();
            float maxHealth = nest.getMaxHealth();
            if (currentHealth < maxHealth)
            {
                nest.setHealth(Math.min(maxHealth, currentHealth + HEAL_AMOUNT));
                healingCooldown = HEAL_COOLDOWN_TICKS;
                spawnHealingParticles(nest);
                return;
            }
        }

        if (owner != null && owner.isAlive())
        {
            float currentHealth = owner.getHealth();
            float maxHealth = owner.getMaxHealth();
            if (currentHealth < maxHealth)
            {
                owner.heal(HEAL_AMOUNT);
                healingCooldown = HEAL_COOLDOWN_TICKS;
                spawnHealingParticles(owner);
            }
        }
    }

    private void spawnHealingParticles(Entity target)
    {
        for (int i = 0; i < 5; i++)
        {
            double x = target.getX() + (Math.random() - 0.5) * target.getBbWidth();
            double y = target.getY() + target.getBbHeight() * 0.5;
            double z = target.getZ() + (Math.random() - 0.5) * target.getBbWidth();
            
            this.level().addParticle(net.minecraft.core.particles.ParticleTypes.HEART,
                    x, y, z, 0.0, 0.1, 0.0);
        }
    }

    @Override
    public LivingEntity getOwner()
    {
        return owner;
    }

    @Override
    @Nullable
    public UUID getOwnerUUID()
    {
        return owner != null ? owner.getUUID() : null;
    }

    public boolean isHealing()
    {
        return healingCooldown > 0;
    }

    public SpiderNestEntity getNest()
    {
        return nest;
    }

    public boolean isDefendingNest()
    {
        return defendingNest;
    }

    private static class WartlingHealGoal extends Goal
    {
        private final WartlingEntity wartling;

        public WartlingHealGoal(WartlingEntity wartling)
        {
            this.wartling = wartling;
        }

        @Override
        public boolean canUse()
        {
            if (wartling.healingCooldown > 0)
                return false;
            return needsHealing();
        }

        @Override
        public boolean canContinueToUse()
        {
            return needsHealing();
        }

        @Override
        public void tick()
        {
            wartling.performHealing();
        }

        private boolean needsHealing()
        {
            if (wartling.nest != null && 
                (wartling.defendingNest || wartling.nest.getHealth() < wartling.nest.getMaxHealth()))
            {
                return true;
            }

            if (wartling.owner != null && wartling.owner.isAlive() && 
                wartling.owner.getHealth() < wartling.owner.getMaxHealth())
            {
                return true;
            }

            return false;
        }
    }

    private static class FollowNestOrOwnerGoal extends Goal
    {
        private final WartlingEntity wartling;
        private final double speedModifier = 0.3;

        public FollowNestOrOwnerGoal(WartlingEntity wartling)
        {
            this.wartling = wartling;
        }

        @Override
        public boolean canUse()
        {
            return wartling.getTarget() == null;
        }

        @Override
        public void tick()
        {
            Entity target;
            if (wartling.defendingNest && wartling.nest != null)
            {
                target = wartling.nest;
            }
            else if (wartling.owner != null)
            {
                target = wartling.owner;
            }
            else
            {
                return;
            }

            if (wartling.distanceTo(target) > 3.0)
            {
                wartling.getNavigation().moveTo(target, speedModifier);
            }
        }
    }

    private static class SpiderAttackGoal extends MeleeAttackGoal
    {
        public SpiderAttackGoal(WartlingEntity spider)
        {
            super(spider, 0.5, true);
        }
    }

    /**
     * Attack whatever just hurt the caster (owner). Works for any LivingEntity owner.
     */
    private static class WartlingOwnerHurtByTargetGoal extends TargetGoal
    {
        private final WartlingEntity wartling;
        @Nullable
        private LivingEntity ownerLastHurtBy;
        private int timestamp;

        public WartlingOwnerHurtByTargetGoal(WartlingEntity wartling)
        {
            super(wartling, false);
            this.wartling = wartling;
        }

        @Override
        public boolean canUse()
        {
            LivingEntity owner = wartling.owner;
            if (owner == null) return false;
            this.ownerLastHurtBy = owner.getLastHurtByMob();
            int i = owner.getLastHurtByMobTimestamp();
            return i != this.timestamp
                    && this.ownerLastHurtBy != null
                    && this.ownerLastHurtBy != owner
                    && this.ownerLastHurtBy != wartling
                    && this.canAttack(this.ownerLastHurtBy, TargetingConditions.DEFAULT);
        }

        @Override
        public void start()
        {
            this.mob.setTarget(this.ownerLastHurtBy);
            LivingEntity owner = wartling.owner;
            if (owner != null)
                this.timestamp = owner.getLastHurtByMobTimestamp();
            super.start();
        }
    }

    /**
     * Attack whatever the caster (owner) just attacked.
     */
    private static class WartlingOwnerHurtTargetGoal extends TargetGoal
    {
        private final WartlingEntity wartling;
        @Nullable
        private LivingEntity ownerLastHurt;
        private int timestamp;

        public WartlingOwnerHurtTargetGoal(WartlingEntity wartling)
        {
            super(wartling, false);
            this.wartling = wartling;
        }

        @Override
        public boolean canUse()
        {
            LivingEntity owner = wartling.owner;
            if (owner == null) return false;
            this.ownerLastHurt = owner.getLastHurtMob();
            int i = owner.getLastHurtMobTimestamp();
            return i != this.timestamp
                    && this.ownerLastHurt != null
                    && this.ownerLastHurt != owner
                    && this.ownerLastHurt != wartling
                    && this.canAttack(this.ownerLastHurt, TargetingConditions.DEFAULT);
        }

        @Override
        public void start()
        {
            this.mob.setTarget(this.ownerLastHurt);
            LivingEntity owner = wartling.owner;
            if (owner != null)
                this.timestamp = owner.getLastHurtMobTimestamp();
            super.start();
        }
    }

    private PlayState animationPredicate(AnimationState<WartlingEntity> event)
    {
        AnimationController<WartlingEntity> controller = event.getController();
        if (this.isHealing())
            controller.setAnimation(ANIM_HEAL);
        else if (this.getTarget() != null || this.swinging)
            controller.setAnimation(ANIM_ATTACK);
        else
            controller.setAnimation(ANIM_IDLE);
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar)
    {
        controllerRegistrar.add(this.animController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache()
    {
        return this.cache;
    }
}