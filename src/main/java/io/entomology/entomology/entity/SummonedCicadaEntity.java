package io.entomology.entomology.entity;

import io.entomology.entomology.registries.EntityRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericFollowOwnerGoal;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Summoned cicada variant. Hatched from insect eggs laid by Shiraori.
 * Retains the wild cicada's flying AI and area debuff (Slowness + Darkness),
 * but adds owner-driven combat: follows the owner and auto-targets nearby
 * hostiles (Enemy interface) that are not the owner's summons.
 */
public class SummonedCicadaEntity extends CicadaEntity implements IMagicSummon
{
    public SummonedCicadaEntity(EntityType<? extends CicadaEntity> type, Level level)
    {
        super(type, level);
    }

    public SummonedCicadaEntity(Level level, @Nullable LivingEntity owner)
    {
        this(EntityRegistry.SUMMONED_CICADA.get(), level);
        if (owner != null)
        {
            SummonManager.setOwner(this, owner);
            this.entityData.set(OWNER_UUID, java.util.Optional.of(owner.getUUID()));
        }
        this.setHealth(this.getMaxHealth());
    }

    @Override
    protected void registerGoals()
    {
        // Keep the parent's flying wander + look goals
        super.registerGoals();
        // Shoot trail-less sting projectiles at the current target
        this.goalSelector.addGoal(2, new CicadaStingAttackGoal(this));
        // Follow the owner
        this.goalSelector.addGoal(3, new GenericFollowOwnerGoal(this, this::getSummoner, 1.0, 8.0F, 3.0F, true, 16.0F));
        // Auto-attack nearby hostiles (excluding owner and owner's summons)
        this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                target -> !io.entomology.entomology.util.SwarmCreatures.isSameOwnerChain(this, target)
                        && target instanceof Enemy
        ));
    }

    /**
     * Fires a trail-less cicada sting (3-6 damage + Slowness) at the current
     * target every 2 seconds, within 16 blocks and line of sight.
     */
    static class CicadaStingAttackGoal extends net.minecraft.world.entity.ai.goal.Goal
    {
        private static final int ATTACK_INTERVAL = 40; // 2 seconds
        private static final double RANGE_SQR = 16.0D * 16.0D;

        private final SummonedCicadaEntity cicada;
        private int cooldown = 0;

        CicadaStingAttackGoal(SummonedCicadaEntity cicada)
        {
            this.cicada = cicada;
        }

        @Override
        public boolean canUse()
        {
            LivingEntity target = this.cicada.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse()
        {
            return this.canUse();
        }

        @Override
        public void tick()
        {
            LivingEntity target = this.cicada.getTarget();
            if (target == null)
                return;

            if (this.cooldown > 0)
            {
                this.cooldown--;
                return;
            }

            if (this.cicada.distanceToSqr(target) > RANGE_SQR || !this.cicada.hasLineOfSight(target))
            {
                this.cooldown = 10; // re-check soon
                return;
            }

            this.cooldown = ATTACK_INTERVAL;
            CicadaStingProjectile sting = new CicadaStingProjectile(this.cicada.level(), this.cicada);
            sting.setDamage(3.0F + this.cicada.getRandom().nextFloat() * 3.0F);
            sting.moveTo(this.cicada.getX(), this.cicada.getY() + 0.3D, this.cicada.getZ());
            sting.shoot(target.getEyePosition().subtract(sting.position()).normalize());
            this.cicada.level().addFreshEntity(sting);
        }
    }

    // ---- IMagicSummon lifecycle ----

    @Override
    public void onUnSummon()
    {
        if (!this.level().isClientSide)
        {
            for (int i = 0; i < 10; i++)
            {
                this.level().addParticle(ParticleTypes.PORTAL,
                        this.getX() + (this.random.nextDouble() - 0.5) * 0.5,
                        this.getY() + this.random.nextDouble() * 0.5,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 0.5,
                        0, 0, 0);
            }
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
    public boolean shouldBeSaved()
    {
        return false;
    }

    // ---- Cross-world restore handling ----

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
