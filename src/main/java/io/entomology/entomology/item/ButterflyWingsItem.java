package io.entomology.entomology.item;

import io.entomology.entomology.client.item.ButterflyWingsItemRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.function.Consumer;

/**
 * Butterfly Wings — a Curios back-slot accessory that grants the wearer
 * creative-like flight (mayfly). Comes in blue and white variants, each with
 * its own GeckoLib geo/texture but sharing the butterfly idle animation.
 * The 3D model is used as the inventory icon (via GeoItemRenderer).
 */
public class ButterflyWingsItem extends Item implements ICurioItem, GeoItem
{
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.butterfly.idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ButterflyWingsItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, java.util.List<Component> tooltip, TooltipFlag flag)
    {
        tooltip.add(Component.translatable("tooltip.entomology_spell.butterfly_wings")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // ---- ICurioItem: creative flight ----

    @Override
    public void curioTick(SlotContext context, ItemStack stack)
    {
        LivingEntity entity = context.entity();
        if (entity instanceof Player player)
        {
            // Force mayfly every tick on both sides; client needs it to allow
            // double-jump-to-fly, server needs it to legitimise the state.
            player.getAbilities().mayfly = true;
            if (!player.level().isClientSide)
            {
                player.onUpdateAbilities();
            }
        }
    }

    @Override
    public void onEquip(SlotContext context, ItemStack oldStack, ItemStack newStack)
    {
        LivingEntity entity = context.entity();
        if (entity instanceof Player player)
        {
            player.getAbilities().mayfly = true;
            if (!player.level().isClientSide)
            {
                player.onUpdateAbilities();
            }
        }
    }

    @Override
    public void onUnequip(SlotContext context, ItemStack oldStack, ItemStack newStack)
    {
        LivingEntity entity = context.entity();
        if (entity instanceof Player player && !player.isCreative() && !player.isSpectator())
        {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            if (!player.level().isClientSide)
            {
                player.onUpdateAbilities();
            }
        }
    }

    // ---- GeoItem ----

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers)
    {
        controllers.add(new AnimationController<>(this, "butterfly_wings_controller", 5,
                state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache()
    {
        return this.cache;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer)
    {
        consumer.accept(new IClientItemExtensions()
        {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer()
            {
                return new ButterflyWingsItemRenderer();
            }
        });
    }
}
