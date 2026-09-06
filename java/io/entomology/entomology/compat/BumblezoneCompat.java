package io.entomology.entomology.compat;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * Soft dependency integration with The Bumblezone. Nothing here references any
 * Bumblezone classes; effects are looked up from the registry at runtime.
 */
public class BumblezoneCompat
{
    private static final boolean LOADED = ModList.get().isLoaded("the_bumblezone");

    // Entities tagged with this are immune to paralysis (tag defined by The Bumblezone)
    private static final TagKey<EntityType<?>> PARALYZED_IMMUNE = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("the_bumblezone", "paralyzed/immune"));

    public static boolean isLoaded()
    {
        return LOADED;
    }

    @Nullable
    public static MobEffect getParalyzedEffect()
    {
        if (!LOADED)
            return null;
        return ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.fromNamespaceAndPath("the_bumblezone", "paralyzed"));
    }

    /**
     * Whether the given entity can be paralyzed at all.
     * Undead, iron golems and snow golems resist paralysis, as does anything
     * tagged with the_bumblezone:paralyzed/immune.
     */
    public static boolean canParalyze(LivingEntity victim)
    {
        if (getParalyzedEffect() == null)
            return false;
        if (victim.getMobType() == MobType.UNDEAD)
            return false;
        if (victim instanceof IronGolem || victim instanceof SnowGolem)
            return false;
        return !victim.getType().is(PARALYZED_IMMUNE);
    }
}
