package io.entomology.entomology.util;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.registries.SchoolRegistry;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;

/**
 * Shared helpers for identifying damage dealt by the swarm (insect) school.
 */
public class InsectDamage
{
    private static final ResourceLocation INSECT_MAGIC = ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "insect_magic");

    /**
     * True for insect school spells and for the swarm school damage type
     * (summoned bee stings, toxin bursts...).
     */
    public static boolean isInsectSchoolDamage(DamageSource source)
    {
        if (source instanceof SpellDamageSource spellSource)
        {
            return spellSource.spell().getSchoolType() == SchoolRegistry.INSECT.get();
        }
        return source.typeHolder().is(INSECT_MAGIC);
    }
}
