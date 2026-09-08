package io.entomology.entomology.event;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.entity.ButterflyEntity;
import io.entomology.entomology.entity.NPCButterflyPrincessEntity;
import io.entomology.entomology.registries.EntityRegistry;
import io.entomology.entomology.registries.ItemRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Right-click interactions with wild butterflies:
 * <ul>
 *   <li>Royal Jelly → butterfly disappears, drops a Summon Butterfly level 1 scroll</li>
 *   <li>Insect Crystal → butterfly disappears, spawns an NPC Butterfly Princess</li>
 * </ul>
 * Only wild butterflies (not spell-summoned ones) respond to these interactions.
 */
@Mod.EventBusSubscriber(modid = EntomologyMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ButterflyInteractHandler
{
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event)
    {
        if (event.getLevel().isClientSide)
            return;

        if (!(event.getTarget() instanceof ButterflyEntity butterfly) || !butterfly.isWild())
            return;

        Player player = event.getEntity();
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty())
            return;

        ServerLevel serverLevel = (ServerLevel) event.getLevel();

        // Royal Jelly → Summon Butterfly level 1 scroll
        if (held.getItem() == ItemRegistry.ROYAL_JELLY.get())
        {
            ItemStack scroll = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
            ISpellContainer.createScrollContainer(
                    io.entomology.entomology.registries.SpellRegistry.SUMMON_BUTTERFLY_SPELL.get(), 1, scroll);

            if (!player.getAbilities().instabuild)
                held.shrink(1);

            if (!player.getInventory().add(scroll))
                butterfly.spawnAtLocation(scroll, 0.5F);

            butterfly.discard();
            MagicManager.spawnParticles(serverLevel, ParticleTypes.HAPPY_VILLAGER,
                    butterfly.getX(), butterfly.getY() + 0.5, butterfly.getZ(),
                    12, 0.3, 0.3, 0.3, 0.05, false);
            butterfly.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.5F);
            event.setCanceled(true);
        }
        // Insect Crystal → spawn NPC Butterfly Princess
        else if (held.getItem() == ItemRegistry.INSECT_CRYSTAL.get())
        {
            NPCButterflyPrincessEntity princess = new NPCButterflyPrincessEntity(
                    EntityRegistry.NPC_BUTTERFLY_PRINCESS.get(), serverLevel);
            princess.moveTo(butterfly.getX(), butterfly.getY(), butterfly.getZ(),
                    butterfly.getYRot(), butterfly.getXRot());
            princess.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(princess.blockPosition()),
                    net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED, null, null);
            serverLevel.addFreshEntity(princess);

            if (!player.getAbilities().instabuild)
                held.shrink(1);

            butterfly.discard();
            MagicManager.spawnParticles(serverLevel, ParticleTypes.END_ROD,
                    butterfly.getX(), butterfly.getY() + 0.5, butterfly.getZ(),
                    16, 0.3, 0.5, 0.3, 0.05, false);
            butterfly.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.2F);
            event.setCanceled(true);
        }
    }
}
