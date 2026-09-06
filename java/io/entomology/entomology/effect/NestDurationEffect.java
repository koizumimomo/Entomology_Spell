package io.entomology.entomology.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Purely a timer buff shown on the spider nest. The nest kills itself
 * once this effect expires (checked every tick in SpiderNestEntity).
 */
public class NestDurationEffect extends MobEffect
{
    public NestDurationEffect()
    {
        super(MobEffectCategory.BENEFICIAL, 0x8B5E3C);
    }
}
