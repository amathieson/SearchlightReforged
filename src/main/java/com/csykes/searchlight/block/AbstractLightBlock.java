package com.csykes.searchlight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

    public AbstractLightBlock(@NotNull Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FACE, LIT, BRIGHTNESS);
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        boolean isPoweredNow = world.hasNeighborSignal(pos);
        boolean wasPoweredBefore = state.getValue(LIT);
        if (wasPoweredBefore != isPoweredNow) {
            BlockState newState = state.setValue(LIT, isPoweredNow);
            world.setBlock(pos, newState, 2);
            onPowerChanged(world, pos, newState, isPoweredNow);
            world.updateNeighborsAt(pos, this);
        }
    }

    protected abstract void onPowerChanged(Level world, BlockPos pos, BlockState state, boolean isPowered);

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BrightnessStage brightness = state.getValue(BRIGHTNESS);
        if (stack.is(Items.GLOWSTONE_DUST)) {
            if (brightness != BrightnessStage.ULTRA) {
                if (!world.isClientSide) {
                    BrightnessStage next = brightness.next();
                    world.setBlock(pos, state.setValue(BRIGHTNESS, next), 2);
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
        } else if (stack.is(Items.REDSTONE)) {
            if (brightness != BrightnessStage.OFF) {
                if (!world.isClientSide) {
                    BrightnessStage next = brightness.previous();
                    world.setBlock(pos, state.setValue(BRIGHTNESS, next), 2);
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
