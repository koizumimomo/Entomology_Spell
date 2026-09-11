package io.entomology.entomology.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 法阵渲染器：无阴影、不可见名牌。模型本身是两块 32x32 的平面（直径 2 格），
 * 直接铺在蜘蛛脚下。
 */
public class LeyLineAreaRenderer extends GeoEntityRenderer<LeyLineAreaEntity>
{
    public LeyLineAreaRenderer(EntityRendererProvider.Context ctx)
    {
        super(ctx, new LeyLineAreaModel());
        this.shadowRadius = 0.0F;
    }
}
