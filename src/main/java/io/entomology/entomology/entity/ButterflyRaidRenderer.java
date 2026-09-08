package io.entomology.entomology.entity;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * Renders nothing visible: the butterfly raid projectile is purely a particle
 * effect (the entity itself spawns particles via {@code impactParticles} and
 * {@code trailParticles} inherited from {@code AbstractMagicProjectile}).
 * Follows the same no-op pattern as {@link SwarmFireflyRenderer}.
 */
public class ButterflyRaidRenderer extends EntityRenderer<ButterflyRaidProjectile>
{
    public ButterflyRaidRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(ButterflyRaidProjectile entity)
    {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
