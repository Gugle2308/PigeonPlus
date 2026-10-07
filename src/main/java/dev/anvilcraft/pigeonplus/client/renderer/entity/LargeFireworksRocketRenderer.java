package dev.anvilcraft.pigeonplus.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.block.LargeFireworksRocketBlock;
import dev.anvilcraft.pigeonplus.block.state.LargeFireworksRocketPart;
import dev.anvilcraft.pigeonplus.entity.LargeFireworksRocketEntity;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

/**
 * 巨型烟花实体的渲染器：复用方块那三个 Blockbench 模型。
 *
 * <p>三个模型各跨 3 格高，在多方块里挂在 {@code Y1_C}/{@code Y4_C}/{@code Y7_C} 锚点上，
 * 实体复现同一组 Y 偏移即可；锚点 X/Z 偏移都是 0，而模型在 X/Z 上以 0.5 格为中心对称，
 * 所以只补回 {@code (-0.5, 0, -0.5)} 那半格。
 *
 * <p>中段与顶段直接由方块状态解析（{@code part=y4_c}/{@code part=y7_c}），
 * 底段换成点燃版 {@code lit_large_fireworks_rocket_bottom}（实体专用，已单独登记为独立模型）。
 *
 * <p>用 {@code tesselateBlock} 而非 {@code renderBatched}：后者只能吃方块状态解析出的模型，
 * 而点燃版模型不是任何方块状态的模型。两者走的是同一条着色路径——
 * {@code renderModel} 那条路不调用 {@code level.getShade(...)}，六个面都是满亮度、显得过亮发平。
 * 所以必须传真实的 {@link ClientLevel} 与实体所在 {@link BlockPos}，否则会采样到无关位置而算错明暗。
 */
public class LargeFireworksRocketRenderer extends EntityRenderer<LargeFireworksRocketEntity> {
    /** 点燃后的底段模型（实体专用，不从方块状态解析）。 */
    private static final ResourceLocation LIT_BOTTOM = ResourceLocation.fromNamespaceAndPath(
        AnvilCraftPigeonPlus.MOD_ID, "block/lit_large_fireworks_rocket_bottom");

    /**
     * 悬浮的末影珍珠相对实体原点（底面）的高度（格）。
     *
     * <p>与方块形态的渲染器共用 {@link LargeFireworksRocketEntity#PEARL_RENDER_HEIGHT}。
     */
    private static final double PEARL_HEIGHT = LargeFireworksRocketEntity.PEARL_RENDER_HEIGHT;

    /**
     * 悬浮珍珠的缩放倍数。与方块形态的渲染器共用
     * {@link LargeFireworksRocketEntity#PEARL_RENDER_SCALE}（那里解释了为什么需要它）。
     */
    private static final float PEARL_SCALE = LargeFireworksRocketEntity.PEARL_RENDER_SCALE;

    /** 渲染悬浮物品用。{@code EntityRenderer} 本身不持有，需从 Context 取。 */
    private final ItemRenderer itemRenderer;

    /** 三个模型分段：承载该段模型的部件 + 相对主部件的 Y 偏移（格）。 */
    private static final Segment[] SEGMENTS = {
        new Segment(LargeFireworksRocketPart.Y1_C, 1, ModelResourceLocation.standalone(LIT_BOTTOM)),
        new Segment(LargeFireworksRocketPart.Y4_C, 4, null),
        new Segment(LargeFireworksRocketPart.Y7_C, 7, null),
    };

    @SuppressWarnings("unused")
    public LargeFireworksRocketRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        // 火箭本体就是阴影，再画一圈椭圆阴影只会显得脏。
        this.shadowRadius = 0.0F;
        this.shadowStrength = 0.0F;
    }

    @Override
    public @NonNull ResourceLocation getTextureLocation(@NonNull LargeFireworksRocketEntity entity) {
        // 模型自带纹理，不使用实体贴图；返回占位以满足抽象方法。
        return ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    }

    @Override
    public void render(
        @NonNull LargeFireworksRocketEntity entity,
        float entityYaw,
        float partialTick,
        @NonNull PoseStack poseStack,
        @NonNull MultiBufferSource buffer,
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
        BlockState base = AddonBlocks.LARGE_FIREWORKS_ROCKET.get().defaultBlockState();
        BlockPos entityPos = entity.blockPosition();
        RandomSource random = RandomSource.create();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yawFor(entity.getFacing())));
        poseStack.translate(-0.5, 0.0, -0.5);
        for (Segment segment : SEGMENTS) {
            BlockState state = base.setValue(LargeFireworksRocketBlock.PART, segment.part())
                .setValue(LargeFireworksRocketBlock.FACING, entity.getFacing());
            RenderType renderType = ItemBlockRenderTypes.getChunkRenderType(state);
            VertexConsumer consumer = buffer.getBuffer(renderType);
            poseStack.pushPose();
            poseStack.translate(0.0, segment.offsetY(), 0.0);
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

        // 装了末影珍珠就在箭尖上方悬一颗，提示「逃逸塔已就绪」。
        if (entity.getPearls() > 0) {
            this.renderPearl(entity, poseStack, buffer, level);
        }

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    /**
     * 装填了末影珍珠时，在箭尖上方显示一颗末影珍珠物品。
     */
    private void renderPearl(
        LargeFireworksRocketEntity entity,
        PoseStack poseStack,
        MultiBufferSource buffer,
        ClientLevel level
    ) {
        // 从实体原点（底面中心）起算，不再叠加箭体的 -0.5 半格修正：
        // 那是给「以方块角为原点」的方块模型用的，而物品模型以原点为中心。
        poseStack.pushPose();
        poseStack.translate(0.0, PEARL_HEIGHT, 0.0);
        // 刻意不加旋转：保持模型自身朝向（面向南北）。
        // 抵消 GROUND 自带的 0.5 缩放（详见 PEARL_RENDER_SCALE 的说明）。
        poseStack.scale(PEARL_SCALE, PEARL_SCALE, PEARL_SCALE);
        BlockPos pearlPos = BlockPos.containing(
            entity.getX(), entity.getY() + PEARL_HEIGHT, entity.getZ());
        this.itemRenderer.renderStatic(
            new ItemStack(Items.ENDER_PEARL),
            ItemDisplayContext.GROUND,
            LevelRenderer.getLightColor(level, pearlPos),
            OverlayTexture.NO_OVERLAY,
            poseStack,
            buffer,
            level,
            entity.getId()
        );
        poseStack.popPose();
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
     * 取该分段要用的模型：底段换成点燃版 {@code lit_large_fireworks_rocket_bottom}
     */
    private BakedModel resolveModel(BlockRenderDispatcher dispatcher, BlockState state, Segment segment) {
        if (segment.standalone() == null) {
            return dispatcher.getBlockModel(state);
        }
        return Minecraft.getInstance().getModelManager().getModel(segment.standalone());
    }

    /** 一个模型分段；{@code standalone} 非空时用它替代方块状态解析出的模型。 */
    private record Segment(LargeFireworksRocketPart part, int offsetY, @Nullable ModelResourceLocation standalone) {
    }
}