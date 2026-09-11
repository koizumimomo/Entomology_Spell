package io.entomology.entomology.entity;

import io.entomology.entomology.EntomologyMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model for {@link ShiraoriEntity}. Points at the shiraori.geo.json
 * geometry, the shiraori.png texture, and the shiraori.animation.json animation
 * file, all served from this mod's asset namespace.
 */
public class ShiraoriModel extends GeoModel<ShiraoriEntity>
{
    private static final ResourceLocation MODEL_RESOURCE = ResourceLocation.fromNamespaceAndPath(
            EntomologyMod.MODID, "geo/shiraori.geo.json");
    private static final ResourceLocation TEXTURE_RESOURCE = ResourceLocation.fromNamespaceAndPath(
            EntomologyMod.MODID, "textures/entity/shiraori.png");
    private static final ResourceLocation ANIMATION_RESOURCE = ResourceLocation.fromNamespaceAndPath(
            EntomologyMod.MODID, "animations/shiraori.animation.json");

    @Override
    public ResourceLocation getModelResource(ShiraoriEntity animatable)
    {
        return MODEL_RESOURCE;
    }

    @Override
    public ResourceLocation getTextureResource(ShiraoriEntity animatable)
    {
        return TEXTURE_RESOURCE;
    }

    @Override
    public ResourceLocation getAnimationResource(ShiraoriEntity animatable)
    {
        return ANIMATION_RESOURCE;
    }
}
