package io.entomology.entomology.entity;

import com.github.alexthe666.alexsmobs.entity.EntityWarpedMosco;
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
import net.minecraft.world.entity.Entity;
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
 * Summoned Warped Mosco (Alex's Mobs optional integration). Hatched rarely
 * (5%) from insect eggs laid by Shiraori. Retains the wild mosco's flight
 * /dash/spit combat loop (the inner {@code AttackGoal} + {@code AIWalkIdle}
 * registered in {@code EntityWarpedMosco.registerGoals()}) so it can actually
 * fight, but replaces the wild target selection (HurtByTargetGoal alerting
 * Crimson Mosquitoes / Warped Moscos, plus 3D-Nearest-Target against players
 * and tagged mobs) with the same owner-aware rules the other summoned bugs
 * use, so the mosco only attacks the owner's enemies.
 */
public class SummonedWarpedMoscoEntity extends EntityWarpedMosco implements IMagicSummon
{
    public SummonedWarpedMoscoEntity(EntityType<? extends EntityWarpedMosco> type, Level level)
    {
        super(type, level);
        this.setPersistenceRequired();
    }

    public SummonedWarpedMoscoEntity(Level level, @Nullable LivingEntity owner)
    {
        this((EntityType<? extends EntityWarpedMosco>) EntityRegistry.SUMMONED_WARPED_MOSCO.get(), level);
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
        // Strip the wild mosco's hostile target AI (HurtByTargetGoal +
        // EntityAINearestTarget3D against Player and tagged mobs); owner-driven
        // target goals take over. The combat goals (AttackGoal, AIWalkIdle,
        // LookAtPlayerGoal, RandomLookAroundGoal) stay so the mosco can still
        // fly, dash and spit at whatever target we hand it.
        this.targetSelector.getAvailableGoals().removeIf(wrappedGoal ->
        {
            Goal goal = wrappedGoal.getGoal();
            return goal instanceof HurtByTargetGoal || goal instanceof NearestAttackableTargetGoal<?>;
        });

        // Owner-aware target goals: protect summoner, share target, retaliate.
        this.targetSelector.addGoal(1, new GenericOwnerHurtByTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(2, new GenericOwnerHurtTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(3, new GenericHurtByTargetGoal(this, this::isAlly).setAlertOthers());
        this.targetSelector.addGoal(4, new GenericProtectOwnerTargetGoal(this, this::getSummoner));

        // Auto-hunt nearby hostile mobs (excluding owner and owner's summons).
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                target -> target.isAlive() && !this.isAlly(target) && target instanceof Enemy
        ));

        // Follow the owner between fights so the mosco does not stray.
        this.goalSelector.addGoal(6, new GenericFollowOwnerGoal(this, this::getSummoner, 1.0D, 12.0F, 5.0F, true, 24.0F));
    }

    /**
     * Ally check by UUID: the summoner, every other summon owned by the same
     * summoner (e.g. Shiraori and her brood), tamed pets of the summoner and
     * scoreboard allies are never valid targets. UUID comparison is used
     * instead of entity identity so the check survives entity lookups.
     */
    private boolean isAlly(@Nullable LivingEntity target)
    {
        if (target == null || target == this)
        {
            return true;
        }
        if (io.entomology.entomology.util.SwarmCreatures.isShiraoriAttendant(target))
        {
            return true;
        }
        Entity owner = this.getSummoner();
        if (owner == null)
        {
            return false;
        }
        java.util.UUID ownerId = owner.getUUID();
        if (target.getUUID().equals(ownerId))
        {
            return true;
        }
        Entity targetOwner = SummonManager.getOwner(target);
        if (targetOwner != null && targetOwner.getUUID().equals(ownerId))
        {
            return true;
        }
        // Transitive: target owned by a summon whose owner is the same player
        // (e.g. nest spiders are owned by Shiraori, who is owned by the player)
        if (targetOwner != null)
        {
            Entity grandOwner = SummonManager.getOwner(targetOwner);
            if (grandOwner != null && grandOwner.getUUID().equals(ownerId))
            {
                return true;
            }
        }
        if (target instanceof net.minecraft.world.entity.OwnableEntity ownable
                && ownable.getOwnerUUID() != null && ownable.getOwnerUUID().equals(ownerId))
        {
            return true;
        }
        return owner.isAlliedTo(target) || target.isAlliedTo(owner);
    }

    /**
     * Hard gate so no targeting path (retaliation, alert) can ever pick the
     * summoner or a same-owner summon such as Shiraori.
     */
    @Override
    public void setTarget(@Nullable LivingEntity target)
    {
        if (target != null && this.isAlly(target))
        {
            return;
        }
        super.setTarget(target);
    }

    // ---- Summon lifecycle ----

    @Override
    public void onUnSummon()
    {
        if (!this.level().isClientSide)
        {
            for (int i = 0; i < 16; i++)
            {
                this.level().addParticle(ParticleTypes.WITCH,
                        this.getX() + (this.random.nextDouble() - 0.5) * 0.8,
                        this.getY() + this.random.nextDouble() * this.getBbHeight(),
                        this.getZ() + (this.random.nextDouble() - 0.5) * 0.8,
                        0, 0.05, 0);
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
