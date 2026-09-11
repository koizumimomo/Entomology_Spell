package io.entomology.entomology.entity;

import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BugBeetleRenderer extends GeoEntityRenderer<BugBeetleEntity>
{
    public BugBeetleRenderer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context ctx)
    {
        super(ctx, new BugBeetleModel());
        this.shadowRadius = 0.4F;
    }
}
