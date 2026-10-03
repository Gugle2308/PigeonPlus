package dev.anvilcraft.pigeonplus.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.pigeonplus.block.GiantFireworkBlock;
import dev.anvilcraft.pigeonplus.block.state.GiantFireworkPart;
import dev.anvilcraft.pigeonplus.entity.GiantFireworkEntity;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * 巨型烟花实体的渲染器：把方块那三个 Blockbench 模型整体搬过来。
 *
 * <h2>为什么按锚点偏移分三段摆</h2>
 * 三个模型各跨 3 格高，是为「方块」设计的：每段都以其所在方块的角为原点、Y 覆盖
 * -16~32（即上下各伸出 1 格）。多方块里它们分别挂在 {@code Y1_C}/{@code Y4_C}/{@code Y7_C}
 * 三个锚点上，也就是主部件之上的第 1/4/7 格。实体只需复现同一组偏移，
 * 外观就与发射前完全一致。
 *
 * <p>三个锚点的 X/Z 偏移都是 0，所以只需在 Y 上平移；又因为模型在 X/Z 上以 8（即 0.5 格）
 * 为中心对称，实体位置取底面中心时不需左右补偿，只把 {@code (-0.5, 0, -0.5)}
 * 那半格补回到方块角即可。
 *
 * <h2>为什么用 {@code renderBatched} 而不是 {@code renderModel}</h2>
 * 这是本渲染器的关键。{@code ModelBlockRenderer#renderModel}（BER 常用那个重载）
 * 内部直接走 {@code renderQuadList}，<b>完全不调用 {@code level.getShade(...)}</b>，
 * 于是六个面都是满亮度——实体就会比方块明显发亮、发平。
 *
 * <p>{@code BlockRenderDispatcher#renderBatched} 走的是 {@code tesselateBlock}，
 * 也就是方块在世界里的那条路径：它会给每个面乘上
 * {@code ClientLevel#getShade}（顶 1.0 / 南北 0.8 / 东西 0.6 / 底 0.5），
 * 并按周围方块做环境光遮蔽（AO）。用它渲染，实体的明暗才和方块一致。
 *
 * <p>因此这里传入<b>真实的 {@link ClientLevel} 与实体所在 {@link BlockPos}</b>：
 * {@code tesselateBlock} 的光照与 AO 都是从这两者现算的，传 {@code BlockPos.ZERO}
 * 或自定义的假 level 会采样到无关位置（例如世界原点的石头）而算错明暗。
 *
 * <p>模型本身直接由方块状态经 blockstate JSON 解析（{@code part=y1_c} → 底段模型），
 * 这样实体用的就是<b>同一个</b>模型与渲染层，无需另注册 standalone 模型。
 */
public class GiantFireworkRenderer extends EntityRenderer<GiantFireworkEntity> {
    /** 三个模型分段：承载该段模型的部件 + 相对主部件的 Y 偏移（格）。 */
    private static final Segment[] SEGMENTS = {
        new Segment(GiantFireworkPart.Y1_C, 1),
        new Segment(GiantFireworkPart.Y4_C, 4),
        new Segment(GiantFireworkPart.Y7_C, 7),
    };

    @SuppressWarnings("unused")
    public GiantFireworkRenderer(EntityRendererProvider.Context context) {
        super(context);
        // 火箭本体就是阴影，再画一圈椭圆阴影只会显得脏。
        this.shadowRadius = 0.0F;
        this.shadowStrength = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(GiantFireworkEntity entity) {
        // 模型自带纹理，不使用实体贴图；返回占位以满足抽象方法。
        return ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    }

    @Override
    public void render(
        GiantFireworkEntity entity,
        float entityYaw,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight
    ) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BlockState base = AddonBlocks.GIANT_FIREWORK.get().defaultBlockState();
        BlockPos entityPos = entity.blockPosition();
        RandomSource random = RandomSource.create();

        poseStack.pushPose();
        // 实体位置是底面中心，而方块模型以方块的角为原点，补回半格。
        poseStack.translate(-0.5, 0.0, -0.5);
        for (Segment segment : SEGMENTS) {
            BlockState state = base.setValue(GiantFireworkBlock.PART, segment.part());
            RenderType renderType = ItemBlockRenderTypes.getChunkRenderType(state);
            VertexConsumer consumer = buffer.getBuffer(renderType);
            poseStack.pushPose();
            poseStack.translate(0.0, segment.offsetY(), 0.0);
            // pos 必须跟着段一起上移，否则 AO 会一直采样底面那一层。
            dispatcher.renderBatched(
                state,
                entityPos.offset(0, segment.offsetY(), 0),
                level,
                poseStack,
                consumer,
                false,
                random,
                ModelData.EMPTY,
                renderType
            );
            poseStack.popPose();
        }
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    /** 一个模型分段。 */
    private record Segment(GiantFireworkPart part, int offsetY) {
    }
}
