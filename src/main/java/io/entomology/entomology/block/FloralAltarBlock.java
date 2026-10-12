package io.entomology.entomology.block;

import io.entomology.entomology.entity.ButterflyEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Floral Altar ("Flower of Emergence") — a hive-like butterfly sanctuary
 * block. Sapling-style properties (walk-through, insta-mineable by hand,
 * breaks when its support is removed) and spawns wild butterflies around
 * itself on random ticks. It is obtained by ripening it out of grass blocks
 * with bone meal ({@code FloralAltarBonemealHandler}).
 */
public class FloralAltarBlock extends Block
{
    /** Low, flat selection shape that hugs the flower-petal model. */
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 6.0D, 15.0D);

    /** Horizontal radius around the altar in which butterflies may appear. */
    private static final int SPAWN_RADIUS = 3;

    /** Wild butterflies within this range count towards {@link #MAX_NEARBY_BUTTERFLIES}. */
    private static final double NEARBY_SCAN_RANGE = 16.0D;

    /** Random ticks stop adding butterflies beyond this wild count nearby. */
    private static final int MAX_NEARBY_BUTTERFLIES = 3;

    public FloralAltarBlock()
    {
        super(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                .mapColor(net.minecraft.world.level.material.MapColor.PLANT)
                .noCollission()
                .instabreak()
                .randomTicks()
                .sound(net.minecraft.world.level.block.SoundType.GRASS)
                .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return SHAPE;
    }

    // ---- Sapling-style support rules ----

    @Override
    @SuppressWarnings("deprecation")
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
    {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    @SuppressWarnings("deprecation")
    public net.minecraft.world.level.block.state.BlockState updateShape(BlockState state, Direction facing,
            BlockState facingState, net.minecraft.world.level.LevelAccessor level, BlockPos currentPos, BlockPos facingPos)
    {
        if (facing == Direction.DOWN && !state.canSurvive(level, currentPos))
        {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    // ---- Butterfly spawning ----

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
    {
        if (countNearbyWildButterflies(level, pos) >= MAX_NEARBY_BUTTERFLIES)
        {
            return;
        }
        BlockPos spawnPos = findSpawnPosition(level, pos, random, SPAWN_RADIUS);
        if (spawnPos != null)
        {
            spawnButterfly(level, spawnPos);
        }
    }

    /**
     * Picks a random spot right around {@code center} (same level, up to two
     * blocks above/below): requires two blocks of air over a sturdy floor and
     * no liquid.
     */
    @Nullable
    public static BlockPos findSpawnPosition(ServerLevel level, BlockPos center, RandomSource random, int radius)
    {
        for (int attempt = 0; attempt < 6; attempt++)
        {
            int dx = random.nextIntBetweenInclusive(-radius, radius);
            int dz = random.nextIntBetweenInclusive(-radius, radius);
            int dy = random.nextIntBetweenInclusive(-1, 2);
            BlockPos candidate = center.offset(dx, dy, dz);
            if (!level.getFluidState(candidate).isEmpty()
                    || !level.getFluidState(candidate.below()).isEmpty())
            {
                continue;
            }
            if (!level.getBlockState(candidate).isAir() || !level.getBlockState(candidate.above()).isAir())
            {
                continue;
            }
            BlockPos floor = candidate.below();
            if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP))
            {
                continue;
            }
            return candidate;
        }
        return null;
    }

    public static void spawnButterfly(ServerLevel level, BlockPos pos)
    {
        ButterflyEntity butterfly = new ButterflyEntity(level, null, true);
        butterfly.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        level.addFreshEntity(butterfly);
        level.playSound(null, pos, SoundEvents.BEEHIVE_EXIT, SoundSource.BLOCKS, 0.6F, 1.4F);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                8, 0.4D, 0.4D, 0.4D, 0.0D);
    }

    private static int countNearbyWildButterflies(ServerLevel level, BlockPos pos)
    {
        List<ButterflyEntity> butterflies = level.getEntitiesOfClass(ButterflyEntity.class,
                new AABB(pos).inflate(NEARBY_SCAN_RANGE, NEARBY_SCAN_RANGE * 0.5D, NEARBY_SCAN_RANGE),
                ButterflyEntity::isWild);
        return butterflies.size();
    }
}
