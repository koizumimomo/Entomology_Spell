package io.entomology.entomology.entity;

import io.entomology.entomology.EntomologyMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GuardianSpiderModel extends GeoModel<GuardianSpiderEntity>
{
    @Override
    public ResourceLocation getModelResource(GuardianSpiderEntity entity)
    {
        return ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "geo/guardian_spider.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GuardianSpiderEntity entity)
    {
        return ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "textures/entity/guardian_spider.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GuardianSpiderEntity entity)
    {
        return ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "animations/guardian_spider.animation.json");
    }
}
