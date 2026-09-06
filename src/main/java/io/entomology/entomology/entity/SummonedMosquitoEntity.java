package io.entomology.entomology.entity;

import com.github.alexthe666.alexsmobs.entity.EntityCrimsonMosquito;
import com.github.alexthe666.alexsmobs.entity.ai.EntityAINearestTarget3D;
import io.entomology.entomology.registries.EffectRegistry;
import io.entomology.entomology.registries.EntityRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericHurtByTargetGoal;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

/**
 * Summoned crimson mosquito (Alex's Mobs optional integration). Keeps the wild
 * mosquito's dive-bite-retreat-spit combat loop (FlyTowardsTarget /
 * FlyAwayFromTarget / RandomFlyGoal) but replaces the wild target selection:
 * enemy players first, then hostile mobs, never the owner, same-owner allies
 * or players carrying the insect pheromone effect (multiplayer teammates).
 *
 * Every bit of damage it deals (bite while attached, blood spit after feeding)
 * pulses a small heal onto same-owner summons nearby - see
 * MosquitoLifestealHandler. Lifetime is short (20s, managed by SummonManager).
 */
public class SummonedMosquitoEntity extends EntityCrimsonMosquito implements IMagicSummon
{
    public static final double HEAL_RADIUS = 6.0D;
    private static final double OWNER_FOLLOW_DISTANCE_SQR = 12.0D * 12.0D;

    public SummonedMosquitoEntity(EntityType<? extends EntityCrimsonMosquito> entityType, Level level)
    {
        super(entityType, level);
        this.setPersistenceRequired();
    }

    public SummonedMosquitoEntity(Level level, LivingEntity owner)
    {
        this((EntityType<? extends EntityCrimsonMosquito>) EntityRegistry.SUMMONED_MOSQUITO.get(), level);
        if (owner != null)
        {
            SummonManager.setOwner(this, owner);
        }
        // Mosquitoes fight airborne: take off immediately instead of idling grounded.
        this.setFlying(true);
    }

    @Override
    protected void registerGoals()
    {
        super.registerGoals();
        // Replace all wild behaviour goals, then re-add the public flight
        // combat loop (dive -> mount & bite -> fly away -> spit blood).
        // RandomFlyGoal is package-private in Alex's Mobs, so idle hovering is
        // handled in tick() instead.
        this.goalSelector.getAvailableGoals().removeIf(wrapped -> true);
        this.targetSelector.getAvailableGoals().removeIf(wrapped -> true);
        this.goalSelector.addGoal(2, new FlyTowardsTarget(this));
        this.goalSelector.addGoal(2, new FlyAwayFromTarget(this));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 32.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        // Retaliation that also wakes the rest of the swarm
        this.targetSelector.addGoal(0, new GenericHurtByTargetGoal(this, entity -> entity == this.getSummoner()).setAlertOthers());
        // Priority 1: enemy players (PvP). Priority 2: hostile mobs (PvE default).
        this.targetSelector.addGoal(1, new EntityAINearestTarget3D<>(this, Player.class, 20, true, false, this::isEnemyOfOwner));
        this.targetSelector.addGoal(2, new EntityAINearestTarget3D<>(this, Mob.class, 50, false, false,
                target -> target instanceof Enemy && this.isEnemyOfOwner(target)));
    }

    /**
     * Target predicate: anything that is not the owner, not a same-owner summon
     * or pet, not scoreboard-allied with the owner, and not a player carrying
     * insect pheromone (treated as friendly so multiplayer teammates are safe).
     */
    private boolean isEnemyOfOwner(@Nullable LivingEntity target)
    {
        if (target == null || !target.isAlive() || target == this)
        {
            return false;
        }
        Entity owner = this.getSummoner();
        if (owner == null || target == owner)
        {
            return false;
        }
        if (target instanceof OwnableEntity ownable && ownable.getOwner() == owner)
        {
            return false;
        }
        if (target instanceof IMagicSummon summon && summon.getSummoner() == owner)
        {
            return false;
        }
        if (isFriendlyPheromonePlayer(target))
        {
            return false;
        }
        return !owner.isAlliedTo(target) && !target.isAlliedTo(owner);
    }

    /** Players carrying insect pheromone count as allies of the swarm. */
    private static boolean isFriendlyPheromonePlayer(LivingEntity target)
    {
        return target instanceof Player && target.hasEffect(EffectRegistry.INSECT_PHEROMONE.get());
    }

