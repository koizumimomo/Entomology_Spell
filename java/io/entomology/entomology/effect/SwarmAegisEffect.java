package io.entomology.entomology.effect;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.registries.EffectRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Reduces incoming damage by a flat amount plus a percentage per level.
 * Level 1 (amplifier 0) reduces damage by 5 points + 10% of what remains.
 */
@Mod.EventBusSubscriber(modid = EntomologyMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SwarmAegisEffect extends MobEffect
{
    public static final float FLAT_PER_LEVEL = 5.0F;
    public static final float PERCENT_PER_LEVEL = 0.10F;

    public SwarmAegisEffect()
    {
        super(MobEffectCategory.BENEFICIAL, 0x9acd32);
    }

    public static float getFlatReduction(int amplifier)
    {
        return FLAT_PER_LEVEL * (amplifier + 1);
    }

    public static float getPercentReduction(int amplifier)
    {
        return PERCENT_PER_LEVEL * (amplifier + 1);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event)
    {
        LivingEntity victim = event.getEntity();
        MobEffectInstance effect = victim.getEffect(EffectRegistry.SWARM_AEGIS.get());
        if (effect == null)
            return;

        int amplifier = effect.getAmplifier();
        float flat = SwarmAegisEffect.getFlatReduction(amplifier);
        float percent = SwarmAegisEffect.getPercentReduction(amplifier);
        float reduced = Math.max(0.0F, (event.getAmount() - flat) * (1.0F - percent));
        event.setAmount(reduced);
    }
}
