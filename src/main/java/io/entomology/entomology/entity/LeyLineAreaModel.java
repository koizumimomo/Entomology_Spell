package io.entomology.entomology.entity;

import io.entomology.entomology.EntomologyMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class LeyLineAreaModel extends GeoModel<LeyLineAreaEntity>
{
    @Override
    public ResourceLocation getModelResource(LeyLineAreaEntity animatable)
    {
        return ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "geo/ley_lines_area.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(LeyLineAreaEntity animatable)
    {
        return ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "textures/entity/ley_lines_area.png");
    }

    @Override
    public ResourceLocation getAnimationResource(LeyLineAreaEntity animatable)
    {
        return ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "animations/ley_lines_area.animation.json");
    }
}
