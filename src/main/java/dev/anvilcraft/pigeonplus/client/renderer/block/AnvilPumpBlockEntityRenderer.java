package dev.anvilcraft.pigeonplus.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.block.AnvilPumpBlock;
import dev.anvilcraft.pigeonplus.block.entity.AnvilPumpBlockEntity;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public class AnvilPumpBlockEntityRenderer
    implements BlockEntityRenderer<AnvilPumpBlockEntity>, ModelSelectionRenderer<AnvilPumpBlockEntity> {
    private static final ModelResourceLocation PISTON = ModelResourceLocation.standalone(
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
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay
    ) {
        poseStack.pushPose();
        if (!AnvilPumpBlockEntityRenderer.applyTransform(blockEntity, partialTick, poseStack)) {
            poseStack.popPose();
            return;
        }
        BakedModel piston = Minecraft.getInstance().getModelManager().getModel(PISTON);
        Minecraft.getInstance()
            .getBlockRenderer()
            .getModelRenderer()
            .renderModel(
                poseStack.last(),
                buffer.getBuffer(RenderType.cutout()),
                null,
                piston,
                1.0F,
                1.0F,
                1.0F,
                packedLight,
                packedOverlay
            );
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
        if (AnvilPumpBlockEntityRenderer.applyTransform(blockEntity, partialTick, poseStack)) {
            consumer.accept(PISTON, poseStack);
        }
        poseStack.popPose();
    }

    private static boolean applyTransform(
        AnvilPumpBlockEntity blockEntity,
        float partialTick,
        PoseStack poseStack
    ) {
        BlockState state = blockEntity.getBlockState();
        if (!(state.getBlock() instanceof AnvilPumpBlock) || !state.hasProperty(AnvilPumpBlock.FACING)) {
            return false;
        }
        Direction facing = state.getValue(AnvilPumpBlock.FACING);
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-AnvilPumpBlock.getYRotation(facing)));
        poseStack.translate(-0.5, -0.5, -0.5);
        poseStack.translate(0.0F, -MAX_PISTON_DROP * blockEntity.getPistonPress(partialTick), 0.0F);
        return true;
    }
}
