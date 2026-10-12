package io.entomology.entomology.compat;

import io.entomology.entomology.entity.SummonedCentipedeEntity;
import io.entomology.entomology.entity.SummonedCockroach;
import io.entomology.entomology.entity.SummonedMosquitoEntity;
import io.entomology.entomology.entity.SummonedWarpedMoscoEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Isolation barrier for insect-egg hatches that require Alex's Mobs
 * (cave centipede / cockroach / crimson mosquito / warped mosco).
 *
 * <h3>Why this class exists</h3>
 * Every class it references extends an Alex's Mobs entity
 * ({@code EntityCentipedeHead}, {@code EntityCockroach},
 * {@code EntityCrimsonMosquito}, {@code EntityWarpedMosco}). The JVM
 * resolves a referenced class's hierarchy while loading/verifying the
 * referring class, so an unconditionally-registered class (the egg
 * BlockEntity) that directly {@code new}-s these mobs would crash with
 * {@code NoClassDefFoundError} when Alex's Mobs is absent — turning the
 * optional dependency into a hard one.
 *
 * <p>The caller ({@link io.entomology.entomology.block.InsectEggBlockEntity})
 * gates every call to this class behind
 * {@code ModList.get().isLoaded("alexsmobs")}; the VM then never loads or
 * verifies {@code AlexEggHatches} (and never resolves its Alex superclasses)
 * in a no-Alex environment. Do <b>not</b> add non-Alex callers that touch
 * this class unconditionally.
 */
public final class AlexEggHatches
{
    /** Probability that a hatch produces a Summoned Warped Mosco. */
    private static final double WARPED_MOSCO_CHANCE = 0.05D;

    private AlexEggHatches()
    {
    }

    /**
     * Rolls the 5% warped-mosco slice. On a hit, spawns the mosco and
     * returns {@code true} (the caller then skips the regular pool).
     * Stamps the brood markers so guardians recognise it as family.
     */
    public static boolean tryWarpedMosco(ServerLevel level, Vec3 pos,
                                         @Nullable LivingEntity owner,
                                         @Nullable java.util.UUID shiraoriUUID,
                                         @Nullable java.util.UUID ownerUUID,
                                         RandomSource rng)
    {
        if (rng.nextDouble() >= WARPED_MOSCO_CHANCE)
        {
            return false;
        }
        SummonedWarpedMoscoEntity mosco = new SummonedWarpedMoscoEntity(level, owner);
        mosco.moveTo(pos);
        io.entomology.entomology.block.InsectEggBlockEntity.stampBrood(mosco, ownerUUID, shiraoriUUID);
        level.addFreshEntity(mosco);
        return true;
    }

    /**
     * Adds the Alex-only entries (cave centipede / cockroach / crimson
     * mosquito) to the uniformly-weighted regular hatch pool. Each spawned
     * bug is stamped with the brood markers (player + Shiraori UUIDs).
     */
    public static void addRegularEntries(List<BiConsumer<ServerLevel, Vec3>> spawners,
                                         @Nullable LivingEntity owner,
                                         @Nullable java.util.UUID shiraoriUUID,
                                         @Nullable java.util.UUID ownerUUID)
    {
        spawners.add((level, pos) ->
        {
            SummonedCentipedeEntity centipede = new SummonedCentipedeEntity(level, owner);
            centipede.moveTo(pos);
            io.entomology.entomology.block.InsectEggBlockEntity.stampBrood(centipede, ownerUUID, shiraoriUUID);
            level.addFreshEntity(centipede);
        });
        spawners.add((level, pos) ->
        {
            // SummonedCockroach requires a non-null owner (it sets owner +
            // spell level in its ctor). Skip rather than crash on a null owner.
            if (owner == null)
            {
                return;
            }
            SummonedCockroach roach = new SummonedCockroach(level, owner, 1);
            roach.moveTo(pos);
            io.entomology.entomology.block.InsectEggBlockEntity.stampBrood(roach, ownerUUID, shiraoriUUID);
            level.addFreshEntity(roach);
        });
        spawners.add((level, pos) ->
        {
            SummonedMosquitoEntity mosquito = new SummonedMosquitoEntity(level, owner);
            mosquito.moveTo(pos);
            io.entomology.entomology.block.InsectEggBlockEntity.stampBrood(mosquito, ownerUUID, shiraoriUUID);
            level.addFreshEntity(mosquito);
        });
    }
}
