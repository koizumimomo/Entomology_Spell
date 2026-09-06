package io.entomology.entomology.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.entomology.entomology.EntomologyMod;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.entity.spells.fireball.FireballRenderer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Renders the bee stinger projectile exactly like iron's magic missile, but
 * with the entomology texture and a yellow flare.
 */
public class BeeStingerRenderer extends EntityRenderer<BeeStingerProjectile>
{
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "textures/entity/bee_stinger.png");
    private static final ResourceLocation FLARE = IronsSpellbooks.id("textures/entity/lens_flare.png");
    private final ModelPart body;

    public BeeStingerRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        ModelPart modelpart = context.bakeLayer(FireballRenderer.MODEL_LAYER_LOCATION);
        this.body = modelpart.getChild("body");
    }

    @Override
    public void render(BeeStingerProjectile entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int light)
    {
        poseStack.pushPose();
        Vec3 motion = entity.getDeltaMovement();
        float xRot = -((float) (Mth.atan2(motion.horizontalDistance(), motion.y) * 57.2957763671875) - 90.0f);
        float yRot = -((float) (Mth.atan2(motion.z, motion.x) * 57.2957763671875) + 90.0f);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
        poseStack.scale(0.35f, 0.35f, 0.35f);
        VertexConsumer consumer = bufferSource.getBuffer(this.renderType(this.getTextureLocation(entity)));
        this.body.render(poseStack, consumer, 0xF000F0, OverlayTexture.NO_OVERLAY, 0.8f, 0.8f, 0.8f, 1.0f);
        poseStack.popPose();

        poseStack.pushPose();
        PoseStack.Pose pose = poseStack.last();
        Matrix4f poseMatrix = pose.pose();
        float f = (float) entity.tickCount + partialTicks;
        float scale = 0.5f + Mth.sin(f) * 0.125f;
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0f));
        poseStack.mulPose(Axis.XP.rotationDegrees(((float) entity.tickCount + partialTicks) * 15.0f));
        consumer = bufferSource.getBuffer(RenderType.entityTranslucent(FLARE));
        consumer.vertex(poseMatrix, 0.0f, -1.0f, -1.0f).color(255, 230, 100, 255).uv(0.0f, 1.0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0.0f, 1.0f, 0.0f).endVertex();
        consumer.vertex(poseMatrix, 0.0f, 1.0f, -1.0f).color(255, 230, 100, 255).uv(0.0f, 0.0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0.0f, 1.0f, 0.0f).endVertex();
        consumer.vertex(poseMatrix, 0.0f, 1.0f, 1.0f).color(255, 230, 100, 255).uv(1.0f, 0.0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0.0f, 1.0f, 0.0f).endVertex();
        consumer.vertex(poseMatrix, 0.0f, -1.0f, 1.0f).color(255, 230, 100, 255).uv(1.0f, 1.0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0.0f, 1.0f, 0.0f).endVertex();
        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, bufferSource, light);
    }

    public RenderType renderType(ResourceLocation texture)
    {
        return RenderType.energySwirl(texture, 0.0f, 0.0f);
    }

    @Override
    public ResourceLocation getTextureLocation(BeeStingerProjectile entity)
    {
        return TEXTURE;
    }
}
