package io.entomology.entomology.entity;

import io.entomology.entomology.registries.EntityRegistry;
import io.entomology.entomology.registries.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Butterfly Raid projectile: a swarm of butterflies that rushes toward the
 * target and deals insect school damage on impact, then vanishes. Used by
 * the Butterfly Princess as her occasional attack.
 */
public class ButterflyRaidProjectile extends AbstractMagicProjectile
{
    public ButterflyRaidProjectile(EntityType<? extends ButterflyRaidProjectile> entityType, Level level)
    {
        super(entityType, level);
        this.setNoGravity(false);
    }

    public ButterflyRaidProjectile(EntityType<? extends ButterflyRaidProjectile> entityType, Level level, LivingEntity shooter)
    {
        this(entityType, level);
        this.setOwner(shooter);
    }

    public ButterflyRaidProjectile(Level level, LivingEntity shooter)
    {
        this(EntityRegistry.BUTTERFLY_RAID.get(), level, shooter);
    }

    @Override
    public void impactParticles(double x, double y, double z)
    {
        this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0.0, 0.0, 0.0);
    }

    @Override
    public float getSpeed()
    {
        return 1.8f;
    }

    @Override
    public Optional<Supplier<SoundEvent>> getImpactSound()
    {
        return Optional.of(() -> io.entomology.entomology.registries.SoundRegistry.INSECT_CAST.get());
    }

    @Override
    protected void onHitBlock(BlockHitResult blockHitResult)
    {
        super.onHitBlock(blockHitResult);
        this.discard();
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult entityHitResult)
    {
        super.onHitEntity(entityHitResult);
        Entity target = entityHitResult.getEntity();
        AbstractSpell spell = SpellRegistry.SUMMON_BUTTERFLY_SPELL.get();
        DamageSources.applyDamage(target, this.damage, spell.getDamageSource(this, this.getOwner()));
        this.discard();
    }

    @Override
    public void trailParticles()
    {
        this.level().addParticle(ParticleHelper.UNSTABLE_ENDER,
                this.getX(), this.getY(), this.getZ(), 0.05, 0.05, 0.05);
    }
}
