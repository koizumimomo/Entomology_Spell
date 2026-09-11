package io.entomology.entomology.entity;

import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CicadaRenderer extends GeoEntityRenderer<CicadaEntity>
{
    public CicadaRenderer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context ctx)
    {
        super(ctx, new CicadaModel());
        this.shadowRadius = 0.2F;
    }
}
