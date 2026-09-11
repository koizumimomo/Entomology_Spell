package io.entomology.entomology.block;

import io.entomology.entomology.entity.SummonedBugBeetleEntity;
import io.entomology.entomology.entity.SummonedCicadaEntity;
import io.entomology.entomology.entity.SummonedCentipedeEntity;
import io.entomology.entomology.entity.SummonedCockroach;
import io.entomology.entomology.entity.SummonedMosquitoEntity;
import io.entomology.entomology.entity.SummonedSilverfishEntity;
import io.entomology.entomology.entity.SummonedWarpedMoscoEntity;
import io.entomology.entomology.registries.BlockRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Block entity for the insect egg block. Counts down a random hatch timer
 * (1200-2400 ticks, 1-2 minutes), then hatches into one of the summoned
 * insects below. All hatched mobs inherit the egg's owner UUID so they are
 * friendly to the caster and their other summons. The timer and owner UUID
 * are persisted to NBT so chunk unloads do not reset the countdown.
 *
 * <h3>Hatch pool</h3>
 * <ul>
 *   <li>5%: Summoned Warped Mosco (only with Alex's Mobs)</li>
 *   <li>The remaining 95% is split uniformly across the rest of the pool:
 *     <ul>
 *       <li>Summoned Cicada</li>
 *       <li>Summoned Bug Beetle</li>
 *       <li>Summoned Silverfish</li>
 *       <li>Summoned Cave Centipede (only with Alex's Mobs)</li>
 *       <li>Summoned Cockroach (only with Alex's Mobs)</li>
 *       <li>Summoned Crimson Mosquito (only with Alex's Mobs)</li>
 *     </ul>
 *   </li>
 * </ul>
 * <p>Without Alex's Mobs only the first three regular entries are in the pool,
 * so each of them gets 1/3 ≈ 33% of the hatch roll (the 5% warped mosco slice
 * is dropped because that mob also requires Alex's Mobs).
 */
public class InsectEggBlockEntity extends BlockEntity
{
    /** Probability that a hatch produces a Summoned Warped Mosco (when Alex's Mobs is loaded). */
    private static final double WARPED_MOSCO_CHANCE = 0.05D;

    private int hatchTimer;
    private UUID ownerUUID;

    public InsectEggBlockEntity(BlockPos pos, BlockState state)
    {
        super(BlockRegistry.INSECT_EGG_BLOCK_ENTITY.get(), pos, state);
        // Random 1200-2400 ticks (1-2 minutes)
        this.hatchTimer = 1200 + (int)(Math.random() * 1200);
    }

    public void setOwner(UUID ownerUUID)
    {
        this.ownerUUID = ownerUUID;
    }

    public UUID getOwnerUUID()
    {
        return this.ownerUUID;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, InsectEggBlockEntity entity)
    {
        if (level.isClientSide)
            return;

        entity.hatchTimer--;
        if (entity.hatchTimer <= 0)
        {
            entity.hatch(level, pos);
        }
    }

    private void hatch(Level level, BlockPos pos)
    {
        if (!(level instanceof ServerLevel serverLevel))
            return;

        LivingEntity owner = null;
        if (ownerUUID != null)
        {
            Entity e = serverLevel.getEntity(ownerUUID);
            if (e instanceof LivingEntity le)
                owner = le;
        }

        Vec3 spawnPos = new Vec3(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        boolean alexLoaded = net.minecraftforge.fml.ModList.get().isLoaded("alexsmobs");
        RandomSource rng = serverLevel.getRandom();

        // 5%: Summoned Warped Mosco (only with Alex's Mobs; falls through to
        // the regular pool when Alex's Mobs is not installed).
        if (alexLoaded && rng.nextDouble() < WARPED_MOSCO_CHANCE)
        {
            spawnWarpedMosco(serverLevel, spawnPos, owner);
        }
        else
        {
            // The regular pool is built dynamically: cicada / beetle / silverfish
            // are always available; centipede / cockroach / crimson mosquito are
            // only added when Alex's Mobs is loaded. Each entry is equally likely.
            List<BiConsumer<ServerLevel, Vec3>> spawners = buildRegularPool(owner, alexLoaded);
            spawners.get(rng.nextInt(spawners.size())).accept(serverLevel, spawnPos);
        }

        // Remove the egg block after hatching
        level.removeBlock(pos, false);
        level.removeBlockEntity(pos);
    }

    /**
     * Builds the uniformly-weighted regular hatch pool. Each entry is a spawn
     * action that already knows the owner; the caller just invokes the chosen
     * one with the server level and spawn position.
     */
    private List<BiConsumer<ServerLevel, Vec3>> buildRegularPool(@Nullable LivingEntity owner, boolean alexLoaded)
    {
        List<BiConsumer<ServerLevel, Vec3>> spawners = new ArrayList<>();
        // Always-available hatches
        spawners.add((sl, p) -> spawnCicada(sl, p, owner));
        spawners.add((sl, p) -> spawnBeetle(sl, p, owner));
        spawners.add((sl, p) -> spawnSilverfish(sl, p, owner));
        // Alex's Mobs hatches
        if (alexLoaded)
        {
            spawners.add((sl, p) -> spawnCentipede(sl, p, owner));
            spawners.add((sl, p) -> spawnCockroach(sl, p, owner));
            spawners.add((sl, p) -> spawnCrimsonMosquito(sl, p, owner));
        }
        return spawners;
    }

    // ---- Spawn helpers ----

    private void spawnCicada(ServerLevel sl, Vec3 p, @Nullable LivingEntity owner)
    {
        SummonedCicadaEntity cicada = new SummonedCicadaEntity(sl, owner);
        cicada.moveTo(p);
        sl.addFreshEntity(cicada);
    }

    private void spawnBeetle(ServerLevel sl, Vec3 p, @Nullable LivingEntity owner)
    {
        SummonedBugBeetleEntity beetle = new SummonedBugBeetleEntity(sl, owner);
        beetle.moveTo(p);
        sl.addFreshEntity(beetle);
    }

    private void spawnSilverfish(ServerLevel sl, Vec3 p, @Nullable LivingEntity owner)
    {
        SummonedSilverfishEntity fish = new SummonedSilverfishEntity(sl);
        fish.moveTo(p);
        // SummonedSilverfishEntity's convenience ctor does not take an owner
        // (the parasite effect wires it), so wire ownership here.
        if (owner != null)
        {
            SummonManager.setOwner(fish, owner);
        }
        sl.addFreshEntity(fish);
    }

    private void spawnCentipede(ServerLevel sl, Vec3 p, @Nullable LivingEntity owner)
    {
        SummonedCentipedeEntity centipede = new SummonedCentipedeEntity(sl, owner);
        centipede.moveTo(p);
        sl.addFreshEntity(centipede);
    }

    private void spawnCockroach(ServerLevel sl, Vec3 p, @Nullable LivingEntity owner)
    {
        // SummonedCockroach requires a non-null owner (it sets owner + spell level
        // in its ctor). If for some reason owner is null, skip the spawn entirely
        // rather than crashing — the egg still hatches and disappears.
        if (owner == null)
            return;
        SummonedCockroach roach = new SummonedCockroach(sl, owner, 1);
        roach.moveTo(p);
        sl.addFreshEntity(roach);
    }

    private void spawnCrimsonMosquito(ServerLevel sl, Vec3 p, @Nullable LivingEntity owner)
    {
        SummonedMosquitoEntity mosquito = new SummonedMosquitoEntity(sl, owner);
        mosquito.moveTo(p);
        sl.addFreshEntity(mosquito);
    }

    private void spawnWarpedMosco(ServerLevel sl, Vec3 p, @Nullable LivingEntity owner)
    {
        SummonedWarpedMoscoEntity mosco = new SummonedWarpedMoscoEntity(sl, owner);
        mosco.moveTo(p);
        sl.addFreshEntity(mosco);
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.putInt("HatchTimer", this.hatchTimer);
        if (this.ownerUUID != null)
            tag.putUUID("OwnerUUID", this.ownerUUID);
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        this.hatchTimer = tag.getInt("HatchTimer");
        if (tag.hasUUID("OwnerUUID"))
            this.ownerUUID = tag.getUUID("OwnerUUID");
    }
}
