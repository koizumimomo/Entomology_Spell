package io.entomology.entomology.entity;

import io.entomology.entomology.registries.EntityRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * One-shot firefly strike for the Swarm Aid effect, modelled after Iron's
 * Spellbooks' Echoing Strikes sword ({@code EchoingSword}): it homes in on the
 * attacked target, deals its damage exactly once and immediately discards
 * itself. This replaces the persistent 10-second {@code FireflySwarmProjectile}
 * whose entities piled up during rapid attacks and caused lag.
 *
 * <p>The entity is a lightweight non-living projectile: no AI goals, no
 * attributes, never saved to disk. Its visual is purely particles, matching
 * Iron's Spells' own firefly swarm.</p>
 */
public class SwarmFireflyProjectile extends Entity
{
    private static final double STRIKE_RANGE_SQR = 1.5 * 1.5;
    private static final float FLIGHT_SPEED = 0.35F;
    /** Failsafe: strike wherever we are after this many ticks (EchoingSword dies at 45). */
    private static final int MAX_LIFETIME = 30;

    private float damage;
    private UUID ownerUUID;
    private Entity cachedOwner;
    private UUID targetUUID;

    public SwarmFireflyProjectile(EntityType<? extends SwarmFireflyProjectile> type, Level level)
    {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public SwarmFireflyProjectile(Level level, Entity owner, LivingEntity target, float damage)
    {
        this(EntityRegistry.SWARM_FIREFLY.get(), level);
        this.setOwner(owner);
        this.targetUUID = target.getUUID();
        this.damage = damage;
        this.setPos(owner.getX(), owner.getY() + 1.0, owner.getZ());
    }

    public void setOwner(Entity owner)
    {
        if (owner != null)
        {
            this.ownerUUID = owner.getUUID();
            this.cachedOwner = owner;
        }
    }

    public Entity getOwner()
    {
        if (this.cachedOwner != null && !this.cachedOwner.isRemoved())
            return this.cachedOwner;
        if (this.ownerUUID != null && this.level() instanceof ServerLevel serverLevel)
        {
            this.cachedOwner = serverLevel.getEntity(this.ownerUUID);
            return this.cachedOwner;
        }
        return null;
    }

    public LivingEntity getTarget()
    {
        if (this.targetUUID == null)
            return null;
        Entity entity = this.level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(this.targetUUID) : null;
        return entity instanceof LivingEntity living ? living : null;
    }

    @Override
    public void tick()
    {
        super.tick();

        // Particle-only visual, same as Iron's Spells' firefly swarm entity
        if (this.level().isClientSide)
        {
            for (int i = 0; i < 2; i++)
            {
                Vec3 motion = Utils.getRandomVec3(0.05F).add(this.getDeltaMovement());
                Vec3 spawn = Utils.getRandomVec3(0.25);
                this.level().addParticle(ParticleHelper.FIREFLY,
                        this.getX() + spawn.x,
                        this.getY() + this.getBbHeight() * 0.5F + spawn.z,
                        this.getZ() + spawn.z,
                        motion.x, motion.y, motion.z);
            }
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive())
        {
            this.discard();
            return;
        }

        // Home in on the target's center (EchoingSword-style flight)
        Vec3 targetCenter = target.getBoundingBox().getCenter();
        Vec3 direction = targetCenter.subtract(this.position());
        this.setDeltaMovement(direction.normalize().scale(FLIGHT_SPEED));
        this.move(MoverType.SELF, this.getDeltaMovement());

        // Strike exactly once, then vanish
        if (this.distanceToSqr(targetCenter.x, targetCenter.y, targetCenter.z) < STRIKE_RANGE_SQR
                || this.tickCount >= MAX_LIFETIME)
        {
            this.performHit(target);
            this.discard();
        }
    }

    private void performHit(LivingEntity target)
    {
        this.playSound((SoundEvent) SoundRegistry.FIREFLY_SWARM_ATTACK.get(), 0.75F,
                0.9F + this.random.nextFloat() * 0.2F);
        if (this.level() instanceof ServerLevel serverLevel)
            serverLevel.sendParticles(ParticleHelper.FIREFLY,
                    this.getX(), this.getY(), this.getZ(), 12, 0.2, 0.2, 0.2, 0.05);
        // Clear hurt invulnerability so the strike always lands (EchoingSword pattern)
        target.invulnerableTime = 0;
        DamageSources.applyDamage(target, this.damage,
                SpellRegistry.FIREFLY_SWARM_SPELL.get().getDamageSource(this, this.getOwner()));
    }

    @Override
    public boolean shouldBeSaved()
    {
        return false;
    }

    @Override
    public boolean isPickable()
    {
        return false;
    }

    @Override
    protected void defineSynchedData()
    {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound)
    {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound)
    {
    }
}
