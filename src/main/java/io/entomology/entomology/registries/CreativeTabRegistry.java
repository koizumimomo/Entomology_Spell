package io.entomology.entomology.registries;

import io.entomology.entomology.EntomologyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

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
                    })
                    .build());

    public static void register(IEventBus eventBus)
    {
        TABS.register(eventBus);
    }
}
