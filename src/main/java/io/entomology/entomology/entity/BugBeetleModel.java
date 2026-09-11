package io.entomology.entomology.entity;

import io.entomology.entomology.EntomologyMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BugBeetleModel extends GeoModel<BugBeetleEntity>
{
    @Override
    public ResourceLocation getModelResource(BugBeetleEntity entity)
    {
        return ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "geo/bug_beetle.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BugBeetleEntity entity)
    {
        return ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "textures/entity/bug_beetle.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BugBeetleEntity entity)
    {
        return ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "animations/bug_beetle.animation.json");
    }
}
