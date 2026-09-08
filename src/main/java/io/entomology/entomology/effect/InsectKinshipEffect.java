package io.entomology.entomology.effect;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.registries.EffectRegistry;
import io.entomology.entomology.util.SwarmCreatures;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Granted permanently while the Weaver Spider Chelicerae are worn. No insect
 * (silverfish, spiders, bees...) will ever pick the holder as an attack target.
 */
@Mod.EventBusSubscriber(modid = EntomologyMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class InsectKinshipEffect extends MobEffect
{
    public InsectKinshipEffect()
    {
        super(MobEffectCategory.BENEFICIAL, 0x8bc34a);
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event)
    {
        LivingEntity target = event.getNewTarget();
        if (target == null)
            return;
        // Swarm Exemption marks the target as fair game: insects may attack it
        // despite kinship (the swarm exemption spell's answer to kinship standoffs)
        if (target.hasEffect(EffectRegistry.SWARM_EXEMPTION.get()))
            return;
        if (!target.hasEffect(EffectRegistry.INSECT_KINSHIP.get()))
            return;
        if (SwarmCreatures.isSwarmCreature(event.getEntity()))
        {
            event.setCanceled(true);
        }
    }
}
