package io.entomology.entomology.client.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * Custom GeoItemRenderer for butterfly wings. The model's root bone pivot is
 * at y=24 (entity coordinate system), which causes the model to render too
 * high in the GUI item slot. This renderer applies a Y-translation and scale
 * to centre the butterfly model in the slot.
 */
public class ButterflyWingsItemRenderer extends GeoItemRenderer<io.entomology.entomology.item.ButterflyWingsItem>
{
    public ButterflyWingsItemRenderer()
    {
        super(new ButterflyWingsItemModel());
        this.withScale(0.4f);
        this.useAlternateGuiLighting();
    }

    @Override
    protected void renderInGui(ItemDisplayContext transformType, PoseStack poseStack,
                               MultiBufferSource bufferSource, int packedLight, int packedOverlay)
    {
        // Model renders slightly left of centre; shift right to compensate.
        poseStack.translate(0.4f, 0, 0);
        super.renderInGui(transformType, poseStack, bufferSource, packedLight, packedOverlay);
    }
}
