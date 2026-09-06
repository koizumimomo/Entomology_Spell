package io.entomology.entomology.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.BeeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the requiem projectile as a half-size vanilla bee (reusing the
 * vanilla bee model and texture). It is a projectile, not a living mob, so it
 * has no health, no death message, no aggro - it only ever homes onto the
 * spell's chosen target and detonates on impact.
 */
public class BeeRequiemRenderer extends EntityRenderer<BeeRequiemProjectile>
{
    private static final ResourceLocation BEE_TEXTURE = ResourceLocation.tryParse("textures/entity/bee/bee.png");
    private final BeeModel<?> model;

    public BeeRequiemRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        this.model = new BeeModel<>(context.bakeLayer(ModelLayers.BEE));
        this.shadowRadius = 0.2F;
    }

    @Override
    public void render(BeeRequiemProjectile entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
    {
        poseStack.pushPose();
        // Face the direction of travel: yaw + pitch
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-entityYaw));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-entity.getXRot()));
        // Half size
        poseStack.scale(0.5F, 0.5F, 0.5F);

        // Note: BeeModel.setupAnim() reads Bee.hasNectar()/isAngry() and NPEs on a
        // null entity, so we skip it and render the model in its default pose.
        // The wings sit folded; for a fast homing projectile that reads fine.

        RenderType renderType = this.model.renderType(BEE_TEXTURE);
        VertexConsumer consumer = bufferSource.getBuffer(renderType);
        // packed light, no hurt flash / no uv2 overlay, white tint
        this.model.renderToBuffer(poseStack, consumer, packedLight,
                OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(BeeRequiemProjectile entity)
    {
        return BEE_TEXTURE;
    }
}
