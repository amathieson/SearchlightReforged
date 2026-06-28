package com.csykes.searchlight.features.corner_light;

import com.csykes.searchlight.features.wall_light.WallLightBlockEntity;
import com.csykes.searchlight.utils.lighting.AbstractDirectionalLightBlock;
import com.csykes.searchlight.utils.lighting.BrightnessStage;
import com.csykes.searchlight.utils.lighting.CornerLightStage;
import com.csykes.searchlight.utils.lighting.LightRodConnection;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class CornerLightBlock extends AbstractDirectionalLightBlock implements EntityBlock {
    private final DyeColor blockColor;

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new WallLightBlockEntity(pos, state);
    }

    public CornerLightBlock(Properties properties, DyeColor blockColor) {
        super(properties);
        this.blockColor = blockColor;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(LIT, true)
                .setValue(BRIGHTNESS, BrightnessStage.MEDIUM)
                .setValue(CONNECTION, LightRodConnection.SINGLE)
                .setValue(CORNER, CornerLightStage.BOTTOM_LEFT));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState baseState = super.getStateForPlacement(context);
        if (baseState == null) return null;

        Vec3 hit = context.getClickLocation();
        BlockPos pos = context.getClickedPos();

        CornerLightStage corner = getCorner(hit, pos);
        return baseState.setValue(CORNER, corner).setValue(CONNECTION, this.getConnectionState(context.getLevel(), pos, corner));
    }

    private static @NotNull CornerLightStage getCorner(Vec3 hit, BlockPos pos) {
        double localX = hit.x - pos.getX();
        double localZ = hit.z - pos.getZ();

        CornerLightStage corner;

        if (localX < 0.5) {
            if (localZ < 0.5) {
                corner = CornerLightStage.BOTTOM_RIGHT;
            } else {
                corner = CornerLightStage.BOTTOM_LEFT;
            }
        } else {
            if (localZ < 0.5) {
                corner = CornerLightStage.TOP_RIGHT;
            } else {
                corner = CornerLightStage.TOP_LEFT;
            }
        }
        return corner;
    }

    @Override
    public @NotNull BlockState updateShape(BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState, @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighborPos) {
        return state.setValue(CONNECTION, getConnectionState(level, pos, state.getValue(CORNER)));
    }

    private LightRodConnection getConnectionState(LevelAccessor level, BlockPos pos, CornerLightStage corner) {
        boolean hasAbove = isMatchingConnection(level, pos.relative(Direction.UP), corner);
        boolean hasBelow = isMatchingConnection(level, pos.relative(Direction.DOWN), corner);

        if (hasAbove && hasBelow) return LightRodConnection.MIDDLE;
        if (hasAbove) return LightRodConnection.BOTTOM;
        if (hasBelow) return LightRodConnection.TOP;
        return LightRodConnection.SINGLE;
    }

    private boolean isMatchingConnection(LevelAccessor level, BlockPos target, CornerLightStage corner) {
        BlockState state = level.getBlockState(target);
        return state.getBlock() instanceof CornerLightBlock && state.getValue(CORNER) == corner;
    }

    public static final com.mojang.serialization.MapCodec<CornerLightBlock> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(instance -> instance.group(propertiesCodec(), net.minecraft.world.item.DyeColor.CODEC.fieldOf("color").forGetter(CornerLightBlock::getBlockColor)).apply(instance, CornerLightBlock::new));

    @Override
    protected com.mojang.serialization.@NotNull MapCodec<? extends net.minecraft.world.level.block.Block> codec() {
        return CODEC;
    }

    private static final VoxelShape SHAPE_BL = Block.box(0, 0, 14, 2, 16, 16);

    private static final VoxelShape SHAPE_BR = Block.box(0, 0, 0, 2, 16, 2);

    private static final VoxelShape SHAPE_TR = Block.box(14, 0, 0, 16, 16, 2);

    private static final VoxelShape SHAPE_TL = Block.box(14, 0, 14, 16, 16, 16);


    @Override
    public @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return switch (state.getValue(CORNER)) {
            case BOTTOM_LEFT -> SHAPE_BL;
            case BOTTOM_RIGHT -> SHAPE_BR;
            case TOP_RIGHT -> SHAPE_TR;
            case TOP_LEFT -> SHAPE_TL;
        };
    }

}