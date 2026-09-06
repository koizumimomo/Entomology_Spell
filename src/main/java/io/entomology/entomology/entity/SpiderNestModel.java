package io.entomology.entomology.entity;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * Simple spider nest model inspired by Goety's spider nest block.
 * Creates a small cubic structure with glowing emission.
 */
public class SpiderNestModel extends GeoModel<SpiderNestEntity>
{
    private static final ResourceLocation MODEL_RESOURCE = ResourceLocation.fromNamespaceAndPath(
            "entomology_spell", "geo/spider_nest.geo.json");
    private static final ResourceLocation TEXTURE_RESOURCE = ResourceLocation.fromNamespaceAndPath(
            "entomology_spell", "textures/entity/spider_nest.png");
    private static final ResourceLocation ANIMATION_RESOURCE = ResourceLocation.fromNamespaceAndPath(
            "entomology_spell", "animations/spider_nest.animation.json");

    @Override
    public ResourceLocation getModelResource(SpiderNestEntity animatable)
    {
        return MODEL_RESOURCE;
    }

    @Override
    public ResourceLocation getTextureResource(SpiderNestEntity animatable)
    {
        return TEXTURE_RESOURCE;
    }

    @Override
    public ResourceLocation getAnimationResource(SpiderNestEntity animatable)
    {
        return ANIMATION_RESOURCE;
    }
}