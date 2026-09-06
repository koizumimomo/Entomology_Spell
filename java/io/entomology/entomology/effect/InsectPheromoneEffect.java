package io.entomology.entomology.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Marks affected entities with the scent of the swarm.
 * While this effect is active, the entity is treated as an arthropod
 * (see {@link io.entomology.entomology.mixin.LivingEntityMixin}) and thus
 * takes bonus damage from Bane of Arthropods.
 */
public class InsectPheromoneEffect extends MobEffect
{
    public InsectPheromoneEffect()
    {
        super(MobEffectCategory.NEUTRAL, 0x6b8e23);
    }
}
