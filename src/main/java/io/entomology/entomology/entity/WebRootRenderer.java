package io.entomology.entomology.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironsspellbooks.render.GeoLivingEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public class WebRootRenderer extends GeoLivingEntityRenderer<WebRootEntity>
{
    public WebRootRenderer(EntityRendererProvider.Context context)
    {
        super(context, new WebRootModel());
    }

    @Override
    public void preRender(PoseStack poseStack, WebRootEntity animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource,
                          @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha)
    {
        Entity rooted = animatable.getFirstPassenger();
        if (rooted != null)
        {
            float scale = rooted.getBbWidth() / 0.6f;
            poseStack.scale(scale, scale, scale);
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
