package io.entomology.entomology.util;

import io.entomology.entomology.EntomologyMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Set;

/**
 * Shared insect detection: entities tagged as swarm creatures or whose
 * registry id contains an insect keyword are treated as insects.
 */
public class SwarmCreatures
{
    public static final Set<String> INSECT_KEYWORDS = Set.of("spider", "bee", "silverfish", "insect", "wasp", "beetle",
            "centipede", "mosquito", "mosco", "tarantula", "fly", "myrmex" , "worm" , "cuttler");
    private static final TagKey<EntityType<?>> SWARM_CREATURES = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "swarm_creatures"));

    public static boolean isSwarmCreature(EntityType<?> type)
    {
        if (type.is(SWARM_CREATURES))
            return true;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id == null)
            return false;
        String path = id.getPath();
        for (String keyword : INSECT_KEYWORDS)
        {
            if (path.contains(keyword))
                return true;
        }
        return false;
    }

    public static boolean isSwarmCreature(Entity entity)
    {
        return isSwarmCreature(entity.getType());
    }
}
