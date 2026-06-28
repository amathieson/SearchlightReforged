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
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractDirectionalLightBlock extends Block {
    public static final EnumProperty<BrightnessStage> BRIGHTNESS = EnumProperty.create("brightness", BrightnessStage.class);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final EnumProperty<LightRodConnection> CONNECTION = EnumProperty.create("connection", LightRodConnection.class);
    public static final EnumProperty<CornerLightStage> CORNER = EnumProperty.create("corner", CornerLightStage.class);

    public AbstractDirectionalLightBlock(@NotNull Properties properties) {
        super(properties);
        // Set the default facing direction, e.g., North, unlit, with standard brightness
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(CONNECTION, LightRodConnection.SINGLE)
                .setValue(CORNER, CornerLightStage.BOTTOM_LEFT)
                .setValue(LIT, Boolean.TRUE)
                .setValue(BRIGHTNESS, BrightnessStage.MEDIUM)); // Adjust the default stage as needed
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, BRIGHTNESS);
        builder.add(CONNECTION, CORNER);
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

    protected void updateLitState(Level world, BlockPos pos, BlockState state) {
        if (world.isClientSide) return;
        boolean isPoweredNow = world.hasNeighborSignal(pos);
        boolean wasLitBefore = state.getValue(LIT);

        if (state.getValue(CONNECTION) == LightRodConnection.BOTTOM || state.getValue(CONNECTION) == LightRodConnection.MIDDLE) {
            int distance = 1;
            BlockState target = world.getBlockState(pos.relative(Direction.UP, distance));
            while (target.getBlock() instanceof AbstractDirectionalLightBlock) {
                isPoweredNow |= world.hasNeighborSignal(pos.relative(Direction.UP, distance));
                distance++;
                target = world.getBlockState(pos.relative(Direction.UP, distance));
            }
        }

        if (state.getValue(CONNECTION) == LightRodConnection.TOP || state.getValue(CONNECTION) == LightRodConnection.MIDDLE) {
            int distance = 1;
            BlockState target = world.getBlockState(pos.relative(Direction.DOWN, distance));
            while (target.getBlock() instanceof AbstractDirectionalLightBlock) {
                isPoweredNow |= world.hasNeighborSignal(pos.relative(Direction.DOWN, distance));
                distance++;
                target = world.getBlockState(pos.relative(Direction.DOWN, distance));
            }
        }
        boolean shouldBeLit = !isPoweredNow;

        if (wasLitBefore != shouldBeLit) {
            world.setBlock(pos, state.setValue(LIT, shouldBeLit), 3);
            world.updateNeighborsAt(pos, this);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(LIT, !context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BrightnessStage brightness = state.getValue(BRIGHTNESS);
        if (stack.is(Items.GLOWSTONE_DUST)) {
            if (brightness != BrightnessStage.ULTRA) {
                if (!world.isClientSide) {
                    BrightnessStage next = brightness.next();
                    world.setBlock(pos, state.setValue(BRIGHTNESS, next), 3);
                    world.updateNeighborsAt(pos, this);
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    world.playSound(null, pos, SoundEvents.GLOW_ITEM_FRAME_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
                    if (next == BrightnessStage.ULTRA) {
                        player.displayClientMessage(Component.literal("This light has reached max brightness."), true);
                    }
                }
                return ItemInteractionResult.sidedSuccess(world.isClientSide);
            }
        } else if (stack.is(Items.CHARCOAL) || stack.is(Items.COAL) || stack.is(Items.REDSTONE)) {
            if (brightness != BrightnessStage.OFF) {
                if (!world.isClientSide) {
                    BrightnessStage next = brightness.previous();
                    world.setBlock(pos, state.setValue(BRIGHTNESS, next), 3);
                    world.updateNeighborsAt(pos, this);
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    world.playSound(null, pos, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
                    if (next == BrightnessStage.OFF) {
                        player.displayClientMessage(Component.literal("This light has reached the lowest brightness"), true);
                    }
                }
                return ItemInteractionResult.sidedSuccess(world.isClientSide);
            }
        }
        return super.useItemOn(stack, state, world, pos, player, hand, hit);
    }
}