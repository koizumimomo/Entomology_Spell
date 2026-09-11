package io.entomology.entomology.entity.goal;

import io.entomology.entomology.entity.GuardianSpiderEntity;
import io.entomology.entomology.entity.ShiraoriEntity;
import io.entomology.entomology.entity.SpiderNestEntity;
import io.entomology.entomology.block.InsectEggBlockEntity;
import io.entomology.entomology.registries.BlockRegistry;
import io.entomology.entomology.registries.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * Brood-keeping goal for Shiraori the Spider Mother. This goal always runs
 * (independent of combat) and drives her three nest-building behaviours, in
 * the fixed priority order requested by the design:
 * <ol>
 *   <li><b>Startup nests</b> — once, shortly after being summoned, she spawns
 *       two {@link SpiderNestEntity} guard nests on solid ground around her.
 *       The "already done" flag is persisted on Shiraori herself
 *       ({@link ShiraoriEntity#setBroodNestsSummoned(boolean)}) so chunk
 *       reloads do not grant duplicate nests.</li>
 *   <li><b>Egg laying</b> — every 5-8 seconds she tries to place one insect
 *       egg block at her feet level (including one block below) within a
 *       3-block radius, capped at {@link #MAX_NEARBY_EGGS} nearby eggs.
 *       Hatched bugs inherit her summoner's owner UUID, not hers, so they
 *       stay friendly to the player after she despawns.</li>
 *   <li><b>Guardian summons</b> — every 30 seconds she summons a
     *       {@link GuardianSpiderEntity} near her, or casts the Summon Ice
     *       Spider spell (50/50 chance, no combat target required).</li>
 * </ol>
 *
 * <p>No {@link Goal.Flag} is claimed, so Shiraori keeps following her owner
 * ({@code GenericFollowOwnerGoal}) while the brood timers tick in the
 * background.
 */
public class ShiraoriBroodGoal extends Goal
{
    /** Short grace period after she appears before the nests materialise. */
    private static final int INITIAL_NEST_DELAY = 30;
    private static final int NEST_COUNT = 2;
    private static final int NEST_SEARCH_RADIUS = 3;

    private static final int EGG_INTERVAL_MIN = 100; // 5 seconds
    private static final int EGG_INTERVAL_MAX = 160; // 8 seconds
    private static final int EGG_RADIUS = 3;
    private static final int MAX_NEARBY_EGGS = 4;

    private static final int GUARDIAN_INTERVAL = 600; // 30 seconds
    private static final int FIRST_GUARDIAN_DELAY = 200; // first guard spider ~10s in

    private final ShiraoriEntity mob;
    private final int spellLevel;
    private int nestDelay;
    private int eggTimer;
    private int guardianTimer;

    public ShiraoriBroodGoal(ShiraoriEntity mob)
    {
        this(mob, 1);
    }

    public ShiraoriBroodGoal(ShiraoriEntity mob, int spellLevel)
    {
        this.mob = mob;
        this.spellLevel = Math.max(1, spellLevel);
        this.nestDelay = INITIAL_NEST_DELAY;
        this.eggTimer = EGG_INTERVAL_MIN;
        this.guardianTimer = FIRST_GUARDIAN_DELAY;
    }

    @Override
    public boolean canUse()
    {
        // Brood-keeping happens on the server side regardless of combat.
        return !this.mob.level().isClientSide;
    }

    @Override
    public boolean canContinueToUse()
    {
        return true;
    }

    @Override
    public void tick()
    {
        if (!(this.mob.level() instanceof ServerLevel serverLevel))
        {
            return;
        }

        // 1) Priority: summon the two startup nests once, shortly after spawn.
        if (!this.mob.isBroodNestsSummoned())
        {
            if (--this.nestDelay <= 0)
            {
                summonStartupNests(serverLevel);
                this.mob.setBroodNestsSummoned(true);
            }
            return;
        }

        // 2) Lay an insect egg every 5-8 seconds.
        if (--this.eggTimer <= 0)
        {
            this.eggTimer = EGG_INTERVAL_MIN + this.mob.getRandom().nextInt(EGG_INTERVAL_MAX - EGG_INTERVAL_MIN);
            tryLayEgg(serverLevel);
        }

        // 3) Summon a guardian spider every 30 seconds.
        if (--this.guardianTimer <= 0)
        {
            this.guardianTimer = GUARDIAN_INTERVAL;
            summonGuardianSpider(serverLevel);
        }
    }

    /**
     * Spawns {@link #NEST_COUNT} nests on the heightmap surface around
     * Shiraori. Nests are owned by Shiraori herself, so their spiders share
     * her ownership chain and the nests collapse automatically once she
     * despawns (the nest's tick discards it when its owner is gone).
     */
    private void summonStartupNests(ServerLevel serverLevel)
    {
        BlockPos center = this.mob.blockPosition();
        int spawned = 0;
        for (int attempt = 0; attempt < 12 && spawned < NEST_COUNT; attempt++)
        {
            int dx = this.mob.getRandom().nextIntBetweenInclusive(-NEST_SEARCH_RADIUS, NEST_SEARCH_RADIUS);
            int dz = this.mob.getRandom().nextIntBetweenInclusive(-NEST_SEARCH_RADIUS, NEST_SEARCH_RADIUS);
            if (dx == 0 && dz == 0)
            {
                continue;
            }
            int x = center.getX() + dx;
            int z = center.getZ() + dz;
            int y = serverLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos base = new BlockPos(x, y, z);
            // Need room for the 1-tall nest model.
            if (!serverLevel.isEmptyBlock(base) || !serverLevel.isEmptyBlock(base.above()))
            {
                continue;
            }
            // Don't drop a nest inside liquids.
            if (!serverLevel.getFluidState(base).isEmpty())
            {
                continue;
            }

            SpiderNestEntity nest = new SpiderNestEntity(serverLevel,
                    new Vec3(base.getX() + 0.5D, base.getY(), base.getZ() + 0.5D), this.mob);
            serverLevel.addFreshEntity(nest);
            nest.summonSpiders(); // first wave immediately
            spawned++;
        }

        if (spawned > 0)
        {
            serverLevel.playSound(null, center, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 0.8F, 0.7F);
        }
    }

    /**
     * Attempts to place one insect egg on solid ground below/near Shiraori.
     * The block entity stores her summoner's UUID, so hatched summons are
     * friendly to the player and survive Shiraori's despawn.
     */
    private void tryLayEgg(ServerLevel serverLevel)
    {
        if (countNearbyEggs(serverLevel) >= MAX_NEARBY_EGGS)
        {
            return;
        }

        BlockPos target = findEggPosition(serverLevel);
        if (target == null)
        {
            return;
        }

        // Randomly pick one of the two egg models (normal / purple)
        io.entomology.entomology.block.InsectEggBlock.Variant variant = serverLevel.random.nextBoolean()
                ? io.entomology.entomology.block.InsectEggBlock.Variant.NORMAL
                : io.entomology.entomology.block.InsectEggBlock.Variant.PURPLE;
        serverLevel.setBlock(target, BlockRegistry.INSECT_EGG.get().defaultBlockState()
                .setValue(io.entomology.entomology.block.InsectEggBlock.VARIANT, variant), Block.UPDATE_ALL);
        if (serverLevel.getBlockEntity(target) instanceof InsectEggBlockEntity egg)
        {
            LivingEntity owner = (LivingEntity) SummonManager.getOwner(this.mob);
            egg.setOwner((owner != null ? owner : this.mob).getUUID());
            egg.setChanged();
        }
        serverLevel.playSound(null, target, SoundEvents.SPIDER_STEP, SoundSource.HOSTILE, 0.6F, 0.8F);
    }

    private int countNearbyEggs(ServerLevel serverLevel)
    {
        AABB box = this.mob.getBoundingBox().inflate(6.0D);
        int minX = net.minecraft.util.Mth.floor(box.minX);
        int maxX = net.minecraft.util.Mth.floor(box.maxX);
        int minY = net.minecraft.util.Mth.floor(box.minY);
        int maxY = net.minecraft.util.Mth.floor(box.maxY);
        int minZ = net.minecraft.util.Mth.floor(box.minZ);
        int maxZ = net.minecraft.util.Mth.floor(box.maxZ);

        int count = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++)
        {
            for (int y = minY; y <= maxY; y++)
            {
                for (int z = minZ; z <= maxZ; z++)
                {
                    cursor.set(x, y, z);
                    if (serverLevel.getBlockState(cursor).is(BlockRegistry.INSECT_EGG.get()))
                    {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Searches at feet level, one block below and one block above within a
     * {@link #EGG_RADIUS} cube for an air/replaceable spot sitting on a block
     * with a non-empty collision shape.
     */
    private BlockPos findEggPosition(ServerLevel serverLevel)
    {
        BlockPos origin = this.mob.blockPosition();
        CollisionContext collision = CollisionContext.of(this.mob);

        for (int attempt = 0; attempt < 10; attempt++)
        {
            int dx = this.mob.getRandom().nextIntBetweenInclusive(-EGG_RADIUS, EGG_RADIUS);
            int dy = this.mob.getRandom().nextIntBetweenInclusive(-1, 1);
            int dz = this.mob.getRandom().nextIntBetweenInclusive(-EGG_RADIUS, EGG_RADIUS);
            BlockPos candidate = origin.offset(dx, dy, dz);

            BlockState state = serverLevel.getBlockState(candidate);
            if (!state.isAir() && !state.canBeReplaced())
            {
                continue;
            }
            if (!state.getFluidState().isEmpty())
            {
                continue;
            }
            BlockState below = serverLevel.getBlockState(candidate.below());
            if (below.getCollisionShape(serverLevel, candidate.below(), collision).isEmpty())
            {
                continue;
            }
            if (serverLevel.getBlockEntity(candidate) != null)
            {
                continue;
            }
            return candidate;
        }
        return null;
    }

    /**
     * Spawns a guardian spider near Shiraori. Half the time this is delegated
     * to the Summon Ice Spider spell (which does not require a combat target);
     * otherwise a {@link GuardianSpiderEntity} is spawned directly and owned by
     * Shiraori's summoner.
     */
    private void summonGuardianSpider(ServerLevel serverLevel)
    {
        // 50% chance: cast Summon Ice Spider spell for protection, even when
        // not in combat. Skipped if Shiraori is already mid-cast (e.g. web
        // entangle) to avoid interrupting her combat spell.
        if (!this.mob.isCasting() && this.mob.getRandom().nextBoolean())
        {
            AbstractSpell iceSpiderSpell = SpellRegistry.SUMMON_ICE_SPIDER_SPELL.get();
            if (iceSpiderSpell != null)
            {
                this.mob.initiateCastSpell(iceSpiderSpell, this.spellLevel);
                return;
            }
        }

        GuardianSpiderEntity guardian = new GuardianSpiderEntity(serverLevel, this.mob);
        double dx = (this.mob.getRandom().nextDouble() - 0.5) * 3.0D;
        double dz = (this.mob.getRandom().nextDouble() - 0.5) * 3.0D;
        guardian.moveTo(this.mob.getX() + dx, this.mob.getY(), this.mob.getZ() + dz,
                this.mob.getRandom().nextFloat() * 360.0F, 0.0F);
        serverLevel.addFreshEntity(guardian);
        LivingEntity owner = (LivingEntity) SummonManager.getOwner(this.mob);
        SummonManager.setOwner(guardian, owner != null ? owner : this.mob);
    }
}
