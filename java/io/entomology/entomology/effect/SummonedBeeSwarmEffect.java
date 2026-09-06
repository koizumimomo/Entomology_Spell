package io.entomology.entomology.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Status indicator shown on the player while a summoned bee swarm is active.
 */
public class SummonedBeeSwarmEffect extends MobEffect
{
    public SummonedBeeSwarmEffect()
    {
        super(MobEffectCategory.BENEFICIAL, 0xf5c518);
    }
}
