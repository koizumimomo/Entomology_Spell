package io.entomology.entomology.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Shared helper for "stackable" debuffs (Parasitic Breath's Slowness, Chaotic
 * Stinger's Poison/Wither): every application raises the amplifier by one
 * (capped) and refreshes the duration.
 */
public final class StackingEffects
{
    private StackingEffects()
    {
    }

    /**
     * Amplifier for the next application: existing level + 1, capped at
     * {@code maxStacks} (never below the spell's own base level, never a
     * downgrade of a stronger potion the victim already carries).
     */
    public static int nextAmplifier(LivingEntity victim, MobEffect effect, int baseAmplifier, int maxStacks)
    {
        MobEffectInstance existing = victim.getEffect(effect);
        if (existing == null)
        {
            return baseAmplifier;
        }
        int cap = Math.max(baseAmplifier, maxStacks);
        return Math.max(existing.getAmplifier(), Math.min(existing.getAmplifier() + 1, cap));
    }

    /**
     * Forces the debuff to (re)apply at the given level, refreshing its
     * duration. Vanilla addEffect alone would keep a longer old duration.
     */
    public static void applyForced(LivingEntity victim, MobEffect effect, int duration, int amplifier)
    {
        victim.removeEffect(effect);
        victim.addEffect(new MobEffectInstance(effect, duration, amplifier, false, true, true));
    }
}
