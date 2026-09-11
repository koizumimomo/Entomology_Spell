package io.entomology.entomology.event;

import io.entomology.entomology.EntomologyMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.util.ISlotHelper;

/**
 * Guarantees every player has at least one Curios "back" slot for the
 * Butterfly Wings. Curios ships a data-driven "back" slot definition, but
 * minimal mod sets or worlds created before the slot was available may leave
 * players without one; this enforces it on login and respawn.
 */
@Mod.EventBusSubscriber(modid = EntomologyMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CuriosSlotHandler
{
    private static final String BACK_SLOT = "back";

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer serverPlayer)
        {
            ensureBackSlot(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer serverPlayer)
        {
            ensureBackSlot(serverPlayer);
        }
    }

    private static void ensureBackSlot(ServerPlayer player)
    {
        try
        {
            ISlotHelper slotHelper = CuriosApi.getSlotHelper();
            if (slotHelper == null)
            {
                return;
            }
            // Only touch the player when the back slot type is actually loaded.
            if (slotHelper.getSlotType(BACK_SLOT).isEmpty())
            {
                return;
            }
            int current = slotHelper.getSlotsForType(player, BACK_SLOT);
            if (current < 1)
            {
                slotHelper.growSlotType(BACK_SLOT, 1 - current, player);
            }
        }
        catch (Throwable ignored)
        {
            // Curios is guaranteed (hard dependency of Iron's Spellbooks), but
            // never crash login over a cosmetic slot fix.
        }
    }
}
