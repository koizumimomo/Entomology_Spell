package io.entomology.entomology.registries;

import io.entomology.entomology.EntomologyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = EntomologyMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CreativeTabRegistry
{
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EntomologyMod.MODID);

    public static final RegistryObject<CreativeModeTab> ENTOMOLOGY_TAB = TABS.register("entomology",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.entomology_spell"))
                    .icon(() -> new ItemStack(ItemRegistry.QUEEN_BEE_ICON.get()))
                    .displayItems((parameters, output) ->
                    {
                        output.accept(ItemRegistry.QUEEN_BEE_CROWN.get());
                        output.accept(ItemRegistry.WEAVER_SPIDER_CHELICERAE.get());
                        output.accept(ItemRegistry.BEE_INCARNATION.get());
                        output.accept(ItemRegistry.SWARM_SPELL_BOOK.get());
                        output.accept(ItemRegistry.SWARM_STAFF.get());
                        output.accept(ItemRegistry.SWARM_RUNE.get());
                        output.accept(ItemRegistry.SWARM_UPGRADE_ORB.get());
                        output.accept(ItemRegistry.ROYAL_JELLY.get());
                        output.accept(ItemRegistry.SPIDER_VENOM_GLAND.get());
                        output.accept(ItemRegistry.INSECT_CRYSTAL.get());
                        output.accept(ItemRegistry.VINE_WRAPPED_STICK.get());
                        output.accept(ItemRegistry.BUTTERFLY_SPIRIT.get());
                        output.accept(ItemRegistry.SHIRAORI_S_FANG.get());
                        output.accept(ItemRegistry.TRUE_QUEEN_CROWN.get());
                        output.accept(ItemRegistry.BUTTERFLY_WINGS_BLUE.get());
                        output.accept(ItemRegistry.BUTTERFLY_WINGS_WHITE.get());
                        output.accept(ItemRegistry.BUTTERFLY_SPAWN_EGG.get());
                        output.accept(ItemRegistry.NPC_BUTTERFLY_PRINCESS_SPAWN_EGG.get());
                        output.accept(ItemRegistry.CICADA_SPAWN_EGG.get());
                        output.accept(ItemRegistry.BUG_BEETLE_SPAWN_EGG.get());
                        output.accept(ItemRegistry.SHIRAORI_SPAWN_EGG.get());
                        output.accept(ItemRegistry.GUARDIAN_SPIDER_SPAWN_EGG.get());
                        output.accept(io.entomology.entomology.registries.BlockRegistry.INSECT_EGG_ITEM.get());
                    })
                    .build());

    public static void register(IEventBus eventBus)
    {
        TABS.register(eventBus);
    }

    @SubscribeEvent
    public static void onBuildCreativeTabContents(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS)
        {
            event.accept(ItemRegistry.BUTTERFLY_SPAWN_EGG.get());
            event.accept(ItemRegistry.NPC_BUTTERFLY_PRINCESS_SPAWN_EGG.get());
            event.accept(ItemRegistry.CICADA_SPAWN_EGG.get());
            event.accept(ItemRegistry.BUG_BEETLE_SPAWN_EGG.get());
            event.accept(ItemRegistry.SHIRAORI_SPAWN_EGG.get());
            event.accept(ItemRegistry.GUARDIAN_SPIDER_SPAWN_EGG.get());
        }
    }
}
