package com.csykes.searchlight.features.searchlight;

import com.csykes.searchlight.MutableVector3d;
import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.utils.SearchlightUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SearchlightLightSourceBlockEntity extends BlockEntity {
    public @Nullable BlockPos searchlightBlockPos;
    public boolean suppressMovement = false;

    public SearchlightLightSourceBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(Searchlight.LIGHT_SOURCE_BE.get(), blockPos, blockState);
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
        if (searchlightBlockPos != null) {
            tag.putInt("searchlight_x", searchlightBlockPos.getX());
            tag.putInt("searchlight_y", searchlightBlockPos.getY());
            tag.putInt("searchlight_z", searchlightBlockPos.getZ());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("searchlight_x") && tag.contains("searchlight_y") && tag.contains("searchlight_z"))
            searchlightBlockPos = new BlockPos(tag.getInt("searchlight_x"), tag.getInt("searchlight_y"), tag.getInt("searchlight_z"));
        else
            searchlightBlockPos = null;
    }

    public void moveLightSource() {
        if (suppressMovement)
            return;
        if (level == null || level.isClientSide || searchlightBlockPos == null)
            return;
        SearchlightUtil.castBlockEntity(level.getBlockEntity(searchlightBlockPos), searchlightBlockPos, (SearchlightBlockEntity searchlightBlockEntity) -> {
            if (getBlockPos().equals(searchlightBlockEntity.getLightSourcePos()))
                searchlightBlockEntity.placeLightSource(calculateLightSourcePosition(searchlightBlockEntity.getBeamDirection().scale(-1)));
        });
    }

    public @Nullable BlockPos calculateLightSourcePosition(@NotNull Vec3 direction) {
        direction = direction.normalize();
        MutableVector3d currentBlockPosD = new MutableVector3d(getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5);
        BlockPos.MutableBlockPos currentBlockPos = new BlockPos.MutableBlockPos(currentBlockPosD.x, currentBlockPosD.y, currentBlockPosD.z);
        BlockPos.MutableBlockPos prevBlockPos = new BlockPos.MutableBlockPos(0, 0, 0);

        while (true) {
            prevBlockPos.set(currentBlockPos);
            currentBlockPosD.add(direction);
            currentBlockPos.set(currentBlockPosD.x, currentBlockPosD.y, currentBlockPosD.z);

            if (prevBlockPos.equals(currentBlockPos))
                continue;

            if (!level.isInWorldBounds(currentBlockPos))
                return null;

            if (!level.isLoaded(currentBlockPos))
                return null;

            if (currentBlockPos.equals(searchlightBlockPos))
                return null;

            if (level.getBlockState(currentBlockPos).isAir())
                return SearchlightUtil.moveAwayFromSurfaces(level, currentBlockPos);
        }
    }
}