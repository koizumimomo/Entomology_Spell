package io.entomology.entomology.event;

import io.entomology.entomology.EntomologyMod;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * A summoned bee that enters a bee nest or beehive loses its summon bond, so
 * when it comes back out it behaves as a completely normal bee.
 */
@Mod.EventBusSubscriber(modid = EntomologyMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SummonedBeeHiveHandler
{
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event)
    {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide || living.tickCount % 20 != 0)
            return;

        if (living instanceof Bee bee && SummonManager.getOwner(bee) != null && bee.hasHive())
        {
            SummonManager.removeSummon(bee);
        }
    }
}
