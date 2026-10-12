package io.entomology.entomology.block;

import io.entomology.entomology.compat.AlexEggHatches;
import io.entomology.entomology.entity.SummonedBugBeetleEntity;
import io.entomology.entomology.entity.SummonedCicadaEntity;
import io.entomology.entomology.entity.SummonedSilverfishEntity;
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
 *
 * <p>All Alex-only spawn actions live in {@link AlexEggHatches}, which is
 * loaded solely when alexsmobs is present; this BlockEntity is registered
 * unconditionally and must never directly reference an Alex-derived class.
 */
public class InsectEggBlockEntity extends BlockEntity
{
    /**
     * Persistent-data keys stamped onto every hatched bug. They record both
     * the casting player and the brood mother Shiraori by UUID, so ally
     * checks (e.g. GuardianSpiderEntity) never depend on resolving entities
     * through SummonManager at decision time.
     */
    public static final String BROOD_OWNER_KEY = "EntomologyBroodOwner";
    public static final String BROOD_SHIRAORI_KEY = "EntomologyBroodShiraori";

    private int hatchTimer;
    private UUID ownerUUID;
    @Nullable
    private UUID shiraoriUUID;

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

    /** Records the Shiraori that laid this egg (the brood mother). */
    public void setShiraori(@Nullable UUID shiraoriUUID)
    {
        this.shiraoriUUID = shiraoriUUID;
    }

    /**
     * Stamps the brood markers onto a freshly hatched bug so guardians can
     * recognise it as family without resolving SummonManager owners.
     */
    public static void stampBrood(Entity bug, @Nullable UUID ownerUUID, @Nullable UUID shiraoriUUID)
    {
        if (ownerUUID != null)
            bug.getPersistentData().putUUID(BROOD_OWNER_KEY, ownerUUID);
        if (shiraoriUUID != null)
            bug.getPersistentData().putUUID(BROOD_SHIRAORI_KEY, shiraoriUUID);
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
        // the regular pool when Alex's Mobs is not installed). The call into
        // AlexEggHatches is short-circuited by alexLoaded so its class is
        // never resolved without Alex's Mobs.
        if (alexLoaded && AlexEggHatches.tryWarpedMosco(serverLevel, spawnPos, owner, this.shiraoriUUID, this.ownerUUID, rng))
        {
            // Hatched into the rare slice; nothing else to do.
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
        // Alex's Mobs hatches — the call is gated by alexLoaded, so without
        // Alex's Mobs the AlexEggHatches class is never loaded.
        if (alexLoaded)
        {
            AlexEggHatches.addRegularEntries(spawners, owner, this.shiraoriUUID, this.ownerUUID);
        }
        return spawners;
    }

    // ---- Spawn helpers (Alex-free pool only) ----

    private void spawnCicada(ServerLevel sl, Vec3 p, @Nullable LivingEntity owner)
    {
        SummonedCicadaEntity cicada = new SummonedCicadaEntity(sl, owner);
        cicada.moveTo(p);
        stampBrood(cicada, this.ownerUUID, this.shiraoriUUID);
        sl.addFreshEntity(cicada);
    }

    private void spawnBeetle(ServerLevel sl, Vec3 p, @Nullable LivingEntity owner)
    {
        SummonedBugBeetleEntity beetle = new SummonedBugBeetleEntity(sl, owner);
        beetle.moveTo(p);
        stampBrood(beetle, this.ownerUUID, this.shiraoriUUID);
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
        stampBrood(fish, this.ownerUUID, this.shiraoriUUID);
        sl.addFreshEntity(fish);
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.putInt("HatchTimer", this.hatchTimer);
        if (this.ownerUUID != null)
            tag.putUUID("OwnerUUID", this.ownerUUID);
        if (this.shiraoriUUID != null)
            tag.putUUID("ShiraoriUUID", this.shiraoriUUID);
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        this.hatchTimer = tag.getInt("HatchTimer");
        if (tag.hasUUID("OwnerUUID"))
            this.ownerUUID = tag.getUUID("OwnerUUID");
        if (tag.hasUUID("ShiraoriUUID"))
            this.shiraoriUUID = tag.getUUID("ShiraoriUUID");
    }
}
