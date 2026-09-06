package io.entomology.entomology.entity;

import com.github.alexthe666.alexsmobs.entity.EntityCockroach;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericFollowOwnerGoal;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Summoned dancing cockroach. Pure support summon: it permanently holds maracas
 * (which forces the vanilla dance animation, standing upright and shaking them)
 * and every few seconds pulses a "rhythm aura" of regen/speed/haste/strength onto
 * friendly creatures nearby. No combat AI at all.
 */
public class SummonedCockroach extends EntityCockroach implements IMagicSummon
{
    public static final int AURA_RADIUS = 8;
    public static final int AURA_INTERVAL_TICKS = 60;
    public static final int BUFF_DURATION_TICKS = 80; // 4s, > interval so it never flickers

    private int spellLevel = 1;

    public SummonedCockroach(EntityType<? extends EntityCockroach> entityType, Level level)
    {
        super(entityType, level);
        // Holding maracas makes the parent tick force dancing every frame,
        // including the La Cucaracha status broadcast (byte 67) handled client-side.
        this.setMaracas(true);
    }

    public SummonedCockroach(Level level, LivingEntity owner, int spellLevel)
    {
        this((EntityType<? extends EntityCockroach>) io.entomology.entomology.registries.EntityRegistry.SUMMONED_COCKROACH.get(), level);
        SummonManager.setOwner(this, owner);
        this.spellLevel = spellLevel;
    }

    @Override
    protected void registerGoals()
    {
        // Strip all wild behaviour (panic, flee from players/light, breeding,
        // tempting, item targeting) and replace with a calm follower routine.
        this.goalSelector.getAvailableGoals().removeIf(wrapped -> true);
        this.targetSelector.getAvailableGoals().removeIf(wrapped -> true);
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(6, new GenericFollowOwnerGoal(this, this::getSummoner, 1.0, 4.0F, 2.0F, false, 20.0F));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick()
    {
        super.tick();
        SummonedRestorationHelper.handleRestoredDespawn(this);
        // Re-broadcast the La Cucaracha status (byte 67) every couple of seconds so
        // the client keeps playing the maracas BGM. The parent's tick() already
        // does this, but it is gated on isSilent() and random chance - sending it
        // ourselves on a fixed cadence guarantees the music does not drop out.
        if (!this.level().isClientSide && this.hasMaracas() && this.tickCount % 40 == 0)
        {
            this.level().broadcastEntityEvent(this, (byte) 67);
        }
        if (!this.level().isClientSide && this.tickCount % AURA_INTERVAL_TICKS == 0)
        {
            this.pulseRhythmAura();
        }
    }

    /**
     * Buff pulse: friendly creatures around the troupe get regen + speed + haste
     * + strength. Amplifiers scale with spell level (level 3+: regen II, level 5:
     * everything II). Hostile mobs are deliberately unaffected.
     */
    private void pulseRhythmAura()
    {
        int regenAmp = this.spellLevel >= 3 ? 1 : 0;
        int allAmp = this.spellLevel >= 5 ? 1 : 0;
        for (Entity entity : this.level().getEntities(this, this.getBoundingBox().inflate(AURA_RADIUS), this::isBuffableFriend))
        {
            LivingEntity friend = (LivingEntity) entity;
            this.applyAuraEffect(friend, MobEffects.REGENERATION, regenAmp);
            this.applyAuraEffect(friend, MobEffects.MOVEMENT_SPEED, allAmp);
            this.applyAuraEffect(friend, MobEffects.DIG_SPEED, allAmp);
            this.applyAuraEffect(friend, MobEffects.DAMAGE_BOOST, allAmp);
            this.level().addParticle(ParticleTypes.NOTE, friend.getX(), friend.getY() + friend.getBbHeight() + 0.3, friend.getZ(), 0, 0, 0);
        }
    }

    private void applyAuraEffect(LivingEntity friend, MobEffect effect, int amplifier)
    {
        MobEffectInstance current = friend.getEffect(effect);
        // Only refresh when about to run out, so stronger external buffs aren't downgraded
        if (current == null || current.getDuration() <= AURA_INTERVAL_TICKS || current.getAmplifier() < amplifier)
        {
            friend.addEffect(new MobEffectInstance(effect, BUFF_DURATION_TICKS, amplifier, true, true, true));
        }
    }

    private boolean isBuffableFriend(Entity entity)
    {
        if (!(entity instanceof LivingEntity living) || living == this)
        {
            return false;
        }
        Entity owner = this.getSummoner();
        if (living == owner)
        {
            return true;
        }
        if (owner == null)
        {
            return false;
        }
        if (living instanceof Player)
        {
            return owner.isAlliedTo(living);
        }
        if (living instanceof OwnableEntity ownable)
        {
            return ownable.getOwner() == owner;
        }
        if (living instanceof IMagicSummon summon)
        {
            return summon.getSummoner() == owner;
        }
        return false;
    }

    // ---- Summon lifecycle ----

    @Override
    public void onUnSummon()
    {
        if (!this.level().isClientSide)
        {
            for (int i = 0; i < 15; i++)
            {
                this.level().addParticle(ParticleTypes.NOTE,
                        this.getX() + this.random.nextGaussian() * 0.4,
                        this.getY() + 0.5 + this.random.nextGaussian() * 0.4,
                        this.getZ() + this.random.nextGaussian() * 0.4, 0, 0, 0);
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

    // ---- Cross-world restore handling ----

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        SummonedRestorationHelper.writeRestoreFlag(this, tag);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        SummonedRestorationHelper.readRestoreFlag(this, tag);
    }
}
