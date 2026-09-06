package io.entomology.entomology.entity;

import io.entomology.entomology.registries.EntityRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericCopyOwnerTargetGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericHurtByTargetGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericOwnerHurtByTargetGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericOwnerHurtTargetGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericProtectOwnerTargetGoal;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Summoned spider (its own registered entity type, not EntityType.SPIDER) so
 * the spider nest keeps working in modpacks where natural spawning is disabled
 * or vanilla spiders are removed. The wild hostility goals (daylight-agnostic
 * player hunting, golem hunting, counterattack) are replaced with owner-driven
 * summon goals; the SpiderNestEntity keeps redirecting them at nearby monsters.
 */
public class SummonedSpiderEntity extends Spider implements IMagicSummon
{
    public SummonedSpiderEntity(EntityType<? extends Spider> entityType, Level level)
    {
        super(entityType, level);
        this.setPersistenceRequired();
    }

    public SummonedSpiderEntity(Level level, @Nullable LivingEntity owner)
    {
        this(EntityRegistry.SUMMONED_SPIDER.get(), level);
        if (owner != null)
        {
            SummonManager.setOwner(this, owner);
        }
    }

    @Override
    protected void registerGoals()
    {
        super.registerGoals();
        // Strip the wild spider's hostility (SpiderTargetGoal extends
        // NearestAttackableTargetGoal) and the counterattack; owner-driven
        // target goals take over. Melee attack/leap goals stay untouched.
        this.targetSelector.getAvailableGoals().removeIf(wrappedGoal ->
        {
            Goal goal = wrappedGoal.getGoal();
            return goal instanceof NearestAttackableTargetGoal<?> || goal instanceof HurtByTargetGoal;
        });

        this.targetSelector.addGoal(1, new GenericOwnerHurtByTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(2, new GenericOwnerHurtTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(3, new GenericCopyOwnerTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(4, new GenericHurtByTargetGoal(this, entity -> entity == this.getSummoner()).setAlertOthers());
        this.targetSelector.addGoal(5, new GenericProtectOwnerTargetGoal(this, this::getSummoner));
    }

    // ---- Summon lifecycle ----

    @Override
    public void onUnSummon()
    {
        if (!this.level().isClientSide)
        {
            io.redspace.ironsspellbooks.capabilities.magic.MagicManager.spawnParticles(
                    this.level(), ParticleTypes.SQUID_INK,
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
