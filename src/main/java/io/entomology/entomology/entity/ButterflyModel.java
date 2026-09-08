package io.entomology.entomology.entity;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model for the butterfly entity (group of 15 butterflies).
 * Uses the converted Blockbench model and idle animation.
 */
public class ButterflyModel extends GeoModel<ButterflyEntity>
{
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            "entomology_spell", "geo/butterfly.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "entomology_spell", "textures/entity/butterfly.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(
            "entomology_spell", "animations/butterfly.animation.json");

    @Override
    public ResourceLocation getModelResource(ButterflyEntity animatable)
    {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ButterflyEntity animatable)
    {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ButterflyEntity animatable)
    {
        return ANIMATION;
    }
}
