package io.entomology.entomology.entity;

import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GuardianSpiderRenderer extends GeoEntityRenderer<GuardianSpiderEntity>
{
    public GuardianSpiderRenderer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context ctx)
    {
        super(ctx, new GuardianSpiderModel());
        this.shadowRadius = 1.0F;
    }
}
