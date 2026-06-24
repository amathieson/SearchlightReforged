package com.csykes.searchlight.features.searchlight;

import com.csykes.searchlight.utils.lighting.AbstractLightBlock;
import net.minecraft.client.model.geom.ModelPart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.csykes.searchlight.utils.SearchlightUtil;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * This class renders the searchlight block entity
 * Also used for the debug light beam
 */
public class SearchlightBlockRenderer implements BlockEntityRenderer<SearchlightBlockEntity> {
    protected static final ResourceLocation SEARCHLIGHT_BODY_TEXTURE = ResourceLocation.fromNamespaceAndPath("searchlight", "textures/block/searchlight.png");
    protected static final ResourceLocation SEARCHLIGHT_BEAM = ResourceLocation.fromNamespaceAndPath("searchlight", "textures/block/searchlight_beam.png");

    protected static final Vec3 CEILING_PIVOT = new Vec3(8, 10, 8);
    protected static final Vec3 FLOOR_PIVOT = new Vec3(8, 6, 8);
    protected static final Vec3 NORTH_PIVOT = new Vec3(8, 8, 12);
    protected static final Vec3 SOUTH_PIVOT = new Vec3(8, 8, 4);
    protected static final Vec3 WEST_PIVOT = new Vec3(12, 8, 8);
    protected static final Vec3 EAST_PIVOT = new Vec3(4, 8, 8);

    protected final ModelPart onWallBody;
    protected final ModelPart onWallLightFace;
    protected final ModelPart onFloorBody;
    protected final ModelPart onFloorLightFace;

    public SearchlightBlockRenderer(BlockEntityRendererProvider.Context context) {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -6, -3, 6, 7, 6), PartPose.ZERO);
        root.addOrReplaceChild("front", CubeListBuilder.create().texOffs(0, 13).addBox(4, 4, 4, 8, 2, 8), PartPose.offset(-8, -12, -8));
        onWallBody = LayerDefinition.create(meshDefinition, 32, 32).bakeRoot();

        MeshDefinition faceMesh = new MeshDefinition();
        faceMesh.getRoot().addOrReplaceChild("face", CubeListBuilder.create().texOffs(0, 23).addBox(-4, -8, -4, 8, 1, 8), PartPose.ZERO);
        onWallLightFace = LayerDefinition.create(faceMesh, 32, 32).bakeRoot();

        MeshDefinition floorMesh = new MeshDefinition();
        PartDefinition floorRoot = floorMesh.getRoot();
        floorRoot.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -4, -3, 6, 7, 6), PartPose.ZERO);
        floorRoot.addOrReplaceChild("front", CubeListBuilder.create().texOffs(0, 13).addBox(4, 4, 4, 8, 2, 8), PartPose.offset(-8, -10, -8));
        onFloorBody = LayerDefinition.create(floorMesh, 32, 32).bakeRoot();

        MeshDefinition floorFaceMesh = new MeshDefinition();
        floorFaceMesh.getRoot().addOrReplaceChild("face", CubeListBuilder.create().texOffs(0, 23).addBox(-4, -6, -4, 8, 1, 8), PartPose.ZERO);
        onFloorLightFace = LayerDefinition.create(floorFaceMesh, 32, 32).bakeRoot();
    }

    @Override
    public int getViewDistance() {
        return SearchlightUtil.displayBeams() ? 256 : BlockEntityRenderer.super.getViewDistance();
    }

    @Override
    public boolean shouldRenderOffScreen(SearchlightBlockEntity blockEntity) {
        return true;
    }

    @Override
    public void render(SearchlightBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        Vec3 pivot = getModelPivot(blockEntity);
        Vec3 direction = blockEntity.getBeamDirection();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(SEARCHLIGHT_BODY_TEXTURE));

        boolean isOnWall = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE) == AttachFace.WALL;
        ModelPart body = isOnWall ? onWallBody : onFloorBody;
        ModelPart lightFace = isOnWall ? onWallLightFace : onFloorLightFace;

        body.setPos((float) pivot.x, (float) pivot.y, (float) pivot.z);
        body.yRot = (float) Mth.atan2(direction.x, direction.z);
        body.xRot = (float) (Mth.atan2(Mth.sqrt((float) (direction.z * direction.z + direction.x * direction.x)), (float) direction.y) + Math.PI);
        body.render(poseStack, vertexConsumer, packedLight, packedOverlay);

        boolean shouldRenderLight = blockEntity.getLightSourcePos() != null && state.getValue(AbstractLightBlock.LIT);
        if (shouldRenderLight) {
            lightFace.setPos((float) pivot.x, (float) pivot.y, (float) pivot.z);
            lightFace.yRot = body.yRot;
            lightFace.xRot = body.xRot;
            lightFace.render(poseStack, vertexConsumer, 15728880, OverlayTexture.NO_OVERLAY);
        }

        if (SearchlightUtil.displayBeams() && blockEntity.getLightSourcePos() != null && state.getValue(AbstractLightBlock.LIT)) {
            float distance = (float) Mth.sqrt((float) blockEntity.getLightSourcePos().distSqr(blockEntity.getBlockPos())) + 1.0f;
            drawBeam(pivot, body.yRot, body.xRot, distance, poseStack, bufferSource, blockEntity.getLevel().getGameTime(), partialTick);
        }
    }

    /**
     * Draws the beam of the searchlight
     * Looks like a beacon beam
     * only shows if a searchlight is being held
     * @param pivot
     * @param yRot
     * @param xRot
     * @param distance
     * @param poseStack
     * @param bufferSource
     * @param gameTime
     * @param partialTick
     */
    protected void drawBeam(Vec3 pivot, float yRot, float xRot, float distance, PoseStack poseStack, MultiBufferSource bufferSource, long gameTime, float partialTick) {
        poseStack.pushPose();
        poseStack.translate(pivot.x / 16.0, pivot.y / 16.0, pivot.z / 16.0);
        poseStack.mulPose(Axis.YP.rotation(yRot));
        poseStack.mulPose(Axis.XP.rotation((float) (Math.PI + xRot)));
        poseStack.translate(-0.5, 0.35, -0.5);

        BeaconRenderer.renderBeaconBeam(poseStack, bufferSource, SEARCHLIGHT_BEAM, partialTick, 1.0f, gameTime, 0, (int) distance, 0xC8A2C8, 1.75f, 0.08f);
        poseStack.popPose();
    }

    protected Vec3 getModelPivot(SearchlightBlockEntity blockEntity) {
        BlockState state = blockEntity.getBlockState();
        AttachFace face = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE);
        if (face == AttachFace.CEILING) return CEILING_PIVOT;
        if (face == AttachFace.FLOOR) return FLOOR_PIVOT;

        return switch (state.getValue(FaceAttachedHorizontalDirectionalBlock.FACING)) {
            case EAST -> EAST_PIVOT;
            case WEST -> WEST_PIVOT;
            case SOUTH -> SOUTH_PIVOT;
            default -> NORTH_PIVOT;
        };
    }
}
