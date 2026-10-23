package dev.anvilcraft.pigeonplus.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.block.BlenderBlock;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.Map;
import javax.annotation.Nullable;

public class BlenderBlockEntityRenderer<T extends BlockEntity>
    implements BlockEntityRenderer<T>, ModelSelectionRenderer<T> {
    private static final float ROTATION_DEGREES_PER_TICK = 45.0f;
    private static final float HEAD_LIFT = 0.875f;
    private static final double RENDER_BOX_REACH = 1.0;
    private static final double RENDER_BOX_HEIGHT = 4.0;

    private static final Map<BlenderBlock.BlenderHead, ModelResourceLocation> HEAD_MODELS = Map.of(
        BlenderBlock.BlenderHead.IRON, headModel("blender_head"),
        BlenderBlock.BlenderHead.BLAZE, headModel("blender_head_blaze")
    );

    private static ModelResourceLocation headModel(String name) {
        return ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(AnvilCraftPigeonPlus.MOD_ID, "block/blender/" + name)
        );
    }

    public BlenderBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        T blockEntity,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay
    ) {
        ModelResourceLocation headLocation = headLocation(blockEntity.getBlockState());
        if (headLocation == null) {
            return;
        }
        int baseLight = getBaseLight(blockEntity, packedLight);
        poseStack.pushPose();
        BlenderBlockEntityRenderer.applyTransform(blockEntity, partialTick, poseStack);
        renderModel(blockEntity.getBlockState(), headLocation, poseStack, bufferSource, baseLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public void collectSelectionModels(
        T blockEntity,
        float partialTick,
        PoseStack poseStack,
        ModelConsumer consumer
    ) {
        ModelResourceLocation headLocation = headLocation(blockEntity.getBlockState());
        if (headLocation == null) {
            return;
        }
        poseStack.pushPose();
        BlenderBlockEntityRenderer.applyTransform(blockEntity, partialTick, poseStack);
        consumer.accept(headLocation, poseStack);
        poseStack.popPose();
    }

    @Nullable
    private static ModelResourceLocation headLocation(BlockState blockState) {
        if (!blockState.hasProperty(BlenderBlock.HEAD)) {
            return null;
        }
        return HEAD_MODELS.get(blockState.getValue(BlenderBlock.HEAD));
    }

    private static <T extends BlockEntity> void applyTransform(
        T blockEntity,
        float partialTick,
        PoseStack poseStack
    ) {
        Level level = blockEntity.getLevel();
        BlockState blockState = blockEntity.getBlockState();
        boolean hasCauldronAbove = level != null && BlenderBlockEntityRenderer.isLargeCauldronCore(
            level, blockEntity.getBlockPos().above(2));
        float angle = level != null && blockState.getValue(BlenderBlock.WORKING)
            ? (level.getGameTime() + partialTick) * ROTATION_DEGREES_PER_TICK
            : 0.0f;
        poseStack.translate(0.0, HEAD_LIFT, 0.0);
        poseStack.translate(0.5, 0.5, 0.5);
        if (hasCauldronAbove) {
            poseStack.scale(3.0f, 3.0f, 3.0f);
            poseStack.translate(0.0, 0.25, 0.0);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));
        poseStack.translate(-0.5, -0.5, -0.5);
    }

    @Override
    public AABB getRenderBoundingBox(T blockEntity) {
        return new AABB(blockEntity.getBlockPos())
            .inflate(RENDER_BOX_REACH)
            .expandTowards(0.0, RENDER_BOX_HEIGHT, 0.0);
    }

    private static int getBaseLight(BlockEntity blockEntity, int fallbackLight) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return fallbackLight;
        }
        return LevelRenderer.getLightColor(level, blockEntity.getBlockPos());
    }

    private static boolean isLargeCauldronCore(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        ResourceLocation id = state.getBlock().builtInRegistryHolder().key().location();
        if (!id.equals(ResourceLocation.fromNamespaceAndPath("anvilcraft", "large_cauldron"))) {
            return false;
        }
        Property<?> halfProp = state.getBlock().getStateDefinition().getProperty("half");
        if (halfProp == null) {
            return false;
        }
        return "mid_center".equals(state.getValue(halfProp).toString());
    }

    private static void renderModel(
        BlockState state,
        ModelResourceLocation modelLocation,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        ModelManager modelManager = minecraft.getModelManager();
        BlockRenderDispatcher dispatcher = minecraft.getBlockRenderer();
        BakedModel model = modelManager.getModel(modelLocation);
        RandomSource random = RandomSource.create();
        ModelData modelData = ModelData.EMPTY;
        for (RenderType renderType : model.getRenderTypes(state, random, modelData)) {
            VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
            dispatcher.getModelRenderer().renderModel(
                poseStack.last(),
                vertexConsumer,
                state,
                model,
                1.0F,
                1.0F,
                1.0F,
                packedLight,
                packedOverlay,
                modelData,
                renderType
            );
        }
    }
}
