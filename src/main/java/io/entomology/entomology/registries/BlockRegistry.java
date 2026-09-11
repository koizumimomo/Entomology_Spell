package io.entomology.entomology.registries;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.block.InsectEggBlock;
import io.entomology.entomology.block.InsectEggBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BlockRegistry
{
    public static final RegistryObject<InsectEggBlock> INSECT_EGG = EntomologyMod.BLOCKS.register("insect_egg", InsectEggBlock::new);

    public static final RegistryObject<BlockEntityType<InsectEggBlockEntity>> INSECT_EGG_BLOCK_ENTITY =
            EntomologyMod.BLOCK_ENTITIES.register("insect_egg",
                    () -> BlockEntityType.Builder.of(InsectEggBlockEntity::new, INSECT_EGG.get()).build(null));

    public static final RegistryObject<Item> INSECT_EGG_ITEM = EntomologyMod.ITEMS.register("insect_egg",
            () -> new BlockItem(INSECT_EGG.get(), new Item.Properties()));

    public static void init()
    {
    }
}
