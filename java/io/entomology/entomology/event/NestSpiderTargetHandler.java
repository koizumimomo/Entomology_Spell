package io.entomology.entomology.event;

import io.entomology.entomology.EntomologyMod;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Summoned mobs never pick other summons (including the owner) as attack
 * targets, otherwise the spiders spawned by a nest would tear each other apart.
 */
@Mod.EventBusSubscriber(modid = EntomologyMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NestSpiderTargetHandler
{
    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event)
    {
        if (event.getEntity() instanceof Mob mob && event.getNewTarget() != null)
        {
            Entity owner = SummonManager.getOwner(mob);
            if (owner == null)
                return;

            // Never target the owner, and never target other summons
            if (event.getNewTarget() == owner || SummonManager.getOwner(event.getNewTarget()) != null)
            {
                event.setCanceled(true);
            }
        }
    }
}
