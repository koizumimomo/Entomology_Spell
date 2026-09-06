package io.entomology.entomology.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironsspellbooks.render.GeoLivingEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public class SpiderNestRenderer extends GeoLivingEntityRenderer<SpiderNestEntity>
{
    public SpiderNestRenderer(EntityRendererProvider.Context context)
    {
        super(context, new SpiderNestModel());
    }

    @Override
    public void preRender(PoseStack poseStack, SpiderNestEntity animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource,
                          @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha)
    {
        // Scale based on health
        float healthPercent = animatable.getHealth() / animatable.getMaxHealth();
        float scale = 0.8f + (healthPercent * 0.2f); // Scale between 0.8 and 1.0
        
        poseStack.scale(scale, scale, scale);
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}