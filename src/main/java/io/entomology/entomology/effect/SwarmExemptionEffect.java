package io.entomology.entomology.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Marker applied by the Swarm Exemption spell: the marked entity loses the
 * Weaver Spider Chelicerae protection (Insect Kinship), so insects may pick it
 * as an attack target even in a summoner-vs-summoner standoff. The bypasses
 * themselves live in InsectKinshipEffect (targeting) and
 * SummonFriendlyFireHandler (damage), which both check for this marker.
 */
public class SwarmExemptionEffect extends MobEffect
{
    public SwarmExemptionEffect()
    {
        super(MobEffectCategory.HARMFUL, 0xff9130);
    }
}
