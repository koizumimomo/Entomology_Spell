package io.entomology.entomology.entity;

import io.entomology.entomology.registries.EntityRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.mobs.ice_spider.IceSpiderEntity;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericCopyOwnerTargetGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericFollowOwnerGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericHurtByTargetGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericOwnerHurtByTargetGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericOwnerHurtTargetGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericProtectOwnerTargetGoal;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Summoned frost spider. Reuses Iron's Spellbooks' IceSpiderEntity (which already
 * supports player riding via getControllingPassenger/tickRidden) and turns it into
 * a friendly summon that protects and follows its caster. Right-clicking the spider
 * mounts it; the rider controls movement and attacks stay pointed by the owner AI.
 */
public class SummonedIceSpiderEntity extends IceSpiderEntity implements IMagicSummon
{
    // Synced owner id: SummonManager.getOwner() is server-only, so the client
    // needs this to predict the right-click mount (otherwise the interact
    // packet is never sent and riding does nothing).
    protected static final EntityDataAccessor<java.util.Optional<UUID>> DATA_OWNER_UUID =
            SynchedEntityData.defineId(SummonedIceSpiderEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public SummonedIceSpiderEntity(EntityType<? extends PathfinderMob> entityType, Level level)
    {
        super(entityType, level);
        this.setPersistenceRequired();
    }

    public SummonedIceSpiderEntity(Level level, LivingEntity owner)
    {
        this((EntityType<? extends PathfinderMob>) EntityRegistry.SUMMONED_ICE_SPIDER.get(), level);
        SummonManager.setOwner(this, owner);
        this.entityData.set(DATA_OWNER_UUID, java.util.Optional.of(owner.getUUID()));
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        this.entityData.define(DATA_OWNER_UUID, java.util.Optional.empty());
    }

    @Override
    protected void registerGoals()
    {
        super.registerGoals();
        // Strip the wild spider's hostility: it must never target players,
        // golems or prey on its own. Owner-driven target goals take over.
        this.targetSelector.getAvailableGoals().removeIf(wrappedGoal ->
        {
            Goal goal = wrappedGoal.getGoal();
            return goal instanceof NearestAttackableTargetGoal<?> || goal instanceof io.redspace.ironsspellbooks.entity.mobs.goals.MomentHurtByTargetGoal;
        });

        this.targetSelector.addGoal(1, new GenericOwnerHurtByTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(2, new GenericOwnerHurtTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(3, new GenericCopyOwnerTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(4, new GenericHurtByTargetGoal(this, entity -> entity == this.getSummoner()).setAlertOthers());
        this.targetSelector.addGoal(5, new GenericProtectOwnerTargetGoal(this, this::getSummoner));

        this.goalSelector.addGoal(7, new GenericFollowOwnerGoal(this, this::getSummoner, 1.0, 12.0F, 6.0F, false, 60.0F));
    }

    // ---- Riding ----

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand)
    {
        // Only the owner can mount; checked against the synced UUID so the client
        // can predict the interaction and actually send the packet to the server.
        if (this.getOwnerUUID() != null && this.getOwnerUUID().equals(player.getUUID()) && !this.isPassenger())
        {
            if (!this.level().isClientSide)
            {
                this.doPlayerRide(player);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @javax.annotation.Nullable
    public UUID getOwnerUUID()
    {
        return ((java.util.Optional<UUID>) this.entityData.get(DATA_OWNER_UUID)).orElse(null);
    }

    @Override
    public boolean isPickable()
    {
        // The wild spider returns false (its hitbox is handled by multipart parts);
        // we need right-click raytraces to hit the body so the owner can mount it.
        return true;
    }

    private void doPlayerRide(Player player)
    {
        player.setYRot(this.getYRot());
        player.setXRot(0.0F);
        player.startRiding(this, true);
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger()
    {
        // While grappling a victim the victim is the passenger but must not control.
        Entity passenger = this.getFirstPassenger();
        if (passenger != null && passenger.getUUID().equals(this.getGrappleTargetUUID()))
        {
            return null;
        }
        if (passenger instanceof Player player)
        {
            return player;
        }
        return null;
    }

    @Override
    public boolean canEntityDismount(Entity entity)
    {
        return true;
    }

    // ---- Summon lifecycle ----

    @Override
    public void onUnSummon()
    {
        if (!this.level().isClientSide)
        {
            MagicManagerHelper.spawnSnowParticles(this);
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

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        SummonedRestorationHelper.writeRestoreFlag(this, tag);
        UUID ownerUUID = this.getOwnerUUID();
        if (ownerUUID != null)
        {
            tag.putUUID("SummonOwner", ownerUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        SummonedRestorationHelper.readRestoreFlag(this, tag);
        if (tag.hasUUID("SummonOwner"))
        {
            this.entityData.set(DATA_OWNER_UUID, java.util.Optional.of(tag.getUUID("SummonOwner")));
        }
    }

    @Override
    public void tick()
    {
        super.tick();
        SummonedRestorationHelper.handleRestoredDespawn(this);
    }

    // Small indirection so the class does not need a static import block for MagicManager.
    private static final class MagicManagerHelper
    {
        static void spawnSnowParticles(SummonedIceSpiderEntity spider)
        {
            io.redspace.ironsspellbooks.capabilities.magic.MagicManager.spawnParticles(
                    spider.level(), ParticleTypes.SNOWFLAKE,
                    spider.getX(), spider.getY() + 1.0, spider.getZ(),
                    40, 0.5, 0.8, 0.5, 0.03, false);
        }
    }
}
