package io.entomology.entomology.client.item;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import io.entomology.entomology.item.ButterflyWingsItem;

public class ButterflyWingsWhiteModel extends GeoModel<ButterflyWingsItem>
{
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            "entomology_spell", "geo/butterfly_wings_white.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "entomology_spell", "textures/entity/butterfly_blue_white.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(
            "entomology_spell", "animations/butterfly.animation.json");

    @Override
    public ResourceLocation getModelResource(ButterflyWingsItem animatable)
    {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ButterflyWingsItem animatable)
    {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ButterflyWingsItem animatable)
    {
        return ANIMATION;
    }
}
