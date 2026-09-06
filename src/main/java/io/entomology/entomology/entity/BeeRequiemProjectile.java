package io.entomology.entomology.entity;

import io.entomology.entomology.compat.BumblezoneCompat;
import io.entomology.entomology.registries.EffectRegistry;
import io.entomology.entomology.registries.EntityRegistry;
import io.entomology.entomology.registries.SoundRegistry;
import io.entomology.entomology.registries.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Bee Requiem's kamikaze bee: a no-clip homing projectile that dives onto the
 * selected target, dealing swarm school damage and applying the chaotic
 * stinger debuffs while the caster carries the Chaotic Stinger effect.
 *
 * It is a projectile (not a living mob): no health, no death message, no
 * SummonManager tracking, no aggro - it only ever homes onto the spell's
 * chosen target and detonates on impact.
 */
public class BeeRequiemProjectile extends AbstractMagicProjectile
{
    private static final int MAX_LIFETIME = 200; // 10 seconds
    private static final int POISON_DURATION = 100;
    private static final int WITHER_DURATION = 80;
    private static final int PARALYZE_DURATION = 60;
    private static final float PARALYZE_CHANCE = 0.25F;
    private static final double HOMING_SPEED = 1.6D;

    @Nullable
    private LivingEntity target;

    public BeeRequiemProjectile(EntityType<? extends BeeRequiemProjectile> entityType, Level level)
    {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true; // phase through walls
    }

    public BeeRequiemProjectile(Level level, LivingEntity shooter)
    {
        this(EntityRegistry.BEE_REQUIEM.get(), level);
        this.setOwner(shooter);
    }

    public void setTarget(@Nullable LivingEntity target)
    {
        this.target = target;
    }

    @Override
    public void tick()
    {
        super.tick();
        if (!this.level().isClientSide)
        {
            if (this.target == null || this.target.isDeadOrDying() || this.tickCount > MAX_LIFETIME)
            {
                this.discard();
                return;
            }
            // Home onto the target
            Vec3 aim = this.target.position().add(0.0, this.target.getBbHeight() * 0.5, 0.0).subtract(this.position());
            if (aim.lengthSqr() > 1.0E-4)
            {
                this.setDeltaMovement(aim.normalize().scale(HOMING_SPEED));
            }
        }
    }

    @Override
    public void impactParticles(double x, double y, double z)
    {
        this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0.0, 0.0, 0.0);
    }

    @Override
    public float getSpeed()
    {
        return (float) HOMING_SPEED;
    }

    @Override
    public Optional<Supplier<net.minecraft.sounds.SoundEvent>> getImpactSound()
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
        AbstractSpell spell = SpellRegistry.BEE_REQUIEM_SPELL.get();
        DamageSources.applyDamage(entityHitResult.getEntity(), this.damage, spell.getDamageSource(this, this.getOwner()));

        // Chaotic Stinger synergy: apply the same random debuffs as summoned bee stings
        if (this.getOwner() instanceof LivingEntity owner)
        {
            MobEffectInstance stingerEffect = owner.getEffect(EffectRegistry.CHAOTIC_STINGER.get());
            if (stingerEffect != null && entityHitResult.getEntity() instanceof LivingEntity victim)
            {
                int amplifier = stingerEffect.getAmplifier();
                boolean poison = victim.level().getRandom().nextBoolean();
                MobEffectInstance debuff = poison
                        ? new MobEffectInstance(MobEffects.POISON, POISON_DURATION, amplifier, false, true, true)
                        : new MobEffectInstance(MobEffects.WITHER, WITHER_DURATION, amplifier, false, true, true);
                victim.addEffect(debuff);

                if (victim.level().getRandom().nextFloat() < PARALYZE_CHANCE)
                {
                    MobEffect paralyzed = BumblezoneCompat.getParalyzedEffect();
                    if (paralyzed != null && BumblezoneCompat.canParalyze(victim))
                    {
                        victim.addEffect(new MobEffectInstance(paralyzed, PARALYZE_DURATION, 0, false, true, true));
                    }
                }
            }
        }

        this.consumeEntityImpact(entityHitResult, true);
    }

    @Override
    public void trailParticles()
    {
        this.level().addParticle(ParticleTypes.SNEEZE,
                this.getX(), this.getY(), this.getZ(), 0.05, 0.05, 0.05);
    }
}
