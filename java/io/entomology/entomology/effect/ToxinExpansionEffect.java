package io.entomology.entomology.effect;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.registries.EffectRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Summoned bee stings pile up toxins inside the victim: every sting raises the
 * level by one and refreshes the duration (up to level 255). When the effect
 * finally runs out, it bursts, dealing swarm school damage based on the number
 * of stacked levels.
 */
@Mod.EventBusSubscriber(modid = EntomologyMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ToxinExpansionEffect extends MobEffect
{
    public static final int BASE_DURATION_TICKS = 40; // 2 seconds
    public static final int MAX_AMPLIFIER = 254; // level 255
    public static final float BURST_DAMAGE_PER_LEVEL = 1.0F;
    private static final ResourceKey<DamageType> INSECT_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "insect_magic"));

    public ToxinExpansionEffect()
    {
        super(MobEffectCategory.HARMFUL, 0x9acd32);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event)
    {
        if (!(event.getSource().getEntity() instanceof Bee bee))
            return;
        if (SummonManager.getOwner(bee) == null)
            return;

        LivingEntity victim = event.getEntity();
        MobEffectInstance existing = victim.getEffect(EffectRegistry.TOXIN_EXPANSION.get());
        int amplifier = existing == null ? 0 : Math.min(MAX_AMPLIFIER, existing.getAmplifier() + 1);
        victim.addEffect(new MobEffectInstance(EffectRegistry.TOXIN_EXPANSION.get(), BASE_DURATION_TICKS, amplifier, false, false, true));
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event)
    {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null || instance.getEffect() != EffectRegistry.TOXIN_EXPANSION.get())
            return;

        LivingEntity host = event.getEntity();
        Level world = host.level();
        if (world.isClientSide || !host.isAlive())
            return;

        int layers = instance.getAmplifier() + 1;
        Holder<DamageType> insectMagic = world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(INSECT_MAGIC);
        host.hurt(new DamageSource(insectMagic), layers * BURST_DAMAGE_PER_LEVEL);

        MagicManager.spawnParticles(world, ParticleTypes.SNEEZE, host.getX(), host.getY() + 1.0, host.getZ(), 20, 0.35, 0.5, 0.35, 0.1, false);
    }
}
