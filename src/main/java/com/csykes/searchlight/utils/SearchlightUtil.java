package com.csykes.searchlight.utils;

import com.csykes.searchlight.Searchlight;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public final class SearchlightUtil {
    public static <T extends BlockEntity> boolean castBlockEntity(@Nullable BlockEntity blockEntity, @NotNull BlockPos blockPos, @NotNull Consumer<T> result) {
        if (blockEntity == null) {
            return false;
        }
        if (!blockEntity.hasLevel()) {
            return false;
        }
        try {
            @SuppressWarnings("unchecked")
            T casted = (T) blockEntity;
            result.accept(casted);
            return true;
        } catch (ClassCastException ex) {
            Searchlight.LOGGER.error("Attempted to cast '{}' ({}) at {} but failed", blockEntity, blockEntity.getClass(), blockPos, ex);
            return false;
        }
    }

    public static @NotNull BlockState getBlockStateForceLoad(@NotNull Level world, @NotNull BlockPos blockPos) {
        return world.getBlockState(blockPos);
    }

    public static @NotNull BlockState getBlockStateIfLoaded(Level world, BlockPos blockPos) {
        if (!world.isInWorldBounds(blockPos))
            return Blocks.VOID_AIR.defaultBlockState();
        if (!world.isLoaded(blockPos))
            return Blocks.VOID_AIR.defaultBlockState();
        return world.getBlockState(blockPos);
    }

    public static boolean setBlockStateForceLoad(Level world, BlockPos blockPos, BlockState blockState) {
        return world.setBlock(blockPos, blockState, 3);
    }

    public static Direction getDirection(BlockState state) {
        AttachFace face = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE);
        if (face == AttachFace.CEILING)
            return Direction.DOWN;
        else if (face == AttachFace.FLOOR)
            return Direction.UP;
        return state.getValue(FaceAttachedHorizontalDirectionalBlock.FACING);
    }

    public static @NotNull Vec3 directionToBeamVector(@NotNull Direction direction) {
        return Vec3.atLowerCornerOf(direction.getNormal());
    }

    public static BlockPos moveAwayFromSurfaces(Level world, BlockPos blockPos) {
        if (blockPos == null)
            return null;
        BlockPos resultPos = blockPos.immutable();

        if (!world.getBlockState(resultPos.relative(Direction.WEST)).isAir() && world.getBlockState(resultPos.relative(Direction.EAST)).isAir())
            resultPos = resultPos.relative(Direction.EAST);
        else if (!world.getBlockState(resultPos.relative(Direction.EAST)).isAir() && world.getBlockState(resultPos.relative(Direction.WEST)).isAir())
            resultPos = resultPos.relative(Direction.WEST);

        if (!world.getBlockState(resultPos.relative(Direction.DOWN)).isAir() && world.getBlockState(resultPos.relative(Direction.UP)).isAir())
            resultPos = resultPos.relative(Direction.UP);
        else if (!world.getBlockState(resultPos.relative(Direction.UP)).isAir() && world.getBlockState(resultPos.relative(Direction.DOWN)).isAir())
            resultPos = resultPos.relative(Direction.DOWN);

        if (!world.getBlockState(resultPos.relative(Direction.NORTH)).isAir() && world.getBlockState(resultPos.relative(Direction.SOUTH)).isAir())
            resultPos = resultPos.relative(Direction.SOUTH);
        else if (!world.getBlockState(resultPos.relative(Direction.SOUTH)).isAir() && world.getBlockState(resultPos.relative(Direction.NORTH)).isAir())
            resultPos = resultPos.relative(Direction.NORTH);

        return resultPos;
    }

    /**
     * Doesn't show beams if the player isn't holding a searchlight
     * @return
     */
    public static boolean displayBeams() {
        Player player = Minecraft.getInstance().player;
        return player != null && player.isHolding(Searchlight.SEARCHLIGHT_ITEM.get());
    }
}
