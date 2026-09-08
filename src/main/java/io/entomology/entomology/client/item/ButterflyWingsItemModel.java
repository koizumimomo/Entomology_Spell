package io.entomology.entomology.client.item;

import io.entomology.entomology.item.ButterflyWingsItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * Unified GeoModel for both butterfly wings variants. Selects the correct
 * geo/texture based on the item's description ID (blue vs white).
 * Animation is reused from the butterfly entity.
 */
public class ButterflyWingsItemModel extends GeoModel<ButterflyWingsItem>
{
    private static final ResourceLocation BLUE_MODEL = ResourceLocation.fromNamespaceAndPath(
            "entomology_spell", "geo/butterfly_wings_blue.geo.json");
    private static final ResourceLocation WHITE_MODEL = ResourceLocation.fromNamespaceAndPath(
            "entomology_spell", "geo/butterfly_wings_white.geo.json");
    private static final ResourceLocation BLUE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "entomology_spell", "textures/entity/butterfly_blue.png");
    private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "entomology_spell", "textures/entity/butterfly_blue_white.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(
            "entomology_spell", "animations/butterfly.animation.json");

    @Override
    public ResourceLocation getModelResource(ButterflyWingsItem animatable)
    {
        return animatable.getDescriptionId().contains("white") ? WHITE_MODEL : BLUE_MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ButterflyWingsItem animatable)
    {
        return animatable.getDescriptionId().contains("white") ? WHITE_TEXTURE : BLUE_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ButterflyWingsItem animatable)
    {
        return ANIMATION;
    }
}
