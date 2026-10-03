package dev.anvilcraft.pigeonplus.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.anvilcraft.pigeonplus.block.state.GiantFireworkPart;
import dev.anvilcraft.pigeonplus.entity.GiantFireworkEntity;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class GiantFireworkBlock extends SimpleMultiPartBlock<GiantFireworkPart>
    implements IHammerRemovable {
    public static final MapCodec<GiantFireworkBlock> CODEC = simpleCodec(GiantFireworkBlock::new);
    public static final EnumProperty<GiantFireworkPart> PART =
        EnumProperty.create("part", GiantFireworkPart.class);
    
    private static final AABB[] GLOBAL_SHAPE = {
        new AABB(11.0, 0.0, 11.0, 23.0, 32.0, 23.0),      // bottom el1：底部立柱（Y0~2 仅此一柱）
        new AABB(-12.0, 32.0, -12.0, 28.0, 48.0, 28.0),   // bottom el2：箭体下段
        new AABB(1.0, 25.0, 5.0, 3.0, 32.0, 7.0),         // bottom el3：侧壁小凸起
        new AABB(0.0, 18.0, 4.0, 2.0, 25.0, 6.0),         // bottom el4：侧壁小凸起
        new AABB(-12.0, 48.0, -12.0, 28.0, 96.0, 28.0),   // middle el1：箭体中段
        new AABB(-12.0, 96.0, -12.0, 28.0, 112.0, 28.0),  // top el1：箭体上段
        new AABB(-16.0, 112.0, -16.0, 32.0, 120.0, 32.0), // top el2：尾翼环（全箭最宽，3 格）
        new AABB(-12.0, 120.0, -12.0, 28.0, 128.0, 28.0), // top el3
        new AABB(-8.0, 128.0, -8.0, 24.0, 136.0, 24.0),   // top el4：收窄
        new AABB(-4.0, 136.0, -4.0, 20.0, 144.0, 20.0),   // top el5：箭尖
    };

    private static final Map<GiantFireworkPart, VoxelShape> SHAPES = createShapes();

    public GiantFireworkBlock(Properties properties) {
        super(properties);
        // 主部件 Y0_C 不是模型锚点，其模型为空；真实几何由 Y1_C/Y4_C/Y7_C 承载。
        this.registerDefaultState(this.stateDefinition.any().setValue(PART, GiantFireworkPart.Y0_C));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    public Property<GiantFireworkPart> getPart() {
        return PART;
    }

    @Override
    public GiantFireworkPart[] getParts() {
        return GiantFireworkPart.values();
    }

    /**
     * 主部件是底层中心，因此整个结构相对主部件只在 Y 轴正向延伸。
     */
    @Override
    public Vec3i getMainPartOffset() {
        return Vec3i.ZERO;
    }

    /**
     * 单个部件贴合模型的碰撞形状。
     *
     * <p>该形状同时被 {@code getCollisionShape}（实体碰撞）与
     * {@code getOcclusionShape}（遮挡/光照）使用，并被
     * {@code AbstractMultiPartBlock#getMultiPartShape} 按部件偏移合并成整支火箭的形状。
     */
    @Override
    public VoxelShape getPartShape(BlockState state) {
        if (!state.hasProperty(PART)) {
            return Shapes.block();
        }
        return SHAPES.getOrDefault(state.getValue(PART), Shapes.block());
    }

    /**
     * 模型承载状态：把任意部件映射到真正带几何的那一格（底段锚点），
     * 供铁砧锤预览、JEI 等展示场景查询，避免渲染到空模型上。
     */
    @Override
    public BlockState getModelHolderState(BlockState original) {
        return original.setValue(PART, GiantFireworkPart.Y1_C);
    }

    /**
     * 右键点火发射。
     *
     * <p>顺序必须是<b>先拆结构、再生实体</b>：实体碰撞箱是 3×3×9，
     * 若先放实体，它会立刻把这 81 格方块当成阻挡，下一 tick 就判定撞击而原地炸掉。
     *
     * <p>结构被消耗掉、不返还物品——这符合烟花火箭「点着就没了」的语义。
     * 也正因如此不调用 {@code dropResources}，否则拆掉的每一格都会掉一个方块。
     *
     * <h3>为什么先让铁砧锤走</h3>
     * 方块交互在物品 {@code useOn} <b>之前</b>执行（见 {@code ServerPlayerGameMode#useItemOn}），
     * 所以这里的 {@code CONSUME} 会抢在铁砧锤的拆除逻辑前面，
     * 导致 {@code IHammerRemovable} 对本方块失效、拿锤子右键反而把它射出去。
     * 因此手持铁砧锤时直接 {@code PASS}，把机会还给锤子。
     */
    @Override
    public InteractionResult use(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        if (player.getItemInHand(hand).getItem() instanceof AnvilHammerItem) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            // 客户端提前返回成功，避免手臂不摆动；真正的逻辑在服务端。
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        BlockPos mainPos = this.getMainPartPos(pos, state);
        this.removePartsAndUpdate(level, pos);
        GiantFireworkEntity.launch(serverLevel, mainPos, player);
        return InteractionResult.CONSUME;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        // 靠模型轮廓判断：箭体内部是实心整格（不向下透光），边缘与尾翼则是部分占位。
        return !Block.isShapeFullBlock(this.getPartShape(state)) && state.getFluidState().isEmpty();
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(PART, state.getValue(PART).rotate(rotation));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(PART, state.getValue(PART).mirror(mirror));
    }

    private static Map<GiantFireworkPart, VoxelShape> createShapes() {
        Map<GiantFireworkPart, VoxelShape> shapes = new EnumMap<>(GiantFireworkPart.class);
        for (GiantFireworkPart part : GiantFireworkPart.values()) {
            shapes.put(part, slicePartShape(part));
        }
        return shapes;
    }

    /**
     * 把 {@link #GLOBAL_SHAPE} 裁剪到 {@code part} 所在的那一格，并平移回该方块的局部坐标。
     *
     * <p>全部部件裁剪结果的并集等于原始 {@link #GLOBAL_SHAPE}
     * （因为该形状完全落在 81 格的范围内），所以整体形状不会缺料。
     */
    private static VoxelShape slicePartShape(GiantFireworkPart part) {
        double minX = part.getOffsetX() * 16.0;
        double minY = part.getOffsetY() * 16.0;
        double minZ = part.getOffsetZ() * 16.0;
        double maxX = minX + 16.0;
        double maxY = minY + 16.0;
        double maxZ = minZ + 16.0;
        List<AABB> localParts = new ArrayList<>();
        for (AABB globalPart : GLOBAL_SHAPE) {
            double clippedMinX = Math.max(globalPart.minX, minX);
            double clippedMinY = Math.max(globalPart.minY, minY);
            double clippedMinZ = Math.max(globalPart.minZ, minZ);
            double clippedMaxX = Math.min(globalPart.maxX, maxX);
            double clippedMaxY = Math.min(globalPart.maxY, maxY);
            double clippedMaxZ = Math.min(globalPart.maxZ, maxZ);
            if (clippedMinX >= clippedMaxX || clippedMinY >= clippedMaxY || clippedMinZ >= clippedMaxZ) {
                continue;
            }
            localParts.add(new AABB(
                clippedMinX - minX, clippedMinY - minY, clippedMinZ - minZ,
                clippedMaxX - minX, clippedMaxY - minY, clippedMaxZ - minZ
            ));
        }
        return localParts.isEmpty() ? Shapes.empty() : ShapeUtil.merge(localParts.toArray(AABB[]::new));
    }
}
