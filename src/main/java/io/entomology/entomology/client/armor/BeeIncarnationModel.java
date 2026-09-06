package io.entomology.entomology.client.armor;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.item.SwarmArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BeeIncarnationModel extends GeoModel<SwarmArmorItem>
{
    @Override
    public ResourceLocation getModelResource(SwarmArmorItem object)
    {
        return ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "geo/bee_incarnation.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SwarmArmorItem object)
    {
        return ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "textures/armor/bee_incarnation.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SwarmArmorItem animatable)
    {
        return ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "animations/swarm_armor_animation.json");
    }
}
