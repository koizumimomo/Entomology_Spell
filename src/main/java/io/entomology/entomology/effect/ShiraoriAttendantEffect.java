package io.entomology.entomology.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Shiraori's Attendant ritual marker. Applied to a spider for 30 seconds by
 * using Shiraori's Fang on it. While active, nearby non-insect hostile mobs
 * are taunted into attacking the spider (handled in
 * {@code ShiraoriAttendantHandler}, which scans every tick); when the effect
 * ends the spider is permanently branded with the attendant NBT and a name
 * prefix, and killing it drops a level 1 Summon Shiraori scroll.
 *
 * <p>The effect itself carries no attribute or tick logic — it is only a
 * marker / timer, mirroring {@link SwarmExemptionEffect}.
 */
public class ShiraoriAttendantEffect extends MobEffect
{
    public ShiraoriAttendantEffect()
    {
        super(MobEffectCategory.HARMFUL, 0x9D6CD8);
    }
}
