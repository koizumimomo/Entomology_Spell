package io.entomology.entomology.entity;

import io.entomology.entomology.registries.EntityRegistry;
import io.entomology.entomology.registries.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
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
 * Swarm school magic missile: a tiny bee stinger that flies straight and deals
 * swarm school damage on impact. Modeled after iron's Magic Missile.
 */
public class BeeStingerProjectile extends AbstractMagicProjectile
{
    public BeeStingerProjectile(EntityType<? extends BeeStingerProjectile> entityType, Level level)
    {
        super(entityType, level);
        this.setNoGravity(true);
    }

    public BeeStingerProjectile(EntityType<? extends BeeStingerProjectile> entityType, Level levelIn, LivingEntity shooter)
    {
        this(entityType, levelIn);
        this.setOwner(shooter);
    }

    public BeeStingerProjectile(Level levelIn, LivingEntity shooter)
    {
        this(EntityRegistry.BEE_STINGER.get(), levelIn, shooter);
    }

    @Override
    public void impactParticles(double x, double y, double z)
    {
        this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0.0, 0.0, 0.0);
    }

    @Override
    public float getSpeed()
    {
        return 2.5f;
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
        AbstractSpell spell = SpellRegistry.BEE_STINGER_SPELL.get();
        DamageSources.applyDamage(entityHitResult.getEntity(), this.damage, spell.getDamageSource(this, this.getOwner()));
        this.consumeEntityImpact(entityHitResult, true);
    }

    @Override
    public void trailParticles()
    {
        // Small yellow/green trail (native addParticle: this runs client-side too)
        this.level().addParticle(ParticleHelper.UNSTABLE_ENDER,
                this.getX(), this.getY(), this.getZ(), 0.05, 0.05, 0.05);
    }
}
