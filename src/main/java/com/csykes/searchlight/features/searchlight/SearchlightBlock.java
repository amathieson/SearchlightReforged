package com.csykes.searchlight.features.searchlight;

import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.utils.SearchlightUtil;
import com.csykes.searchlight.utils.lighting.AbstractLightBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.csykes.searchlight.utils.lighting.BrightnessStage;

public class SearchlightBlock extends AbstractLightBlock implements EntityBlock {
    public SearchlightBlock(@NotNull Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FACE, AttachFace.WALL)
                .setValue(LIT, true)
                .setValue(BRIGHTNESS, BrightnessStage.MEDIUM));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SearchlightBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!world.isClientSide) {
                SearchlightUtil.castBlockEntity(world.getBlockEntity(pos), pos, (SearchlightBlockEntity be) -> be.deleteLightSource());
            }
            super.onRemove(state, world, pos, newState, isMoving);
        } else if (state.getValue(LIT) != newState.getValue(LIT)) {
            if (!world.isClientSide) {
                SearchlightUtil.castBlockEntity(world.getBlockEntity(pos), pos, (SearchlightBlockEntity be) -> {
                    if (newState.getValue(LIT)) {
                        be.turnOnLightSource();
                    } else {
                        be.turnOffLightSource();
                    }
                });
            }
        }
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
        if (!world.isClientSide) {
            SearchlightUtil.castBlockEntity(world.getBlockEntity(pos), pos, (SearchlightBlockEntity be) -> {
                if (be.getLightSourcePos() == null) {
                    updateSearchLight(world, pos, state, placer);
                }
            });
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemInteractionResult result = super.useItemOn(stack, state, world, pos, player, hand, hit);
        if (result.consumesAction()) return result;

        if (!world.isClientSide) {
            updateSearchLight(world, pos, state, player);
            world.playSound(null, pos, SoundEvents.NETHERITE_BLOCK_PLACE, SoundSource.BLOCKS, 1, 0.4f);
        }
        return ItemInteractionResult.sidedSuccess(world.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide) {
            updateSearchLight(world, pos, state, player);
            world.playSound(null, pos, SoundEvents.NETHERITE_BLOCK_PLACE, SoundSource.BLOCKS, 1, 0.4f);
        }
        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    protected void updateSearchLight(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer) {
        SearchlightUtil.castBlockEntity(world.getBlockEntity(pos), pos, (SearchlightBlockEntity blockEntity) -> {
            Vec3 direction;
            if (placer != null) {
                direction = placer.getLookAngle().scale(-1);
            } else {
                direction = SearchlightUtil.directionToBeamVector(SearchlightUtil.getDirection(state));
            }
            blockEntity.raycastAndPlaceLightSource(direction);
        });
    }

    public static final com.mojang.serialization.MapCodec<SearchlightBlock> CODEC = simpleCodec(SearchlightBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}