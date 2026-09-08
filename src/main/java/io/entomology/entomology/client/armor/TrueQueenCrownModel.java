package io.entomology.entomology.client.armor;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.item.SwarmArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TrueQueenCrownModel extends GeoModel<SwarmArmorItem>
{
    @Override
    public ResourceLocation getModelResource(SwarmArmorItem object)
    {
        return ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "geo/true_queen_crown.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SwarmArmorItem object)
    {
        return ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "textures/armor/true_queen_crown.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SwarmArmorItem animatable)
    {
        return ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "animations/true_queen_crown.animation.json");
    }
}
