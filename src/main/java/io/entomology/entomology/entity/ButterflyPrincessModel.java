package io.entomology.entomology.entity;

import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model for the Butterfly Princess (both the summoned and NPC
 * variants share the same geometry/texture/animation).
 *
 * <p>Extends the plain {@link GeoModel} rather than ISS's
 * {@code AbstractSpellCastingMobModel}: the latter's
 * {@code handleAnimations} drives a {@code TransformStack} that assumes a
 * humanoid bone layout (body/head/arms/legs). The butterfly princess model
 * uses non-humanoid bone names, so that TransformStack returns null bones and
 * crashes the renderer. A plain GeoModel still plays the animation
 * controllers registered on the entity (including ISS's cast animations).</p>
 */
public class ButterflyPrincessModel extends GeoModel<AbstractSpellCastingMob>
{
    private static final ResourceLocation MODEL_RESOURCE = ResourceLocation.fromNamespaceAndPath(
            "entomology_spell", "geo/butterfly_princess.geo.json");
    private static final ResourceLocation TEXTURE_RESOURCE = ResourceLocation.fromNamespaceAndPath(
            "entomology_spell", "textures/entity/butterfly_princess.png");
    private static final ResourceLocation ANIMATION_RESOURCE = ResourceLocation.fromNamespaceAndPath(
            "entomology_spell", "animations/butterfly_princess.animation.json");

    @Override
    public ResourceLocation getModelResource(AbstractSpellCastingMob animatable)
    {
        return MODEL_RESOURCE;
    }

    @Override
    public ResourceLocation getTextureResource(AbstractSpellCastingMob animatable)
    {
        return TEXTURE_RESOURCE;
    }

    @Override
    public ResourceLocation getAnimationResource(AbstractSpellCastingMob animatable)
    {
        return ANIMATION_RESOURCE;
    }
}
