package io.entomology.entomology.client.armor;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.item.SwarmArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class WeaverSpiderCheliceraeModel extends GeoModel<SwarmArmorItem>
{
    @Override
    public ResourceLocation getModelResource(SwarmArmorItem object)
    {
        return ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "geo/weaver_spider_chelicerae.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SwarmArmorItem object)
    {
        return ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "textures/armor/weaver_spider_chelicerae.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SwarmArmorItem animatable)
    {
        return ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "animations/swarm_armor_animation.json");
    }
}
