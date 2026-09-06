package io.entomology.entomology.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Status indicator shown on the player while Bee Alarm posts are active. Kept
 * separate from Summoned Bee Swarm so the two bee spells never touch each
 * other's state (the swarm spell counts only its own summons).
 */
public class SummonedAlarmBeeEffect extends MobEffect
{
    public SummonedAlarmBeeEffect()
    {
        super(MobEffectCategory.BENEFICIAL, 0xff8c1a);
    }
}
