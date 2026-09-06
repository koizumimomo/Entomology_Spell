package io.entomology.entomology.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * ArmorItem that carries extra spell-related attributes (cast time reduction,
 * max mana, cooldown reduction) on top of its defense values.
 * Rendered in 3D via GeckoLib (like Iron's Spells armor), so the worn model is
 * a BlockBench Geo model instead of the vanilla 2D armor layer.
 * Each piece gets a stable UUID per attribute, derived from the item name, so
 * re-equipping can never stack duplicate modifiers.
 */
public abstract class SwarmArmorItem extends ArmorItem implements GeoItem
{
    private final Multimap<Attribute, AttributeModifier> extraModifiers;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private final String setBonusTooltipKey;

    public SwarmArmorItem(String name, ArmorMaterial material, Type type, Item.Properties properties, AttributeContainer... attributes)
    {
        super(material, type, properties);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        for (AttributeContainer container : attributes)
        {
            UUID uuid = UUID.nameUUIDFromBytes((name + "/" + container.attribute().get().getDescriptionId()).getBytes());
            builder.put(container.attribute().get(),
                    new AttributeModifier(uuid, "Swarm armor modifier", container.value(), container.operation()));
        }
        this.extraModifiers = builder.build();
        this.setBonusTooltipKey = switch (name)
        {
            case "queen_bee_crown" -> "tooltip.entomology_spell.queen_bee_crown";
            case "weaver_spider_chelicerae" -> "tooltip.entomology_spell.weaver_spider_chelicerae";
            case "bee_incarnation" -> "tooltip.entomology_spell.bee_incarnation";
            default -> null;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced)
    {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        if (this.setBonusTooltipKey != null)
        {
            tooltipComponents.add(Component.translatable(this.setBonusTooltipKey).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot)
    {
        Multimap<Attribute, AttributeModifier> modifiers = ImmutableMultimap.<Attribute, AttributeModifier>builder()
                .putAll(super.getDefaultAttributeModifiers(slot))
                .build();
        if (slot == this.type.getSlot())
        {
            return ImmutableMultimap.<Attribute, AttributeModifier>builder()
                    .putAll(modifiers)
                    .putAll(this.extraModifiers)
                    .build();
        }
        return modifiers;
    }

    // -------- GeckoLib --------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar)
    {
        // No animations for now; the model is rendered statically.
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
            private GeoArmorRenderer<?> renderer;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original)
            {
                if (this.renderer == null)
                {
                    this.renderer = SwarmArmorItem.this.supplyRenderer();
                }
                this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                return this.renderer;
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    public abstract GeoArmorRenderer<?> supplyRenderer();
}
