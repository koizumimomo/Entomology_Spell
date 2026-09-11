package io.entomology.entomology.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class InsectEggBlock extends net.minecraft.world.level.block.Block implements EntityBlock
{
    public enum Variant implements StringRepresentable
    {
        NORMAL("normal"),
        PURPLE("purple");

        private final String name;

        Variant(String name)
        {
            this.name = name;
        }

        @Override
        public String getSerializedName()
        {
            return this.name;
        }
    }

    public static final EnumProperty<Variant> VARIANT = EnumProperty.create("variant", Variant.class);

    // Fits the egg model (main body ~4.7-11.2 px wide, ~10 px tall)
    private static final VoxelShape SHAPE = net.minecraft.world.level.block.Block.box(4.0D, 0.0D, 4.0D, 12.0D, 10.0D, 12.0D);

    public InsectEggBlock()
    {
        super(BlockBehaviour.Properties.of()
                .strength(0.3F)
                .noOcclusion()
                .randomTicks());
        this.registerDefaultState(this.stateDefinition.any().setValue(VARIANT, Variant.NORMAL));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder)
    {
        builder.add(VARIANT);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new InsectEggBlockEntity(pos, state);
    }

    // Drives the per-tick hatch countdown on the server. Without this the
    // BlockEntity's static tick() is never invoked, so the egg would never hatch.
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        return level.isClientSide ? null
                : (serverLevel, pos, blockState, blockEntity) ->
                        InsectEggBlockEntity.tick(serverLevel, pos, blockState, (InsectEggBlockEntity) blockEntity);
    }

    // Player can break it in survival - block just disappears, no hatching
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.player.Player player)
    {
        super.playerWillDestroy(level, pos, state, player);
    }
}