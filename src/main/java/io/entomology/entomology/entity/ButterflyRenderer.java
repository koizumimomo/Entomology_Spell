package io.entomology.entomology.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer for the butterfly entity. Uses the group model (15 butterflies).
 */
public class ButterflyRenderer extends GeoEntityRenderer<ButterflyEntity>
{
    public ButterflyRenderer(EntityRendererProvider.Context context)
    {
        super(context, new ButterflyModel());
        this.shadowRadius = 0.3F;
    }
}
