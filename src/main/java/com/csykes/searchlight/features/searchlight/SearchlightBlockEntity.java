package com.csykes.searchlight.features.searchlight;

import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.utils.SearchlightUtil;
import com.csykes.searchlight.utils.lighting.AbstractLightBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.csykes.searchlight.MutableVector3d;

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
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        saveAdditional(tag, provider);
        return tag;
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
        if (level == null || level.isClientSide) return false;
        BlockPos oldLightSourcePos = lightSourcePos;
        this.lightSourcePos = null;
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        if (oldLightSourcePos != null && level.getBlockState(oldLightSourcePos).getBlock() instanceof SearchlightLightSourceBlock)
            return level.setBlock(oldLightSourcePos, Blocks.AIR.defaultBlockState(), 3);
        return false;
    }

    public boolean turnOffLightSource() {
        BlockPos lightPos = getLightSourcePos();
        if (lightPos == null)
            return false;

        if (level != null && !level.isClientSide && level.getBlockState(lightPos).getBlock() instanceof SearchlightLightSourceBlock) {
            level.setBlock(lightPos, Blocks.AIR.defaultBlockState(), 3);
        }

        setChanged();
        return true;
    }

    public boolean turnOnLightSource() {
        if (lightSourcePos != null) {
            BlockState currentState = level.getBlockState(lightSourcePos);
            if (currentState.isAir() || currentState.getBlock() instanceof SearchlightLightSourceBlock) {
                return placeLightSource(lightSourcePos);
            }
            return this.raycastAndPlaceLightSource(getBeamDirection());
        }

        return this.raycastAndPlaceLightSource(SearchlightUtil.directionToBeamVector(SearchlightUtil.getDirection(getBlockState())));
    }

    public boolean raycastAndPlaceLightSource(@NotNull Vec3 beamDirection) {
        if (beamDirection.equals(Vec3.ZERO)) return false;
        beamDirection = beamDirection.normalize();
        BlockPos newLightPos = calculateLightSourcePosition(beamDirection);

        if (getBlockState().getValue(AbstractLightBlock.LIT)) {
            return placeLightSource(newLightPos);
        }

        setLightSourcePos(newLightPos);
        return true;
    }

    public boolean placeLightSource(@Nullable BlockPos newLightPos) {
        if (newLightPos == null) {
            deleteLightSource();
            return false;
        }

        if (level == null || level.isClientSide) return false;

        // If there's an existing light source somewhere else, delete it
        if (lightSourcePos != null && !lightSourcePos.equals(newLightPos)) {
            deleteLightSource();
        }

        BlockState oldBlockState = level.getBlockState(newLightPos);
        if (!level.setBlock(newLightPos, Searchlight.LIGHT_SOURCE_BLOCK.get().defaultBlockState(), 3))
            return false;

        if (!SearchlightUtil.castBlockEntity(level.getBlockEntity(newLightPos), newLightPos, (SearchlightLightSourceBlockEntity lightBlockEntity) -> {
            lightBlockEntity.searchlightBlockPos = getBlockPos();
            setLightSourcePos(newLightPos);
        })) {
            level.setBlock(newLightPos, oldBlockState, 3);
            // If it failed to place, and it wasn't already there, clear it
            if (lightSourcePos != null && lightSourcePos.equals(newLightPos)) {
                setLightSourcePos(null);
            }
            return false;
        }
        return true;
    }

    public @Nullable BlockPos calculateLightSourcePosition(@NotNull Vec3 beamDirection) {
        if (beamDirection.equals(Vec3.ZERO)) return null;
        beamDirection = beamDirection.normalize();
        MutableVector3d currentBlockPosD = new MutableVector3d(getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5);
        BlockPos.MutableBlockPos currentBlockPos = new BlockPos.MutableBlockPos(currentBlockPosD.x, currentBlockPosD.y, currentBlockPosD.z);
        BlockPos.MutableBlockPos prevBlockPos = new BlockPos.MutableBlockPos(0, 0, 0);
        BlockPos lastValidBlockPos = null;
        int distance = 0;

        while (distance < Searchlight.MAX_DISTANCE) {
            prevBlockPos.set(currentBlockPos);
            currentBlockPosD.add(beamDirection);
            currentBlockPos.set(currentBlockPosD.x, currentBlockPosD.y, currentBlockPosD.z);
            if (prevBlockPos.equals(currentBlockPos)) {
                // If we didn't move to a new block, we increment distance slightly to avoid infinite loops
                // but we don't want to increment it too fast. 
                // Since beamDirection is normalized, adding it should move us at least 0.5 blocks.
                // If it doesn't move us to a new block, it might be due to floating point precision
                // or very small components. 
                // However, a normalized vector ALWAYS has at least one component >= 1/sqrt(3) ~= 0.57.
                // So it should move us to a new block in at most 2 iterations.
                distance++; // Safety increment
                continue;
            }
            distance++;
            if (!level.isInWorldBounds(currentBlockPos)) return null;
            if (!level.isLoaded(currentBlockPos)) return null;

            BlockState currentBlockState = SearchlightUtil.getBlockStateForceLoad(level, currentBlockPos);
            if (!currentBlockState.isAir() && !currentBlockPos.equals(lightSourcePos) && !currentBlockPos.equals(getBlockPos())) {
                // Simplistic opacity check for now, can be improved to match vanilla realistic opacity if needed
                if (currentBlockState.getLightBlock(level, currentBlockPos) >= level.getMaxLightLevel() || !level.getFluidState(currentBlockPos).isEmpty()) {
                    return SearchlightUtil.moveAwayFromSurfaces(level, lastValidBlockPos);
                }
            }
            if (currentBlockState.isAir() || currentBlockPos.equals(lightSourcePos))
                lastValidBlockPos = currentBlockPos.immutable();
        }
        return lastValidBlockPos;
    }

    protected void setLightSourcePos(@Nullable BlockPos lightSourcePos) {
        this.lightSourcePos = lightSourcePos;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}