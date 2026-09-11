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
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Summoned silverfish (its own registered entity type, not the vanilla one) so
 * the parasite spell keeps working in modpacks where natural spawning is
 * disabled or EntityType.SILVERFISH is removed. The vanilla goals that make
 * silverfish vanish into stone (SilverfishMergeWithStoneGoal) and call their
 * wild kin are removed; targeting is replaced with owner-driven summon goals,
 * so hatched fish only chase the parasite host (set by the effect) and
 * whoever attacks their summoner.
 */
public class SummonedSilverfishEntity extends Silverfish implements IMagicSummon
{
    public SummonedSilverfishEntity(EntityType<? extends Silverfish> entityType, Level level)
    {
        super(entityType, level);
        this.setPersistenceRequired();
    }

    public SummonedSilverfishEntity(Level level)
    {
        this(EntityRegistry.SUMMONED_SILVERFISH.get(), level);
    }

    @Override
    protected void registerGoals()
    {
        super.registerGoals();
        // Priorities 3 and 5 hold the private SilverfishWakeUpFriendsGoal and
        // SilverfishMergeWithStoneGoal: the latter buries the fish in stone
        // blocks (and effectively deletes the summon), the former calls every
        // wild silverfish in range.
        this.goalSelector.getAvailableGoals().removeIf(wrappedGoal ->
                wrappedGoal.getPriority() == 3 || wrappedGoal.getPriority() == 5);

        // Strip the wild fish's hostility (counterattack + player hunting);
        // owner-driven target goals take over.
        this.targetSelector.getAvailableGoals().removeIf(wrappedGoal ->
        {
            Goal goal = wrappedGoal.getGoal();
            return goal instanceof HurtByTargetGoal || goal instanceof NearestAttackableTargetGoal<?>;
        });

        this.targetSelector.addGoal(1, new GenericOwnerHurtByTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(2, new GenericOwnerHurtTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(3, new GenericCopyOwnerTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(4, new GenericHurtByTargetGoal(this, entity -> io.entomology.entomology.util.SwarmCreatures.isSameOwnerChain(this, entity)).setAlertOthers());
        this.targetSelector.addGoal(5, new GenericProtectOwnerTargetGoal(this, this::getSummoner));

        // Auto-hunt nearby hostile mobs (like summoned swarm bees do).
        this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                target -> !io.entomology.entomology.util.SwarmCreatures.isSameOwnerChain(this, target)
                        && target instanceof Enemy
        ));

        // No follow-owner goal on purpose: these fish are tied to the parasite
        // host rather than to the caster's side, and their lifetime is capped
        // by SummonManager.setDuration in the parasite effect.
    }

    // ---- Summon lifecycle ----

    @Override
    public void onUnSummon()
    {
        if (!this.level().isClientSide)
        {
            io.redspace.ironsspellbooks.capabilities.magic.MagicManager.spawnParticles(
                    this.level(), ParticleTypes.SNEEZE,
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

    /**
     * Transient summon: lifespan is governed by SummonManager's expiration
     * queue (set via setDuration in ParasiteEffect). Returning false prevents
     * the chunk serializer from writing a stale copy that would be rebuilt as
     * a NoAI shell on chunk reload (ISS's saveSummonerData saves the entity
     * explicitly via entity.save(), which is unaffected by this flag).
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
