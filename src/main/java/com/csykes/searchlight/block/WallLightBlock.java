package com.csykes.searchlight.block;

import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.util.SearchlightUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class WallLightBlock extends FaceAttachedHorizontalDirectionalBlock {
    protected static final VoxelShape CEILING_X_SHAPE = Block.box(6, 14, 5, 10, 16, 11);
    protected static final VoxelShape CEILING_Z_SHAPE = Block.box(5, 14, 6, 11, 16, 10);
    protected static final VoxelShape FLOOR_X_SHAPE = Block.box(6, 0, 5, 10, 2, 11);
    protected static final VoxelShape FLOOR_Z_SHAPE = Block.box(5, 0, 6, 11, 2, 10);
    protected static final VoxelShape NORTH_SHAPE = Block.box(5, 8, 14, 11, 12, 16);
    protected static final VoxelShape SOUTH_SHAPE = Block.box(5, 8, 0, 11, 12, 2);
    protected static final VoxelShape WEST_SHAPE = Block.box(14, 8, 5, 16, 12, 11);
    protected static final VoxelShape EAST_SHAPE = Block.box(0, 8, 5, 2, 12, 11);

    public WallLightBlock(@NotNull Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FACE, AttachFace.WALL));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FACE);
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return com.mojang.serialization.MapCodec.unit(() -> (WallLightBlock) Searchlight.WALL_LIGHTS.values().stream().findFirst().get().get());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        Direction direction = SearchlightUtil.getDirection(state);
        if (direction == Direction.UP)
            return state.getValue(FACING).getAxis() == Direction.Axis.X ? FLOOR_X_SHAPE : FLOOR_Z_SHAPE;
        else if (direction == Direction.DOWN)
            return state.getValue(FACING).getAxis() == Direction.Axis.X ? CEILING_X_SHAPE : CEILING_Z_SHAPE;
        else if (direction == Direction.EAST)
            return EAST_SHAPE;
        else if (direction == Direction.WEST)
            return WEST_SHAPE;
        else if (direction == Direction.SOUTH)
            return SOUTH_SHAPE;
        return NORTH_SHAPE;
    }
}
