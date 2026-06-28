package com.csykes.searchlight.utils.lighting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractLightBlock extends FaceAttachedHorizontalDirectionalBlock {
    public static final EnumProperty<BrightnessStage> BRIGHTNESS = EnumProperty.create("brightness", BrightnessStage.class);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final EnumProperty<LightRequest> LIGHT_REQUEST = EnumProperty.create("light_request", LightRequest.class);
    public static final EnumProperty<LightRodConnection> CONNECTION = EnumProperty.create("connection", LightRodConnection.class);
    public static final EnumProperty<CornerLightStage> CORNER = EnumProperty.create("corner", CornerLightStage.class);

    protected AbstractLightBlock(@NotNull Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FACE, LIT, BRIGHTNESS, LIGHT_REQUEST);
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        updateLitState(world, pos, state);
    }

    @Override
    protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!oldState.is(state.getBlock())) {
            updateLitState(world, pos, state);
        }
    }

    public void updateLitState(Level world, BlockPos pos, BlockState state) {
        if (world.isClientSide) return;
        boolean isPoweredNow = world.hasNeighborSignal(pos);
        boolean wasLitBefore = state.getValue(LIT);
        LightRequest requested = state.getValue(LIGHT_REQUEST);

        if (state.hasProperty(CONNECTION)) {
            if (state.getValue(CONNECTION) == LightRodConnection.BOTTOM || state.getValue(CONNECTION) == LightRodConnection.MIDDLE) {
                int distance = 1;
                BlockState target = world.getBlockState(pos.relative(Direction.UP, distance));
                while (target.getBlock() instanceof AbstractLightBlock) {
                    isPoweredNow |= world.hasNeighborSignal(pos.relative(Direction.UP, distance));
                    if (target.getValue(LIGHT_REQUEST) != LightRequest.RELEASE)
                    {
                        requested = target.getValue(LIGHT_REQUEST);
                    }
                    target = world.getBlockState(pos.relative(Direction.UP, distance));
                    distance++;
                }
            }

            if (state.getValue(CONNECTION) == LightRodConnection.TOP || state.getValue(CONNECTION) == LightRodConnection.MIDDLE) {
                int distance = 1;
                BlockState target = world.getBlockState(pos.relative(Direction.DOWN, distance));
                while (target.getBlock() instanceof AbstractLightBlock) {
                    isPoweredNow |= world.hasNeighborSignal(pos.relative(Direction.DOWN, distance));
                    distance++;
                    target = world.getBlockState(pos.relative(Direction.DOWN, distance));
                }
            }
        }
        boolean shouldBeLit = !isPoweredNow;
        if (requested != LightRequest.RELEASE) {
            shouldBeLit = requested == LightRequest.ON;
        }

        if (wasLitBefore != shouldBeLit) {
            world.setBlockAndUpdate(pos, state.setValue(LIT, shouldBeLit));
            world.updateNeighborsAt(pos, this);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state != null) {
            return state.setValue(LIT, !context.getLevel().hasNeighborSignal(context.getClickedPos()));
        }
        return null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BrightnessStage brightness = state.getValue(BRIGHTNESS);
        BrightnessStage next = brightness;
        boolean success = false;
        if (world.isClientSide)
            return super.useItemOn(stack, state, world, pos, player, hand, hit);
        if (stack.is(Items.GLOWSTONE_DUST) && brightness != BrightnessStage.ULTRA) {
            next = brightness.next();
            world.playSound(null, pos, SoundEvents.GLOW_ITEM_FRAME_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
            if (next == BrightnessStage.ULTRA) {
                player.displayClientMessage(Component.translatable("searchlight.message.highest_brightness"), true);
            }
            success = true;
        } else if (stack.is(Items.REDSTONE) && brightness != BrightnessStage.OFF) {
            next = brightness.previous();
            world.playSound(null, pos, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
            if (next == BrightnessStage.OFF) {
                player.displayClientMessage(Component.translatable("searchlight.message.lowest_brightness"), true);
            }
            success = true;
        }

        if (success) {
            world.setBlockAndUpdate(pos, state.setValue(BRIGHTNESS, next));
            world.updateNeighborsAt(pos, this);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return ItemInteractionResult.sidedSuccess(false);
        }

        return super.useItemOn(stack, state, world, pos, player, hand, hit);
    }
}