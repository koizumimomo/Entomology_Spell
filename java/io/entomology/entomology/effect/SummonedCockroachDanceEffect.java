package io.entomology.entomology.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Status indicator shown on the caster while the cockroach dance troupe is active.
 * Its duration mirrors the summon's lifetime so the remaining time is visible on the HUD.
 */
public class SummonedCockroachDanceEffect extends MobEffect
{
    public SummonedCockroachDanceEffect()
    {
        super(MobEffectCategory.BENEFICIAL, 0x8a5a2b);
    }
}
