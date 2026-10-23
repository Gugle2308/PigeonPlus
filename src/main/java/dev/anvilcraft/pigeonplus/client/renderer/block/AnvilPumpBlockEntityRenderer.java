package dev.anvilcraft.pigeonplus.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.block.AnvilPumpBlock;
import dev.anvilcraft.pigeonplus.block.entity.AnvilPumpBlockEntity;
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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

public class AnvilPumpBlockEntityRenderer
    implements BlockEntityRenderer<AnvilPumpBlockEntity>, ModelSelectionRenderer<AnvilPumpBlockEntity> {
    private static final ModelResourceLocation PISTON_MODEL = ModelResourceLocation.standalone(
        ResourceLocation.fromNamespaceAndPath(AnvilCraftPigeonPlus.MOD_ID, "block/anvil_pump_pistion")
    );
    private static final float MAX_PISTON_DROP = 8.0F / 16.0F;

    @SuppressWarnings("unused")
    public AnvilPumpBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        AnvilPumpBlockEntity blockEntity,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay
    ) {
        int baseLight = getBaseLight(blockEntity, packedLight);
        poseStack.pushPose();
        AnvilPumpBlockEntityRenderer.applyTransform(blockEntity, partialTick, poseStack);
        renderModel(blockEntity.getBlockState(), PISTON_MODEL, poseStack, bufferSource, baseLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public void collectSelectionModels(
        AnvilPumpBlockEntity blockEntity,
        float partialTick,
        PoseStack poseStack,
        ModelConsumer consumer
    ) {
        poseStack.pushPose();
        AnvilPumpBlockEntityRenderer.applyTransform(blockEntity, partialTick, poseStack);
        consumer.accept(PISTON_MODEL, poseStack);
        poseStack.popPose();
    }

    private static void applyTransform(
        AnvilPumpBlockEntity blockEntity,
        float partialTick,
        PoseStack poseStack
    ) {
        BlockState state = blockEntity.getBlockState();
        if (state.getBlock() instanceof AnvilPumpBlock && state.hasProperty(AnvilPumpBlock.FACING)) {
            Direction facing = state.getValue(AnvilPumpBlock.FACING);
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-AnvilPumpBlock.getYRotation(facing)));
            poseStack.translate(-0.5F, -0.5F, -0.5F);
        }
        poseStack.translate(0.0F, -MAX_PISTON_DROP * blockEntity.getPistonPress(partialTick), 0.0F);
    }

    private static int getBaseLight(AnvilPumpBlockEntity blockEntity, int fallbackLight) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return fallbackLight;
        }
        return LevelRenderer.getLightColor(level, blockEntity.getBlockPos());
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
