package io.entomology.entomology.entity;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * Renders nothing: the swarm firefly is purely a particle effect
 * (the entity itself spawns the particles in {@code tick()}).
 */
public class SwarmFireflyRenderer extends EntityRenderer<SwarmFireflyProjectile>
{
    public SwarmFireflyRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(SwarmFireflyProjectile entity)
    {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