    /**
     * Choke point for every AI path that can pick a target (targeting goals,
     * hurt retaliation, swarm alert): a pheromone player is never accepted.
     */
    @Override
    public void setTarget(@Nullable LivingEntity target)
    {
        if (target != null && isFriendlyPheromonePlayer(target))
        {
            return;
        }
        super.setTarget(target);
    }

    @Override
    public void tick()
    {
        super.tick();
        SummonedRestorationHelper.handleRestoredDespawn(this);
        if (!this.level().isClientSide)
        {
            // Stay airborne for the whole (short) lifetime.
            if (!this.isFlying() && !this.isPassenger())
            {
                this.setFlying(true);
            }
            // Idle flight (replaces Alex's package-private RandomFlyGoal): hover
            // around the caster between kills, and return quickly if straying far.
            if (this.getTarget() == null && !this.isPassenger())
            {
                Entity owner = this.getSummoner();
                double anchorX = owner != null ? owner.getX() : this.getX();
                double anchorY = owner != null ? owner.getY() : this.getY();
                double anchorZ = owner != null ? owner.getZ() : this.getZ();
                this.setFlying(true);
                if (owner != null && this.distanceToSqr(owner) > OWNER_FOLLOW_DISTANCE_SQR)
                {
                    this.getMoveControl().setWantedPosition(anchorX, anchorY + 1.5D, anchorZ, 1.0D);
                }
                else if (!this.getMoveControl().hasWanted() && this.tickCount % 25 == 0)
                {
                    double angle = this.random.nextDouble() * Math.PI * 2.0D;
                    double dist = 2.0D + this.random.nextDouble() * 3.0D;
                    this.getMoveControl().setWantedPosition(
                            anchorX + Math.cos(angle) * dist,
                            anchorY + 1.0D + this.random.nextDouble() * 2.5D,
                            anchorZ + Math.sin(angle) * dist,
                            0.7D);
                }
            }
        }
    }

    /**
     * Called by {@link io.entomology.entomology.event.MosquitoLifestealHandler}
     * whenever this mosquito deals damage (2 HP bite / 4 HP blood spit).
     * Every 2 points of damage dealt heals 1 HP on same-owner summons nearby.
     */
    public void onDealtDamage(float damageAmount)
    {
        if (this.level().isClientSide)
        {
            return;
        }
        Entity owner = this.getSummoner();
        if (owner == null)
        {
            return;
        }
        int heal = Math.max(1, Mth.floor(damageAmount / 2.0F));
        AABB area = this.getBoundingBox().inflate(HEAL_RADIUS);
        for (Entity entity : this.level().getEntities(this, area, entity -> entity instanceof LivingEntity))
        {
            if (entity instanceof IMagicSummon allySummon && allySummon.getSummoner() == owner)
            {
                LivingEntity ally = (LivingEntity) entity;
                if (ally.getHealth() < ally.getMaxHealth())
                {
                    ally.heal(heal);
                    this.level().addParticle(ParticleTypes.HEART,
                            ally.getX(), ally.getY() + ally.getBbHeight() + 0.3D, ally.getZ(),
                            0.0D, 0.1D, 0.0D);
                }
            }
        }
    }

    // ---- Wild behaviour suppression ----

    /** Summons must never convert into a wild Warped Mosco after drinking. */
    @Override
    public void setSick(boolean sick)
    {
        // Deliberately no-op: the SICK synched data stays false forever.
    }

    @Override
    public boolean isNonMungusWarpedTrigger(Entity entity)
    {
        return false;
    }

    /** Summons drop no Alex's Mobs loot (blood sacs etc.) and no XP. */
    @Nullable
    @Override
    protected ResourceLocation getDefaultLootTable()
    {
        return null;
    }

    // ---- Summon lifecycle ----

    @Override
    public void onUnSummon()
    {
        if (!this.level().isClientSide)
        {
            for (int i = 0; i < 12; i++)
            {
                this.level().addParticle(ParticleTypes.CRIMSON_SPORE,
                        this.getX() + this.random.nextGaussian() * 0.4D,
                        this.getY() + 0.5D + this.random.nextGaussian() * 0.4D,
                        this.getZ() + this.random.nextGaussian() * 0.4D, 0.0D, 0.0D, 0.0D);
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
}
