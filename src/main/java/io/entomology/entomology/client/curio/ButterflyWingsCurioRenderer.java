package io.entomology.entomology.client.curio;

import io.entomology.entomology.client.item.ButterflyWingsBlueModel;
import io.entomology.entomology.client.item.ButterflyWingsWhiteModel;
import io.entomology.entomology.item.ButterflyWingsItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * Renders the Butterfly Wings GeckoLib model on the player's back when the
 * item is equipped in the Curios "back" slot. The blue and white variants
 * each have their own GeoModel; this renderer auto-selects the correct one
 * by overriding getGeoModel() based on the current item's registry name.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ButterflyWingsCurioRenderer extends GeoArmorRenderer<ButterflyWingsItem> implements ICurioRenderer
{
    private static final GeoModel<ButterflyWingsItem> BLUE_MODEL = new ButterflyWingsBlueModel();
    private static final GeoModel<ButterflyWingsItem> WHITE_MODEL = new ButterflyWingsWhiteModel();

    public ButterflyWingsCurioRenderer()
    {
        super(BLUE_MODEL);
    }

    @Override
    public GeoModel<ButterflyWingsItem> getGeoModel()
    {
        if (this.animatable != null && this.animatable.getDescriptionId().contains("white"))
        {
            return WHITE_MODEL;
        }
        return BLUE_MODEL;
    }

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack stack, SlotContext slotContext, PoseStack poseStack,
            RenderLayerParent<T, M> renderLayerParent, MultiBufferSource renderTypeBuffer,
            int light, float limbSwing, float limbSwingAmount, float partialTicks,
            float ageInTicks, float netHeadYaw, float headPitch)
    {
        LivingEntity entity = slotContext.entity();
        this.animatable = (ButterflyWingsItem) stack.getItem();

        HumanoidModel<?> baseModel = renderLayerParent.getModel() instanceof HumanoidModel<?> hm
                ? hm
                : new HumanoidModel(Minecraft.getInstance().getEntityModels().bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER));
        ICurioRenderer.followBodyRotations(entity, (HumanoidModel<LivingEntity>) baseModel);
        this.prepForRender(entity, stack, EquipmentSlot.CHEST, baseModel);

        ResourceLocation texture = this.getTextureLocation(this.animatable);
        RenderType renderType = this.getRenderType(this.animatable, texture, renderTypeBuffer, partialTicks);
        VertexConsumer buffer = renderTypeBuffer.getBuffer(renderType);

        this.defaultRender(poseStack, this.animatable, renderTypeBuffer, renderType, buffer, partialTicks, ageInTicks, light);
    }
}
