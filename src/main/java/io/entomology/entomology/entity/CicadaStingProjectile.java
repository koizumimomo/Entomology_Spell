package io.entomology.entomology.entity;

import io.entomology.entomology.registries.EntityRegistry;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;

/**
 * Cicada sting projectile: identical to the bee stinger but with no particle
 * trail. Fired by summoned cicadas at their target; on hit it deals its damage
 * (3-6, rolled by the shooter) and applies Slowness for 3 seconds.
 */
public class CicadaStingProjectile extends BeeStingerProjectile
{
    public CicadaStingProjectile(EntityType<? extends BeeStingerProjectile> entityType, Level level)
    {
        super(entityType, level);
    }

    public CicadaStingProjectile(Level level, LivingEntity shooter)
    {
        this(EntityRegistry.CICADA_STING.get(), level);
        this.setOwner(shooter);
    }

    @Override
    public void trailParticles()
    {
        // No particle trail
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult entityHitResult)
    {
        super.onHitEntity(entityHitResult);
        if (entityHitResult.getEntity() instanceof LivingEntity living)
        {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, true, true));
        }
    }
}
