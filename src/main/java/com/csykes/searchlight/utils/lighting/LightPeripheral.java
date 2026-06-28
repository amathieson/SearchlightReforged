package com.csykes.searchlight.utils.lighting;

import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.features.wall_light.WallLightBlock;
import com.csykes.searchlight.features.corner_light.CornerLightBlock;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LightPeripheral implements IPeripheral {
    private final BlockEntity tile;

    public LightPeripheral(BlockEntity tile) {
        this.tile = tile;
    }

    @NotNull
    @Override
    public String getType() {
        return "searchlight";
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return this == other || (other instanceof LightPeripheral o && o.tile == tile);
    }

    private java.util.List<BlockPos> getConnectedCornerLights(Level world, BlockPos startPos, BlockState startState) {
        java.util.List<BlockPos> positions = new java.util.ArrayList<>();
        if (!(startState.getBlock() instanceof CornerLightBlock)) {
            positions.add(startPos);
            return positions;
        }

        CornerLightStage targetCorner = startState.getValue(CornerLightBlock.CORNER);
        positions.add(startPos);

        // Traverse UP
        BlockPos current = startPos.above();
        while (true) {
            BlockState state = world.getBlockState(current);
            if (state.getBlock() instanceof CornerLightBlock && state.getValue(CornerLightBlock.CORNER) == targetCorner) {
                positions.add(current);
                current = current.above();
            } else {
                break;
            }
        }

        // Traverse DOWN
        current = startPos.below();
        while (true) {
            BlockState state = world.getBlockState(current);
            if (state.getBlock() instanceof CornerLightBlock && state.getValue(CornerLightBlock.CORNER) == targetCorner) {
                positions.add(current);
                current = current.below();
            } else {
                break;
            }
        }

        return positions;
    }

    private BlockState setBrightnessProperty(BlockState state, BrightnessStage stage) {
        if (state.hasProperty(AbstractLightBlock.BRIGHTNESS)) {
            return state.setValue(AbstractLightBlock.BRIGHTNESS, stage);
        } else if (state.hasProperty(AbstractDirectionalLightBlock.BRIGHTNESS)) {
            return state.setValue(AbstractDirectionalLightBlock.BRIGHTNESS, stage);
        }
        return state;
    }

    private BrightnessStage getBrightnessProperty(BlockState state) {
        if (state.hasProperty(AbstractLightBlock.BRIGHTNESS)) {
            return state.getValue(AbstractLightBlock.BRIGHTNESS);
        } else if (state.hasProperty(AbstractDirectionalLightBlock.BRIGHTNESS)) {
            return state.getValue(AbstractDirectionalLightBlock.BRIGHTNESS);
        }
        return BrightnessStage.OFF;
    }

    private BlockState setLitProperty(BlockState state, boolean lit) {
        if (state.hasProperty(AbstractLightBlock.LIT)) {
            return state.setValue(AbstractLightBlock.LIT, lit);
        } else if (state.hasProperty(AbstractDirectionalLightBlock.LIT)) {
            return state.setValue(AbstractDirectionalLightBlock.LIT, lit);
        }
        return state;
    }

    private boolean getLitProperty(BlockState state) {
        if (state.hasProperty(AbstractLightBlock.LIT)) {
            return state.getValue(AbstractLightBlock.LIT);
        } else if (state.hasProperty(AbstractDirectionalLightBlock.LIT)) {
            return state.getValue(AbstractDirectionalLightBlock.LIT);
        }
        return false;
    }

    @LuaFunction(mainThread = true)
    public final void setBrightness(int level) {
        Level world = tile.getLevel();
        BlockPos pos = tile.getBlockPos();
        if (world == null) return;

        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        BrightnessStage stage = BrightnessStage.fromId(Math.max(0, Math.min(4, level)));

        if (block instanceof CornerLightBlock) {
            for (BlockPos connectedPos : getConnectedCornerLights(world, pos, state)) {
                BlockState s = world.getBlockState(connectedPos);
                BlockState updatedState = setBrightnessProperty(s, stage);
                world.setBlock(connectedPos, updatedState, 3);
                world.updateNeighborsAt(connectedPos, s.getBlock());
            }
        } else if (block instanceof AbstractLightBlock || block instanceof AbstractDirectionalLightBlock) {
            BlockState updatedState = setBrightnessProperty(state, stage);
            world.setBlock(pos, updatedState, 3);
            world.updateNeighborsAt(pos, block);
        }
    }

    @LuaFunction(mainThread = true)
    public final int getBrightness() {
        BlockState state = tile.getBlockState();
        Block block = state.getBlock();
        if (block instanceof AbstractLightBlock || block instanceof AbstractDirectionalLightBlock) {
            return getBrightnessProperty(state).getId();
        }
        return 0;
    }

    @LuaFunction(mainThread = true)
    public final void setLit(boolean lit) {
        Level world = tile.getLevel();
        BlockPos pos = tile.getBlockPos();
        if (world == null) return;

        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if (block instanceof CornerLightBlock) {
            for (BlockPos connectedPos : getConnectedCornerLights(world, pos, state)) {
                BlockState s = world.getBlockState(connectedPos);
                BlockState updatedState = setLitProperty(s, lit);
                world.setBlock(connectedPos, updatedState, 3);
                world.updateNeighborsAt(connectedPos, s.getBlock());
            }
        } else if (block instanceof AbstractLightBlock || block instanceof AbstractDirectionalLightBlock) {
            BlockState updatedState = setLitProperty(state, lit);
            world.setBlock(pos, updatedState, 3);
            world.updateNeighborsAt(pos, block);
        }
    }

    @LuaFunction(mainThread = true)
    public final boolean isLit() {
        BlockState state = tile.getBlockState();
        Block block = state.getBlock();
        if (block instanceof AbstractLightBlock || block instanceof AbstractDirectionalLightBlock) {
            return getLitProperty(state);
        }
        return false;
    }

    @LuaFunction(mainThread = true)
    public final boolean setColor(String colorName) {
        Level world = tile.getLevel();
        BlockPos pos = tile.getBlockPos();
        if (world == null || world.isClientSide) return false;

        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        String normalizedColor = colorName.toLowerCase();

        // 1. Handle Wall Lights
        if (block instanceof WallLightBlock) {
            DeferredBlock<Block> newBlockHolder = Searchlight.WALL_LIGHTS.get(normalizedColor);
            if (newBlockHolder != null) {
                Block newBlock = newBlockHolder.get();
                BlockState newState = newBlock.defaultBlockState();
                
                // Copy over all matching block properties
                if (state.hasProperty(WallLightBlock.FACING)) newState = newState.setValue(WallLightBlock.FACING, state.getValue(WallLightBlock.FACING));
                if (state.hasProperty(WallLightBlock.FACE)) newState = newState.setValue(WallLightBlock.FACE, state.getValue(WallLightBlock.FACE));
                if (state.hasProperty(WallLightBlock.LIT)) newState = newState.setValue(WallLightBlock.LIT, state.getValue(WallLightBlock.LIT));
                if (state.hasProperty(WallLightBlock.BRIGHTNESS)) newState = newState.setValue(WallLightBlock.BRIGHTNESS, state.getValue(WallLightBlock.BRIGHTNESS));

                world.setBlock(pos, newState, 3);
                world.updateNeighborsAt(pos, newBlock);
                return true;
            }
        }

        // 2. Handle Corner Lights
        if (block instanceof CornerLightBlock) {
            DeferredBlock<Block> newBlockHolder = Searchlight.CORNER_LIGHTS.get(normalizedColor);
            if (newBlockHolder != null) {
                Block newBlock = newBlockHolder.get();
                java.util.List<BlockPos> connected = getConnectedCornerLights(world, pos, state);
                for (BlockPos connectedPos : connected) {
                    BlockState s = world.getBlockState(connectedPos);
                    BlockState newState = newBlock.defaultBlockState();
                    
                    // Copy over all matching block properties
                    if (s.hasProperty(CornerLightBlock.CORNER)) newState = newState.setValue(CornerLightBlock.CORNER, s.getValue(CornerLightBlock.CORNER));
                    if (s.hasProperty(CornerLightBlock.CONNECTION)) newState = newState.setValue(CornerLightBlock.CONNECTION, s.getValue(CornerLightBlock.CONNECTION));
                    if (s.hasProperty(CornerLightBlock.LIT)) newState = newState.setValue(CornerLightBlock.LIT, s.getValue(CornerLightBlock.LIT));
                    if (s.hasProperty(CornerLightBlock.BRIGHTNESS)) newState = newState.setValue(CornerLightBlock.BRIGHTNESS, s.getValue(CornerLightBlock.BRIGHTNESS));

                    world.setBlock(connectedPos, newState, 3);
                    world.updateNeighborsAt(connectedPos, newBlock);
                }
                return true;
            }
        }

        return false;
    }

    @LuaFunction(mainThread = true)
    public final String getColor() {
        BlockState state = tile.getBlockState();
        Block block = state.getBlock();

        if (block instanceof CornerLightBlock cornerBlock) {
            return cornerBlock.getBlockColor().getName();
        }

        for (java.util.Map.Entry<String, DeferredBlock<Block>> entry : Searchlight.WALL_LIGHTS.entrySet()) {
            if (entry.getValue().get() == block) {
                return entry.getKey();
            }
        }

        return "unknown";
    }
}