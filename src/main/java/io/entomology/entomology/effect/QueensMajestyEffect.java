package io.entomology.entomology.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Queen's Majesty — the visible aura carried by whoever wears the True Queen
 * Crown. The actual stat buffs are radiated onto nearby summons by
 * {@link io.entomology.entomology.item.TrueQueenCrownArmorItem}'s inventory tick;
 * this effect exists so the wearer shows a proper buff icon (it does nothing on
 * its own besides rendering in the effect HUD).
 */
public class QueensMajestyEffect extends MobEffect
{
    public QueensMajestyEffect()
    {
        super(MobEffectCategory.BENEFICIAL, 0xe8b84b);
    }
}
