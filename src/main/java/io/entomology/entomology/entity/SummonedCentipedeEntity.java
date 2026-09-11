package io.entomology.entomology.entity;

import com.github.alexthe666.alexsmobs.entity.EntityCentipedeHead;
import io.entomology.entomology.registries.EntityRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericFollowOwnerGoal;
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
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Summoned cave centipede (Alex's Mobs optional integration). Hatched from
 * insect eggs laid by Shiraori. The head is registered as our own entity type
 * so it counts as the egg owner's summon (IMagicSummon lifecycle); the body
 * and tail parts are still Alex's Mobs {@code EntityCentipedeBody} entities
 * spawned automatically by the head's tick(), which is fine because they are
 * pure visual chain segments with no AI of their own.
 *
 * <p>Segment count is forced to 3 (head + 1 body + 1 tail) in the convenience
 * constructor so the head builds the minimal chain on its first tick. The wild
 * centipede unconditionally targets Player / AbstractVillager / EntityCockroach,
 * so the target selector is wiped and replaced with the same owner-aware rules
 * the other summoned bugs use.
 */
public class SummonedCentipedeEntity extends EntityCentipedeHead implements IMagicSummon
{
    /** Alex's Mobs cave centipede hatches at the minimum natural segment count (head + body + tail). */
    public static final int SEGMENT_COUNT = 3;

    public SummonedCentipedeEntity(EntityType<? extends EntityCentipedeHead> type, Level level)
    {
        super(type, level);
        this.setPersistenceRequired();
    }

    public SummonedCentipedeEntity(Level level, @Nullable LivingEntity owner)
    {
        this((EntityType<? extends EntityCentipedeHead>) EntityRegistry.SUMMONED_CENTIPEDE.get(), level);
        // Force 3 segments before the first tick builds the body chain.
        this.setSegmentCount(SEGMENT_COUNT);
        if (owner != null)
        {
            SummonManager.setOwner(this, owner);
        }
        this.setHealth(this.getMaxHealth());
    }

    @Override
    protected void registerGoals()
    {
        super.registerGoals();
        // Strip the wild centipede's hostile target AI (HurtByTargetGoal +
        // NearestAttackableTargetGoal for Player / AbstractVillager /
        // EntityCockroach); owner-driven target goals take over.
        this.targetSelector.getAvailableGoals().removeIf(wrappedGoal ->
        {
            Goal goal = wrappedGoal.getGoal();
            return goal instanceof HurtByTargetGoal || goal instanceof NearestAttackableTargetGoal<?>;
        });

        // Owner-aware target goals: protect summoner, share target, retaliate.
        this.targetSelector.addGoal(1, new GenericOwnerHurtByTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(2, new GenericOwnerHurtTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(3, new GenericHurtByTargetGoal(this, entity -> io.entomology.entomology.util.SwarmCreatures.isSameOwnerChain(this, entity)).setAlertOthers());
        this.targetSelector.addGoal(4, new GenericProtectOwnerTargetGoal(this, this::getSummoner));

        // Auto-hunt nearby hostile mobs (excluding owner and owner's summons).
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                target -> !io.entomology.entomology.util.SwarmCreatures.isSameOwnerChain(this, target)
                        && target instanceof Enemy
        ));

        // Follow the owner so the centipede does not wander off on its own.
        this.goalSelector.addGoal(6, new GenericFollowOwnerGoal(this, this::getSummoner, 1.0D, 9.0F, 4.0F, false, 20.0F));
    }

    // ---- Summon lifecycle ----

    @Override
    public void onUnSummon()
    {
        if (!this.level().isClientSide)
        {
            for (int i = 0; i < 12; i++)
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
    public boolean canBeLeashed(Player player)
    {
        return false;
    }

    /**
     * Transient summon: lifespan is governed by SummonManager. Returning
     * false prevents the chunk serializer from writing a stale copy that
     * would be rebuilt as a NoAI shell on chunk reload.
     */
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
