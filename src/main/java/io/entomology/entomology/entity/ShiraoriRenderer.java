package io.entomology.entomology.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer for {@link ShiraoriEntity}. Delegates geometry/texture/animation
 * resolution to {@link ShiraoriModel}. The shadow radius is bumped to 0.8F to
 * match Shiraori's larger, more imposing silhouette.
 */
public class ShiraoriRenderer extends GeoEntityRenderer<ShiraoriEntity>
{
    public ShiraoriRenderer(EntityRendererProvider.Context ctx)
    {
        super(ctx, new ShiraoriModel());
        this.shadowRadius = 0.8F;
    }
}
