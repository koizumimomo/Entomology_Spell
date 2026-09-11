package io.entomology.entomology.util;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.registries.EffectRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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

    /**
     * Returns true if the entity is a spider branded as (or currently
     * undergoing the ritual of) Shiraori's Attendant. Such spiders are
     * treated as allies by every swarm summon regardless of ownership,
     * because they are destined to drop the Summon Shiraori scroll.
     */
    public static boolean isShiraoriAttendant(Entity entity)
    {
        if (entity == null)
            return false;
        if (entity.getPersistentData().getBoolean("ShiraoriAttendant"))
            return true;
        if (entity.getPersistentData().getBoolean("ShiraoriAttendantPending"))
            return true;
        return entity instanceof LivingEntity le
                && le.hasEffect(EffectRegistry.SHIRAORI_ATTENDANT.get());
    }

    /**
     * Returns true if two entities belong to the same summon ownership chain
     * (i.e. they share a common ancestor in SummonManager, or one owns the
     * other). Handles transitive ownership: nest spiders → Shiraori → player.
     * Up to 5 hops are checked to prevent infinite loops.
     * <p>
     * Shiraori's Attendant spiders are always treated as same-chain (allies
     * of every swarm summon) even though they have no SummonManager owner.
     */
    public static boolean isSameOwnerChain(Entity a, Entity b)
    {
        if (a == b)
            return true;
        // Attendant spiders are allies of all swarm summons
        if (isShiraoriAttendant(a) || isShiraoriAttendant(b))
            return true;
        // Collect all ancestors of a (including itself) by UUID
        Set<UUID> chainA = new HashSet<>();
        Entity current = a;
        for (int i = 0; i < 5 && current != null; i++)
        {
            chainA.add(current.getUUID());
            Entity owner = SummonManager.getOwner(current);
            if (owner == null || owner == current)
                break;
            current = owner;
        }
        // Walk b's chain and check for overlap
        current = b;
        for (int i = 0; i < 5 && current != null; i++)
        {
            if (chainA.contains(current.getUUID()))
                return true;
            Entity owner = SummonManager.getOwner(current);
            if (owner == null || owner == current)
                break;
            current = owner;
        }
        return false;
    }
}
