package io.entomology.entomology.entity;

import io.entomology.entomology.entity.ai.SummonedBeeDespawnGoal;
import io.entomology.entomology.entity.ai.SummonedBeeOwnerHurtByTargetGoal;
import io.entomology.entomology.entity.ai.SummonedBeeOwnerHurtTargetGoal;
import io.entomology.entomology.registries.EntityRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericFollowOwnerGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericHurtByTargetGoal;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Summoned bee (its own registered entity type, not the vanilla bee). Modpacks
 * that disable natural mob spawning or remove EntityType.BEE would otherwise
 * break the swarm spell. Vanilla bee body/attack AI (BeeAttackGoal, which
 * requires the persistent anger flag) is kept; only the wild hostility goals in
 * the target selector are replaced with owner-driven summon goals.
 */
public class SummonedBeeEntity extends Bee implements IMagicSummon
{
    public SummonedBeeEntity(EntityType<? extends Bee> entityType, Level level)
    {
        super(entityType, level);
        this.setPersistenceRequired();
    }

    public SummonedBeeEntity(Level level, @Nullable LivingEntity owner)
    {
        this(EntityRegistry.SUMMONED_BEE.get(), level);
        if (owner != null)
        {
            SummonManager.setOwner(this, owner);
        }
    }

    @Override
    protected void registerGoals()
    {
        super.registerGoals();
        // Strip the wild bee's counterattack (BeeHurtByOtherGoal) and the
        // anger-driven player targeting (BeeBecomeAngryTargetGoal). The melee
        // BeeAttackGoal stays: it only swings while the anger flag is set,
        // which our owner-driven target goals maintain via setRemainingPersistentAngerTime.
        this.targetSelector.getAvailableGoals().removeIf(wrappedGoal ->
        {
            Goal goal = wrappedGoal.getGoal();
            return goal instanceof HurtByTargetGoal || goal instanceof NearestAttackableTargetGoal<?>;
        });

        this.targetSelector.addGoal(1, new SummonedBeeOwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new SummonedBeeOwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new GenericHurtByTargetGoal(this, entity -> io.entomology.entomology.util.SwarmCreatures.isSameOwnerChain(this, entity)).setAlertOthers());
        // Swarm members (Summon Bee Swarm) actively hunt nearby hostile mobs.
        // Alarm bees / requiem bees override isSwarmMember() to false, so they
        // keep their owner-reactive behaviour and do not wander off to attack.
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                target -> !io.entomology.entomology.util.SwarmCreatures.isSameOwnerChain(this, target)
                        && (target instanceof Enemy
                        || target.hasEffect(io.entomology.entomology.registries.EffectRegistry.SWARM_EXEMPTION.get())))
        {
            @Override
            public boolean canUse()
            {
                return SummonedBeeEntity.this.isSwarmMember() && super.canUse();
            }
        });

        this.goalSelector.addGoal(4, new GenericFollowOwnerGoal(this, this::getSummoner, 1.0, 9.0F, 4.0F, true, 20.0F));
        this.goalSelector.addGoal(5, new SummonedBeeDespawnGoal(this));
    }

    /**
     * Whether this bee counts toward the Summon Bee Swarm's member count.
     * Specialized bees (alarm posts, requiem kamikaze bees) override this.
     */
    public boolean isSwarmMember()
    {
        return true;
    }

    // ---- Summon lifecycle ----

    @Override
    public void onUnSummon()
    {
        if (!this.level().isClientSide)
        {
            io.redspace.ironsspellbooks.capabilities.magic.MagicManager.spawnParticles(
                    this.level(), ParticleTypes.FALLING_HONEY,
                    this.getX(), this.getY() + 0.5, this.getZ(),
                    20, 0.3, 0.4, 0.3, 0.05, false);
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
