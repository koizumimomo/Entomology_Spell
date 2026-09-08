package io.entomology.entomology.entity;

import io.entomology.entomology.entity.ai.SummonedBeeDespawnGoal;
import io.entomology.entomology.registries.EntityRegistry;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericFollowOwnerGoal;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Honey Courier (honey courier): a peaceful summoned bee that does not fight.
 * It flies off and wanders for a short spell-level-scaled time, then flies
 * back to its caster and delivers a bundle of health, mana and a little food
 * before vanishing. Lifetime is self-managed (no swarm buff dependency), so
 * the SummonedBeeDespawnGoal, the follow-owner goal and all targeting goals
 * are stripped.
 */
public class HoneyCourierBeeEntity extends SummonedBeeEntity
{
    private enum State
    {
        WANDERING, RETURNING
    }

    private State state = State.WANDERING;
    private int returnTime = 160; // ticks until it turns back (set by the spell)
    private float healAmount = 8.0F;
    private float manaAmount = 20.0F;
    private int foodAmount = 2;

    public HoneyCourierBeeEntity(EntityType<? extends Bee> entityType, Level level)
    {
        super(entityType, level);
    }

    public HoneyCourierBeeEntity(Level level, @Nullable LivingEntity owner)
    {
        this(EntityRegistry.HONEY_COURIER_BEE.get(), level);
        if (owner != null)
        {
            SummonManager.setOwner(this, owner);
        }
    }

    @Override
    protected void registerGoals()
    {
        super.registerGoals();
        // Strictly pacifist: no target goals at all
        this.targetSelector.getAvailableGoals().removeIf(wrapped -> true);
        // Its lifetime is the delivery schedule, not the swarm buff
        this.goalSelector.getAvailableGoals().removeIf(wrapped -> wrapped.getGoal() instanceof SummonedBeeDespawnGoal);
        // Let it wander off freely instead of hovering on the caster
        this.goalSelector.getAvailableGoals().removeIf(wrapped -> wrapped.getGoal() instanceof GenericFollowOwnerGoal);
    }

    public void setReturnTime(int returnTime)
    {
        this.returnTime = Math.max(20, returnTime);
    }

    public void setRewards(float healAmount, float manaAmount, int foodAmount)
    {
        this.healAmount = healAmount;
        this.manaAmount = manaAmount;
        this.foodAmount = foodAmount;
    }

    @Override
    public boolean isSwarmMember()
    {
        return false;
    }

    @Override
    public void tick()
    {
        super.tick();
        if (this.level().isClientSide)
        {
            return;
        }

        if (this.state == State.WANDERING)
        {
            if (this.tickCount % 20 == 0 && SummonManager.getOwner(this) == null)
            {
                // Owner gone (no summon tracking): bail out
                this.discard();
                return;
            }
            if (this.tickCount >= this.returnTime)
            {
                this.state = State.RETURNING;
                this.getNavigation().stop();
            }
            return;
        }

        // Returning: fly back to the caster and deliver
        LivingEntity owner = this.getOwnerLiving();
        if (owner == null || !owner.isAlive())
        {
            this.discard();
            return;
        }
        if (this.tickCount % 10 == 0 || !this.getNavigation().isInProgress())
        {
            this.getNavigation().moveTo(owner.getX(), owner.getY() + 1.0D, owner.getZ(), 1.25D);
        }
        if (this.distanceToSqr(owner) < 2.5D * 2.5D)
        {
            this.deliver(owner);
        }
        else if (this.tickCount > this.returnTime + 200)
        {
            // Couldn't reach the caster (blocked cave etc.) - grant it remotely
            this.deliver(owner);
        }
    }

    private void deliver(LivingEntity owner)
    {
        owner.heal(this.healAmount);

        if (owner instanceof Player player)
        {
            player.getFoodData().eat(this.foodAmount, 0.5F);
            MagicData magicData = MagicData.getPlayerMagicData(player);
            magicData.setMana(magicData.getMana() + this.manaAmount);
            if (owner instanceof ServerPlayer serverPlayer)
            {
                PacketDistributor.sendToPlayer(serverPlayer, new SyncManaPacket(magicData));
            }
        }

        Vec3 pos = owner.position();
        MagicManager.spawnParticles(this.level(), ParticleTypes.HEART,
                pos.x, pos.y + owner.getBbHeight() + 0.4D, pos.z, 5, 0.4D, 0.4D, 0.4D, 0.0D, false);
        MagicManager.spawnParticles(this.level(), ParticleTypes.WITCH,
                pos.x, pos.y + owner.getBbHeight() * 0.6D, pos.z, 12, 0.4D, 0.5D, 0.4D, 0.05D, false);
        MagicManager.spawnParticles(this.level(), ParticleTypes.FALLING_HONEY,
                this.getX(), this.getY() + 0.3D, this.getZ(), 12, 0.25D, 0.25D, 0.25D, 0.05D, false);
        this.level().playSound(null, owner, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.3F);

        this.discard();
    }

    @Nullable
    private LivingEntity getOwnerLiving()
    {
        net.minecraft.world.entity.Entity owner = SummonManager.getOwner(this);
        return owner instanceof LivingEntity living ? living : null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putInt("CourierState", this.state.ordinal());
        tag.putInt("CourierReturnTime", this.returnTime);
        tag.putFloat("CourierHeal", this.healAmount);
        tag.putFloat("CourierMana", this.manaAmount);
        tag.putInt("CourierFood", this.foodAmount);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        if (tag.contains("CourierState"))
        {
            this.state = State.values()[Math.min(1, tag.getInt("CourierState"))];
        }
        if (tag.contains("CourierReturnTime"))
        {
            this.returnTime = tag.getInt("CourierReturnTime");
        }
        if (tag.contains("CourierHeal"))
        {
            this.healAmount = tag.getFloat("CourierHeal");
        }
        if (tag.contains("CourierMana"))
        {
            this.manaAmount = tag.getFloat("CourierMana");
        }
        if (tag.contains("CourierFood"))
        {
            this.foodAmount = tag.getInt("CourierFood");
        }
    }
}
