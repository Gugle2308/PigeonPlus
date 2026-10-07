package dev.anvilcraft.pigeonplus.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.pigeonplus.block.entity.LargeFireworksRocketBlockEntity;
import dev.anvilcraft.pigeonplus.entity.LargeFireworksRocketEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

/**
 * 大型烟花<b>方块形态</b>的渲染器：装填了末影珍珠时，在箭尖上方显示一颗末影珍珠物品。
 *
 * <p>方块本体（箭体、尾翼）由 blockstate 的模型负责，这里只画那一颗珍珠——
 * 它是「逃逸塔已就绪」的视觉提示，让玩家在点火前就能确认珍珠装好了。
 *
 * <p>只在主部件（底层中心）持有方块实体，所以本渲染器天然只在主部件上跑一次。
 *
 * <h2>朝向固定，不做 billboard</h2>
 * 生成的 2D 物品模型是单个 {@code [0,0,7.5] → [16,16,8.5]} 的扁平片，
 * 只有 NORTH/SOUTH 两个面，本身就是「一块薄板」——让它随相机转，
 * 反而像一片翻来翻去的纸。代价是从正东/正西看时是纯侧面，看不到它。
 *
 * <p>渲染箱必须向上延伸到珍珠所在高度，否则会被视锥剔除掉——
 * 方块实体默认的渲染箱只覆盖自己那一格，而珍珠在 9 格之上。
 */
public class LargeFireworksRocketBlockEntityRenderer
    implements BlockEntityRenderer<LargeFireworksRocketBlockEntity> {

    private static final float PEARL_SCALE = LargeFireworksRocketEntity.PEARL_RENDER_SCALE;

    public LargeFireworksRocketBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        LargeFireworksRocketBlockEntity blockEntity,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay
    ) {
        if (blockEntity.getLevel() == null || blockEntity.getPearlCount() <= 0) {
            return;
        }
        BlockPos pos = blockEntity.getBlockPos();
        poseStack.pushPose();
        // 方块实体的原点是方块最小角，珍珠要落在结构的水平中心，所以补半格。
        poseStack.translate(0.5, LargeFireworksRocketEntity.PEARL_RENDER_HEIGHT, 0.5);
        // 刻意不加旋转：与实体形态一致，保持模型自身朝向（面向南北）。
        // 与实体形态同一缩放，抵消 GROUND 自带的 0.5。
        poseStack.scale(PEARL_SCALE, PEARL_SCALE, PEARL_SCALE);
        // 亮度取珍珠所在位置，而不是方块自身那格：它高出 9 格，光照等级可能完全不同。
        BlockPos pearlPos = BlockPos.containing(
            pos.getX() + 0.5,
            pos.getY() + LargeFireworksRocketEntity.PEARL_RENDER_HEIGHT,
            pos.getZ() + 0.5
        );
        Minecraft.getInstance().getItemRenderer().renderStatic(
            new ItemStack(Items.ENDER_PEARL),
            ItemDisplayContext.GROUND,
            LevelRenderer.getLightColor(blockEntity.getLevel(), pearlPos),
            OverlayTexture.NO_OVERLAY,
            poseStack,
            bufferSource,
            blockEntity.getLevel(),
            // 用方块位置作种子：同一位置每帧稳定，不会让贴图随机抖动。
            (int) pos.asLong()
        );
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(LargeFireworksRocketBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        double top = pos.getY() + LargeFireworksRocketEntity.PEARL_RENDER_HEIGHT + 1.0;
        return new AABB(
            pos.getX() - 1.0, pos.getY(), pos.getZ() - 1.0,
            pos.getX() + 2.0, top, pos.getZ() + 2.0
        );
    }
}
