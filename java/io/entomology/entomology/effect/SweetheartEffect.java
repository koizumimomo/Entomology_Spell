package io.entomology.entomology.effect;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.registries.EffectRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Granted permanently while the Queen Bee Crown is worn. Whenever the holder
 * (or one of their summons) deals damage, the victim is stacked with Infatuated
 * (up to level 5), each level raising its swarm damage taken by 5% and slowing
 * it by 10%.
 */
@Mod.EventBusSubscriber(modid = EntomologyMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SweetheartEffect extends MobEffect
{
    public static final int INFATUATION_DURATION_TICKS = 100; // 5 seconds

    public SweetheartEffect()
    {
        super(MobEffectCategory.BENEFICIAL, 0xff8fab);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event)
    {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker))
            return;

        // The crown wearer themselves or the owner of the attacking summon
        LivingEntity heartHolder = null;
        if (attacker.hasEffect(EffectRegistry.SWEETHEART.get()))
        {
            heartHolder = attacker;
        }
        else
        {
            Entity owner = SummonManager.getOwner(attacker);
            if (owner instanceof LivingEntity living && living.hasEffect(EffectRegistry.SWEETHEART.get()))
            {
                heartHolder = living;
            }
        }
        if (heartHolder == null)
            return;

        LivingEntity victim = event.getEntity();
        MobEffectInstance existing = victim.getEffect(EffectRegistry.INFATUATED.get());
        int amplifier = existing == null ? 0 : Math.min(InfatuatedEffect.MAX_AMPLIFIER, existing.getAmplifier() + 1);
        victim.addEffect(new MobEffectInstance(EffectRegistry.INFATUATED.get(), INFATUATION_DURATION_TICKS, amplifier, false, false, true));
    }
}
