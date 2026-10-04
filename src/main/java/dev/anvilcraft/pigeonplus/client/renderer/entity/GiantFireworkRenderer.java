package dev.anvilcraft.pigeonplus.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
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
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

/**
 * 巨型烟花实体的渲染器：复用方块那三个 Blockbench 模型。
 *
 * <p>三个模型各跨 3 格高，在多方块里挂在 {@code Y1_C}/{@code Y4_C}/{@code Y7_C} 锚点上，
 * 实体复现同一组 Y 偏移即可；锚点 X/Z 偏移都是 0，而模型在 X/Z 上以 0.5 格为中心对称，
 * 所以只补回 {@code (-0.5, 0, -0.5)} 那半格。
 *
 * <p>中段与顶段直接由方块状态解析（{@code part=y4_c}/{@code part=y7_c}），
 * 底段换成点燃版 {@code lit_giant_fireworks_bottom}（实体专用，已单独登记为独立模型）。
 *
 * <p>用 {@code tesselateBlock} 而非 {@code renderBatched}：后者只能吃方块状态解析出的模型，
 * 而点燃版模型不是任何方块状态的模型。两者走的是同一条着色路径——
 * {@code renderModel} 那条路不调用 {@code level.getShade(...)}，六个面都是满亮度、显得过亮发平。
 * 所以必须传真实的 {@link ClientLevel} 与实体所在 {@link BlockPos}，否则会采样到无关位置而算错明暗。
 */
public class GiantFireworkRenderer extends EntityRenderer<GiantFireworkEntity> {
    /** 点燃后的底段模型（实体专用，不从方块状态解析）。 */
    private static final ResourceLocation LIT_BOTTOM = ResourceLocation.fromNamespaceAndPath(
        AnvilCraftPigeonPlus.MOD_ID, "block/lit_giant_fireworks_bottom");

    /** 三个模型分段：承载该段模型的部件 + 相对主部件的 Y 偏移（格）。 */
    private static final Segment[] SEGMENTS = {
        new Segment(GiantFireworkPart.Y1_C, 1, ModelResourceLocation.standalone(LIT_BOTTOM)),
        new Segment(GiantFireworkPart.Y4_C, 4, null),
        new Segment(GiantFireworkPart.Y7_C, 7, null),
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
        // 起爆阶段箭体已经炸开，只剩逐颗绽放的星星；此时再画一支完整的火箭会很出戏。
        if (entity.isDetonating()) {
            return;
        }
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BlockState base = AddonBlocks.GIANT_FIREWORK.get().defaultBlockState();
        BlockPos entityPos = entity.blockPosition();
        RandomSource random = RandomSource.create();

        poseStack.pushPose();
        // 朝向：与 blockstate 的 y 旋转同一约定（north=0 / east=90 / south=180 / west=270）。
        //
        // 旋转必须<b>先于</b>平移：PoseStack 是右乘（mulPose 后再 translate 等于在该旋转下平移），
        // 先平移会让旋转中心落在方块角上，模型整体偏离一格多。
        // 当前顺序的旋转中心是实体原点，即方块中心（X/Z）+ 方块底面（Y），与模型自转中心一致。
        poseStack.mulPose(Axis.YP.rotationDegrees(yawFor(entity.getFacing())));
        // 模型以方块角为原点，实体在底面中心，补回半格。
        poseStack.translate(-0.5, 0.0, -0.5);
        for (Segment segment : SEGMENTS) {
            BlockState state = base.setValue(GiantFireworkBlock.PART, segment.part())
                .setValue(GiantFireworkBlock.FACING, entity.getFacing());
            RenderType renderType = ItemBlockRenderTypes.getChunkRenderType(state);
            VertexConsumer consumer = buffer.getBuffer(renderType);
            poseStack.pushPose();
            poseStack.translate(0.0, segment.offsetY(), 0.0);
            // pos 必须跟着段一起上移，否则 AO 会一直采样底面那一层。
            BlockPos segmentPos = entityPos.offset(0, segment.offsetY(), 0);
            dispatcher.getModelRenderer().tesselateBlock(
                level,
                this.resolveModel(dispatcher, state, segment),
                state,
                segmentPos,
                poseStack,
                consumer,
                false,
                random,
                state.getSeed(segmentPos),
                OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY,
                renderType
            );
            poseStack.popPose();
        }
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    /** 朝向对应的 Y 轴旋转角度，与原版 blockstate 的 {@code y} 约定一致。 */
    private static float yawFor(Direction facing) {
        return switch (facing) {
            case EAST -> 270.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }

    /**
     * 取该分段要用的模型：底段换成点燃版 {@code lit_giant_fireworks_bottom}，其余用方块状态解析。
     *
     * <p>点燃版只有 UV 不同（几何完全一致），所以碰撞箱与分段偏移都不用改。
     * 直接调 {@code tesselateBlock} 而不是 {@code renderBatched}，只是为了能传入这个
     * 不从方块状态来的模型；两者走的是同一条着色路径。
     */
    private BakedModel resolveModel(BlockRenderDispatcher dispatcher, BlockState state, Segment segment) {
        if (segment.standalone() == null) {
            return dispatcher.getBlockModel(state);
        }
        return Minecraft.getInstance().getModelManager().getModel(segment.standalone());
    }

    /** 一个模型分段；{@code standalone} 非空时用它替代方块状态解析出的模型。 */
    private record Segment(GiantFireworkPart part, int offsetY, @Nullable ModelResourceLocation standalone) {
    }
}
