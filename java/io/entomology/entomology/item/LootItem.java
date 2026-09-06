package io.entomology.entomology.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Simple item that shows an extra translatable tooltip line, used by the
 * swarm school loot drops to explain how to obtain them.
 */
public class LootItem extends Item
{
    private final String tooltipKey;

    public LootItem(Item.Properties properties, String tooltipKey)
    {
        super(properties);
        this.tooltipKey = tooltipKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced)
    {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        tooltipComponents.add(Component.translatable(this.tooltipKey).withStyle(ChatFormatting.GRAY));
    }
}
