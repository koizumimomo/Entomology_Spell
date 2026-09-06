package io.entomology.entomology.entity;

import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;

/**
 * Shared logic for insect summons that should follow the player across worlds
 * (like Iron's Spellbooks' own summons do) but immediately despawn upon being
 * restored into the new world, mirroring vanilla Iron's Spellbooks behaviour.
 *
 * {@code IMagicSummon}s opt in by delegating their save/load here:
 * <ul>
 *   <li>{@link #writeRestoreFlag(Mob, CompoundTag)} stamps a flag during
 *       {@code saveSummonerData}'s {@code entity.save()} call (logout).</li>
 *   <li>{@link #readRestoreFlag(Mob, CompoundTag)} reads it back during the
 *       login restore and records it in the entity's Forge persistent data.</li>
 *   <li>{@link #handleRestoredDespawn(Mob)} runs from the summon's server
 *       {@code tick()} and self-despawns once when the flag is present.</li>
 * </ul>
 * Entities that override {@code shouldBeSaved()} to return false (transient
 * bees such as Requiem / Alarm) never enter this path.
 */
public final class SummonedRestorationHelper
{
    /** NBT key stamped on save so the restored copy knows it was carried over. */
    public static final String RESTORED_KEY = "EntomologySummonRestored";

    private SummonedRestorationHelper() {}

    /**
     * Called from a summon's {@code addAdditionalSaveData}. Stamps the restore
     * flag so the copy rebuilt in the new world can detect it was carried over
     * rather than freshly spawned.
     */
    public static void writeRestoreFlag(Mob summon, CompoundTag tag)
    {
        tag.putBoolean(RESTORED_KEY, true);
    }

    /**
     * Called from a summon's {@code readAdditionalSaveData}. If the flag is
     * present the entity was carried over from another world; record it in the
     * entity's Forge persistent data so the first server tick can react.
     */
    public static void readRestoreFlag(Mob summon, CompoundTag tag)
    {
        if (tag.getBoolean(RESTORED_KEY))
        {
            summon.getPersistentData().putBoolean(RESTORED_KEY, true);
        }
    }

    /**
     * Called from a summon's {@code tick()} (server side). Performs the
     * deferred despawn once, then clears the flag so a normal first-spawn tick
     * (which never sets the flag) is unaffected.
     */
    public static void handleRestoredDespawn(Mob summon)
    {
        if (summon.getPersistentData().getBoolean(RESTORED_KEY))
        {
            summon.getPersistentData().remove(RESTORED_KEY);
            if (!summon.level().isClientSide && summon instanceof IMagicSummon magicSummon)
            {
                magicSummon.onUnSummon();
            }
        }
    }
}
