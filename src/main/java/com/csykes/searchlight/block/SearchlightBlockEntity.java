package com.csykes.searchlight.block;

import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.util.MutableVector3d;
import com.csykes.searchlight.util.SearchlightUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SearchlightBlockEntity extends BlockEntity {
    private @Nullable BlockPos lightSourcePos;

    public SearchlightBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(Searchlight.SEARCHLIGHT_BE.get(), blockPos, blockState);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveCustomOnly(provider);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (lightSourcePos != null) {
            tag.putInt("light_source_x", lightSourcePos.getX());
            tag.putInt("light_source_y", lightSourcePos.getY());
            tag.putInt("light_source_z", lightSourcePos.getZ());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("light_source_x") && tag.contains("light_source_y") && tag.contains("light_source_z")) {
            lightSourcePos = new BlockPos(tag.getInt("light_source_x"), tag.getInt("light_source_y"), tag.getInt("light_source_z"));
        } else {
            lightSourcePos = null;
        }
    }

    public @Nullable BlockPos getLightSourcePos() {
        return lightSourcePos;
    }

    public @NotNull Vec3 getBeamDirection() {
        if (lightSourcePos == null)
            return SearchlightUtil.directionToBeamVector(SearchlightUtil.getDirection(getBlockState()));
        BlockPos delta = lightSourcePos.subtract(getBlockPos());
        return new Vec3(delta.getX(), delta.getY(), delta.getZ()).normalize();
    }

    public boolean deleteLightSource() {
        BlockPos oldLightSourcePos = lightSourcePos;
        setLightSourcePos(null);
        if (oldLightSourcePos != null && SearchlightUtil.getBlockStateForceLoad(level, oldLightSourcePos).getBlock() instanceof SearchlightLightSourceBlock)
            return SearchlightUtil.setBlockStateForceLoad(level, oldLightSourcePos, Blocks.AIR.defaultBlockState());
        return false;
    }

    public boolean turnOffLightSource() {
        BlockPos lightPos = getLightSourcePos();
        if (lightPos == null)
            return false;
        deleteLightSource();
        if (SearchlightUtil.setBlockStateForceLoad(level, worldPosition, getBlockState().setValue(SearchlightBlock.POWERED, true)))
            return SearchlightUtil.castBlockEntity(level.getBlockEntity(worldPosition), worldPosition, (SearchlightBlockEntity blockEntity) -> blockEntity.setLightSourcePos(lightPos));
        return false;
    }

    public boolean turnOnLightSource() {
        BlockPos lightPos = getLightSourcePos();
        if (lightPos == null)
            return false;
        if (SearchlightUtil.setBlockStateForceLoad(level, worldPosition, getBlockState().setValue(SearchlightBlock.POWERED, false))) {
            if (level.getBlockState(lightPos).isAir())
                return SearchlightUtil.castBlockEntity(level.getBlockEntity(worldPosition), worldPosition, (SearchlightBlockEntity blockEntity) -> blockEntity.placeLightSource(lightPos));
            BlockPos dire = lightPos.subtract(worldPosition);
            return this.raycastAndPlaceLightSource(new Vec3(dire.getX(), dire.getY(), dire.getZ()));
        }
        return false;
    }

    public boolean raycastAndPlaceLightSource(@NotNull Vec3 beamDirection) {
        beamDirection = beamDirection.normalize();
        BlockPos newLightPos = calculateLightSourcePosition(beamDirection);
        return newLightPos != null && placeLightSource(newLightPos);
    }

    public boolean placeLightSource(@Nullable BlockPos newLightPos) {
        deleteLightSource();
        if (newLightPos == null) {
            setLightSourcePos(null);
            return false;
        }
        BlockState oldBlockState = SearchlightUtil.getBlockStateForceLoad(level, newLightPos);
        if (!SearchlightUtil.setBlockStateForceLoad(level, newLightPos, Searchlight.LIGHT_SOURCE_BLOCK.get().defaultBlockState()))
            return false;
        if (!SearchlightUtil.castBlockEntity(level.getBlockEntity(newLightPos), newLightPos, (SearchlightLightSourceBlockEntity lightBlockEntity) -> {
            lightBlockEntity.searchlightBlockPos = getBlockPos();
            setLightSourcePos(newLightPos);
        })) {
            SearchlightUtil.setBlockStateForceLoad(level, newLightPos, oldBlockState);
            setLightSourcePos(null);
            return false;
        }
        return true;
    }

    public @Nullable BlockPos calculateLightSourcePosition(@NotNull Vec3 beamDirection) {
        beamDirection = beamDirection.normalize();
        MutableVector3d currentBlockPosD = new MutableVector3d(getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5);
        BlockPos.MutableBlockPos currentBlockPos = new BlockPos.MutableBlockPos(currentBlockPosD.x, currentBlockPosD.y, currentBlockPosD.z);
        BlockPos.MutableBlockPos prevBlockPos = new BlockPos.MutableBlockPos(0, 0, 0);
        BlockPos lastValidBlockPos = null;

        while (true) {
            prevBlockPos.set(currentBlockPos);
            currentBlockPosD.add(beamDirection);
            currentBlockPos.set(currentBlockPosD.x, currentBlockPosD.y, currentBlockPosD.z);
            if (prevBlockPos.equals(currentBlockPos)) continue;
            if (!level.isInWorldBounds(currentBlockPos)) return null;
            if (!level.isLoaded(currentBlockPos)) return null;

            BlockState currentBlockState = SearchlightUtil.getBlockStateForceLoad(level, currentBlockPos);
            if (!currentBlockState.isAir() && !currentBlockPos.equals(lightSourcePos)) {
                // Simplistic opacity check for now, can be improved to match vanilla realistic opacity if needed
                if (currentBlockState.getLightBlock(level, currentBlockPos) >= level.getMaxLightLevel() || !level.getFluidState(currentBlockPos).isEmpty()) {
                     return SearchlightUtil.moveAwayFromSurfaces(level, lastValidBlockPos);
                }
            }
            if (currentBlockState.isAir() || currentBlockPos.equals(lightSourcePos))
                lastValidBlockPos = currentBlockPos.immutable();
        }
    }

    protected void setLightSourcePos(@Nullable BlockPos lightSourcePos) {
        this.lightSourcePos = lightSourcePos;
        if (level != null && !level.isClientSide) {
             level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
