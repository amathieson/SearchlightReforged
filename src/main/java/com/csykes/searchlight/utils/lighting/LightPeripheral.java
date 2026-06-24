package com.csykes.searchlight.utils.lighting;

import com.csykes.searchlight.utils.lighting.BrightnessStage;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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

    @LuaFunction(mainThread = true)
    public final void setBrightness(int level) {
        Level world = tile.getLevel();
        BlockPos pos = tile.getBlockPos();
        if (world == null) return;

        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof AbstractLightBlock block) {
            BrightnessStage stage = BrightnessStage.fromId(Math.max(0, Math.min(4, level)));
            world.setBlock(pos, state.setValue(AbstractLightBlock.BRIGHTNESS, stage), 3);
            world.updateNeighborsAt(pos, block);
        }
    }

    @LuaFunction(mainThread = true)
    public final int getBrightness() {
        BlockState state = tile.getBlockState();
        if (state.getBlock() instanceof AbstractLightBlock) {
            return state.getValue(AbstractLightBlock.BRIGHTNESS).getId();
        }
        return 0;
    }

    @LuaFunction(mainThread = true)
    public final void setLit(boolean lit) {
        Level world = tile.getLevel();
        BlockPos pos = tile.getBlockPos();
        if (world == null) return;

        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof AbstractLightBlock block) {
            world.setBlock(pos, state.setValue(AbstractLightBlock.LIT, lit), 3);
            world.updateNeighborsAt(pos, block);
        }
    }

    @LuaFunction(mainThread = true)
    public final boolean isLit() {
        BlockState state = tile.getBlockState();
        if (state.getBlock() instanceof AbstractLightBlock) {
            return state.getValue(AbstractLightBlock.LIT);
        }
        return false;
    }
}