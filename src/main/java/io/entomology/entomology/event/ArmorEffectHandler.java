package io.entomology.entomology.event;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.registries.EffectRegistry;
import io.entomology.entomology.registries.ItemRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Armor set bonus effects granted while a swarm armor piece is worn and removed
 * the moment it is taken off:
 * - Queen Bee Crown  -> Sweetheart (infatuates targets of swarm spells)
 * - Bee Incarnation  -> Reproductive Desire (summons may hatch new summons)
 */
@Mod.EventBusSubscriber(modid = EntomologyMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ArmorEffectHandler
{
    private static final int REFRESH_DURATION = 40;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide)
            return;
        Player player = event.player;
        if (player.tickCount % 20 != 0)
            return;

        refresh(player, EquipmentSlot.HEAD, ItemRegistry.QUEEN_BEE_CROWN.get(), EffectRegistry.SWEETHEART.get());
        refresh(player, EquipmentSlot.CHEST, ItemRegistry.WEAVER_SPIDER_CHELICERAE.get(), EffectRegistry.INSECT_KINSHIP.get());
        refresh(player, EquipmentSlot.LEGS, ItemRegistry.BEE_INCARNATION.get(), EffectRegistry.REPRODUCTIVE_DESIRE.get());
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event)
    {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Player player))
            return;

        if (event.getSlot() == EquipmentSlot.HEAD)
        {
            apply(player, event.getTo().is(ItemRegistry.QUEEN_BEE_CROWN.get()), EffectRegistry.SWEETHEART.get());
        }
        else if (event.getSlot() == EquipmentSlot.CHEST)
        {
            apply(player, event.getTo().is(ItemRegistry.WEAVER_SPIDER_CHELICERAE.get()), EffectRegistry.INSECT_KINSHIP.get());
        }
        else if (event.getSlot() == EquipmentSlot.LEGS)
        {
            apply(player, event.getTo().is(ItemRegistry.BEE_INCARNATION.get()), EffectRegistry.REPRODUCTIVE_DESIRE.get());
        }
    }

    private static void refresh(Player player, EquipmentSlot slot, Item item, MobEffect effect)
    {
        ItemStack stack = player.getItemBySlot(slot);
        if (stack.is(item))
        {
            player.addEffect(new MobEffectInstance(effect, REFRESH_DURATION, 0, true, false, true));
        }
        else if (player.hasEffect(effect))
        {
            player.removeEffect(effect);
        }
    }

    private static void apply(Player player, boolean wearing, MobEffect effect)
    {
        if (wearing)
        {
            player.addEffect(new MobEffectInstance(effect, -1, 0, true, false, true));
        }
        else if (player.hasEffect(effect))
        {
            player.removeEffect(effect);
        }
    }
}
