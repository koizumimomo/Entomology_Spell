package io.entomology.entomology.event;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.entity.SummonedMosquitoEntity;
import io.entomology.entomology.registries.SpellRegistry;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Blood symbiosis: when a summoned crimson mosquito deals damage (its 2 HP
 * mounted bite or its 4 HP blood spit - the spit's owner is the mosquito),
 * it heals same-owner summons nearby. Only exists when Alex's Mobs is
 * installed; the SummonedMosquitoEntity reference stays unloaded otherwise.
 */
@Mod.EventBusSubscriber(modid = EntomologyMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MosquitoLifestealHandler
{
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event)
    {
        if (event.isCanceled() || !SpellRegistry.isAlexsMobsLoaded())
        {
            return;
        }
        Entity attacker = event.getSource() != null ? event.getSource().getEntity() : null;
        if (attacker instanceof SummonedMosquitoEntity mosquito)
        {
            mosquito.onDealtDamage(event.getAmount());
            // Inflict Weakness I (5s) on the target hit by the mosquito
            LivingEntity target = event.getEntity();
            if (target != null && target.isAlive())
            {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, false, false, true));
            }
        }
    }
}
