package io.entomology.entomology.entity;

import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer for the Butterfly Princess. Uses a plain {@link GeoEntityRenderer}
 * instead of ISS's {@code AbstractSpellCastingMobRenderer} to avoid the
 * humanoid TransformStack crash. The entity's own animation controllers
 * (registered by {@code AbstractSpellCastingMob}) still drive casting/idle/walk
 * animations through GeckoLib.
 */
public class ButterflyPrincessRenderer extends GeoEntityRenderer<AbstractSpellCastingMob>
{
    public ButterflyPrincessRenderer(EntityRendererProvider.Context context)
    {
        super(context, new ButterflyPrincessModel());
    }
}
