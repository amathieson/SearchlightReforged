package com.csykes.searchlight.block;

import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.util.SearchlightUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SearchlightBlock extends FaceAttachedHorizontalDirectionalBlock implements EntityBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    protected static final VoxelShape CEILING_SHAPE = Block.box(3, 3, 3, 13, 16, 13);
    protected static final VoxelShape FLOOR_SHAPE = Block.box(3, 0, 3, 13, 13, 13);
    protected static final VoxelShape NORTH_SHAPE = Block.box(3, 3, 3, 13, 13, 16);
    protected static final VoxelShape SOUTH_SHAPE = Block.box(3, 3, 0, 13, 13, 13);
    protected static final VoxelShape WEST_SHAPE = Block.box(3, 3, 3, 16, 13, 13);
    protected static final VoxelShape EAST_SHAPE = Block.box(0, 3, 3, 13, 13, 13);

    public SearchlightBlock(@NotNull Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FACE, AttachFace.WALL)
                .setValue(POWERED, false));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SearchlightBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FACE, POWERED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        Direction direction = SearchlightUtil.getDirection(state);
        if (direction == Direction.UP)
            return FLOOR_SHAPE;
        else if (direction == Direction.DOWN)
            return CEILING_SHAPE;
        else if (direction == Direction.EAST)
            return EAST_SHAPE;
        else if (direction == Direction.WEST)
            return WEST_SHAPE;
        else if (direction == Direction.SOUTH)
            return SOUTH_SHAPE;
        return NORTH_SHAPE;
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        boolean isPoweredNow = world.hasNeighborSignal(pos);
        boolean wasPoweredBefore = state.getValue(POWERED);
        if (!wasPoweredBefore && isPoweredNow) {
            SearchlightUtil.castBlockEntity(world.getBlockEntity(pos), pos, (SearchlightBlockEntity be) -> be.turnOffLightSource());
        } else if (wasPoweredBefore && !isPoweredNow) {
            SearchlightUtil.castBlockEntity(world.getBlockEntity(pos), pos, (SearchlightBlockEntity be) -> be.turnOnLightSource());
        }
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
        if (!world.isClientSide) {
            if (!updateSearchLight(world, pos, state, placer)) {
                updateSearchLight(world, pos, state, null);
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide && updateSearchLight(world, pos, state, player)) {
            world.playSound(null, pos, SoundEvents.NETHERITE_BLOCK_PLACE, SoundSource.BLOCKS, 1, 0.4f);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!world.isClientSide) {
            SearchlightUtil.castBlockEntity(world.getBlockEntity(pos), pos, (SearchlightBlockEntity be) -> be.deleteLightSource());
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }

    protected boolean updateSearchLight(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer) {
        final boolean[] result = new boolean[1];
        SearchlightUtil.castBlockEntity(world.getBlockEntity(pos), pos, (SearchlightBlockEntity blockEntity) ->
                result[0] = blockEntity.raycastAndPlaceLightSource(placer != null
                        ? placer.getLookAngle().scale(-1)
                        : SearchlightUtil.directionToBeamVector(SearchlightUtil.getDirection(state))));
        return result[0];
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return com.mojang.serialization.MapCodec.unit(() -> (SearchlightBlock)Searchlight.SEARCHLIGHT_BLOCK.get());
    }
}
