package io.entomology.entomology.entity;

import io.entomology.entomology.EntomologyMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CicadaModel extends GeoModel<CicadaEntity>
{
    @Override
    public ResourceLocation getModelResource(CicadaEntity entity)
    {
        return ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "geo/cicada.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CicadaEntity entity)
    {
        // 30% chance for alternate color texture
        String textureName = entity.isAltColor() ? "cicada2" : "cicada";
        return ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "textures/entity/" + textureName + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(CicadaEntity entity)
    {
        return ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "animations/cicada.animation.json");
    }
}
