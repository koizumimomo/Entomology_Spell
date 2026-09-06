package io.entomology.entomology.entity;

import io.entomology.entomology.EntomologyMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class WebRootModel extends GeoModel<WebRootEntity>
{
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "textures/entity/web_root.png");
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "geo/web_root.geo.json");
    private static final ResourceLocation ANIMS = ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "animations/web_root_animations.json");

    @Override
    public ResourceLocation getTextureResource(WebRootEntity object)
    {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getModelResource(WebRootEntity object)
    {
        return MODEL;
    }

    @Override
    public ResourceLocation getAnimationResource(WebRootEntity animatable)
    {
        return ANIMS;
    }
}
