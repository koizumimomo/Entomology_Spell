package io.entomology.entomology.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Same as the vanilla cave spider renderer (regular spider model scaled to
 * 0.7, cave spider texture) but typed to the mod's own summoned cave spider,
 * so it can be bound to its EntityType. Client-side only like all entity
 * renderers.
 */
public class SummonedCaveSpiderRenderer extends SpiderRenderer<SummonedCaveSpiderEntity>
{
    private static final ResourceLocation CAVE_SPIDER_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/spider/cave_spider.png");

    public SummonedCaveSpiderRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        this.shadowRadius *= 0.7F;
    }

    @Override
    protected void scale(SummonedCaveSpiderEntity entity, PoseStack poseStack, float partialTickTime)
    {
        poseStack.scale(0.7F, 0.7F, 0.7F);
    }

    @Override
    public ResourceLocation getTextureLocation(SummonedCaveSpiderEntity entity)
    {
        return CAVE_SPIDER_TEXTURE;
    }
}
