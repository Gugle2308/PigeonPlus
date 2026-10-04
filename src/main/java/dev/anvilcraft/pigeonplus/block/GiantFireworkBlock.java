package dev.anvilcraft.pigeonplus.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.anvilcraft.pigeonplus.block.entity.GiantFireworkBlockEntity;
import dev.anvilcraft.pigeonplus.block.state.GiantFireworkPart;
import dev.anvilcraft.pigeonplus.entity.GiantFireworkEntity;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class GiantFireworkBlock extends SimpleMultiPartBlock<GiantFireworkPart>
    implements IHammerRemovable, EntityBlock {
    public static final MapCodec<GiantFireworkBlock> CODEC = simpleCodec(GiantFireworkBlock::new);
    public static final EnumProperty<GiantFireworkPart> PART =
        EnumProperty.create("part", GiantFireworkPart.class);
    /**
     * 水平朝向。
     *
     * <p>用原版 {@code FACING}（南北东西四向）而非自建属性，好处是直接复用原版
     * blockstate 的 {@code y} 旋转约定（north=0 / east=90 / south=180 / west=270）。
     */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    
    /**
     * 整支火箭的碰撞轮廓，由三段模型的 elements 逐盒平移而来（1/16 单位，以主部件角为原点）。
     *
     * <p>手写近似会与模型脱节——底部中段是<b>空腔</b>，早先的版本把它当成实心而把底部堵死了。
     * 改动模型后必须同步这里，否则碰撞箱会与外观不符。
     */
    private static final AABB[] GLOBAL_SHAPE = {
        new AABB(11.0, 0.0, 11.0, 23.0, 32.0, 23.0),       // bottom el1：底部立柱（Y0~2 仅此一柱）
        new AABB(-8.0, 32.0, -8.0, 24.0, 48.0, 24.0),      // bottom el2：箭体下段
        new AABB(1.0, 25.0, 5.0, 3.0, 32.0, 7.0),          // bottom el3：侧壁小凸起
        new AABB(0.0, 18.0, 4.0, 2.0, 25.0, 6.0),          // bottom el4：侧壁小凸起
        new AABB(-8.0, 48.0, -8.0, 24.0, 96.0, 24.0),      // middle el1：箭体中段
        new AABB(-16.0, 112.0, -16.0, 32.0, 118.0, 32.0),  // top el1：尾翼环（全箭最宽，3 格）
        new AABB(-12.0, 118.0, -12.0, 28.0, 125.0, 28.0),  // top el2
        new AABB(-8.0, 125.0, -8.0, 24.0, 133.0, 24.0),    // top el3：收窄
        new AABB(-4.0, 133.0, -4.0, 20.0, 142.0, 20.0),    // top el4：箭尖
        new AABB(-8.0, 96.0, -8.0, 24.0, 112.0, 24.0),     // top el5：接中段
    };

    /**
     * 每个朝向下的部件形状表。
     *
     * <p>朝向会改变部件的世界坐标位置（{@code offsetFrom}），所以裁剪基准也跟着转；
     * 这里预先按 4 个朝向各算一份，避免每次查询都重算。
     */
    private static final Map<Direction, Map<GiantFireworkPart, VoxelShape>> SHAPES_BY_FACING =
        createShapesByFacing();

    public GiantFireworkBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(PART, GiantFireworkPart.Y0_C)
            .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, FACING);
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
     * 玩家放置时按水平视线决定朝向（与原版熔炉/活塞一致）。
     *
     * <p>注意不能覆写 {@code getStateForPlacement}——AnvilCraft 把它声明为 {@code final}，
     * 它只负责检查空间并转发到这里。
     */
    @Override
    public BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        if (state == null) {
            return null;
        }
        return state.setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** 主部件是底层中心，所以结构相对主部件只往 Y 正方向延伸。 */
    @Override
    public Vec3i getMainPartOffset() {
        return Vec3i.ZERO;
    }

    /**
     * 单个部件贴合模型的碰撞形状。同时供 {@code getCollisionShape}（实体碰撞）与
     * {@code getOcclusionShape}（遮挡/光照）使用，并由 {@link AbstractMultiPartBlock#getMultiPartShape}
     * 按部件偏移合并成整支火箭的形状。
     */
    @Override
    public VoxelShape getPartShape(BlockState state) {
        if (!state.hasProperty(PART) || !state.hasProperty(FACING)) {
            return Shapes.block();
        }
        return SHAPES_BY_FACING
            .getOrDefault(state.getValue(FACING), Map.of())
            .getOrDefault(state.getValue(PART), Shapes.block());
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
     * 交互分流：铁砧锤 {@code PASS}（让 {@link IHammerRemovable} 拆除生效）、
     * 烟火之星装填、打火石/火焰弹点火、其它物品 {@code PASS}。
     *
     * <p>点火必须用打火石或火焰弹，和 TNT 一致——空手右键不再发射，
     * 这样靠着火箭垫方块或误触时不会把整支烟花射出去。
     *
     * <p>铁砧锤必须让路：方块交互在物品 {@code useOn} 之前执行
     * （见 {@code ServerPlayerGameMode#useItemOn}），这里的 {@code CONSUME}
     * 会抢在锤子拆除逻辑前，导致拿锤子右键反而把它射出去。
     *
     * <p>AnvilCraft 的 {@code SimpleMultiPartBlock} 把 {@code useItemOn} 与
     * {@code useWithoutItem} 都转发到本方法（见其字节码），所以空手与持物都会走到这里。
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
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof AnvilHammerItem) {
            return InteractionResult.PASS;
        }
        if (held.is(Items.FIREWORK_STAR)) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            return level instanceof ServerLevel serverLevel
                ? this.installStar(serverLevel, pos, state, player, held)
                : InteractionResult.PASS;
        }
        if (!held.is(Items.FLINT_AND_STEEL) && !held.is(Items.FIRE_CHARGE)) {
            return InteractionResult.PASS;
        }
        return this.ignite(state, level, pos, player, hand, held);
    }

    /**
     * 打火石 / 火焰弹点火发射，并像 TNT 那样消耗点火物。
     *
     * <p>打火石掉 1 点耐久、火焰弹消耗 1 个；创造模式两者都不消耗。
     */
    private InteractionResult ignite(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        ItemStack held
    ) {
        if (level.isClientSide()) {
            // 客户端提前返回成功，避免手臂不摆动；真正的逻辑在服务端。
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        BlockPos mainPos = this.getMainPartPos(pos, state);
        // 必须在拆结构之前读取：拆掉主部件会连带移除方块实体。
        List<FireworkExplosion> explosions = level.getBlockEntity(mainPos)
            instanceof GiantFireworkBlockEntity fireworks
            ? fireworks.getExplosions()
            : List.of();
        this.removePartsAndUpdate(level, pos);
        GiantFireworkEntity.launch(serverLevel, mainPos, state.getValue(FACING), player, explosions);
        // 与 TNT 一致：创造模式不消耗
        if (!player.getAbilities().instabuild) {
            if (held.is(Items.FLINT_AND_STEEL)) {
                held.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            } else {
                held.consume(1, player);
            }
        }
        player.awardStat(Stats.ITEM_USED.get(held.getItem()));
        return InteractionResult.CONSUME;
    }

    /** 装填一个烟火之星，只取出其中的 {@link FireworkExplosion} 存进方块实体。 */
    private InteractionResult installStar(
        ServerLevel level,
        BlockPos pos,
        BlockState state,
        Player player,
        ItemStack stack
    ) {
        BlockPos mainPos = this.getMainPartPos(pos, state);
        if (!(level.getBlockEntity(mainPos) instanceof GiantFireworkBlockEntity fireworks)) {
            return InteractionResult.PASS;
        }
        if (fireworks.isFull()) {
            player.displayClientMessage(
                Component.translatable(
                    "message.anvilcraft_pigeon_plus.giant_firework.full",
                    GiantFireworkBlockEntity.MAX_STARS
                ),
                true
            );
            return InteractionResult.FAIL;
        }
        FireworkExplosion explosion = stack.get(DataComponents.FIREWORK_EXPLOSION);
        if (explosion == null) {
            explosion = FireworkExplosion.DEFAULT;
        }
        if (!fireworks.addExplosion(explosion)) {
            return InteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(
            null, mainPos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS,
            1.0F, 1.0F
        );
        player.displayClientMessage(
            Component.translatable(
                "message.anvilcraft_pigeon_plus.giant_firework.installed",
                fireworks.getStarCount(),
                GiantFireworkBlockEntity.MAX_STARS
            ),
            true
        );
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // 整个多方块一体，只在主部件（底层中心）挂方块实体。
        return this.isMainPart(state) ? new GiantFireworkBlockEntity(pos, state) : null;
    }

    /**
     * 拆除时把已装填的烟火之星还回来。
     *
     * <p>战利品表只为主部件产出方块本体、方块实体也只在主部件上，所以只在正确拆除主部件时生效；
     * 拆其它部件会由 {@code AbstractMultiPartBlock#updateShape} 连带清掉整座结构，装填物仍会丢失——
     * 与巨型铁砧等原版多方块一致。
     */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
            instanceof GiantFireworkBlockEntity fireworks) {
            for (FireworkExplosion explosion : fireworks.getExplosions()) {
                ItemStack star = new ItemStack(Items.FIREWORK_STAR);
                star.set(DataComponents.FIREWORK_EXPLOSION, explosion);
                drops.add(star);
            }
        }
        return drops;
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
        return state.setValue(PART, state.getValue(PART).rotate(rotation))
            .setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(PART, state.getValue(PART).mirror(mirror))
            .setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    private static Map<Direction, Map<GiantFireworkPart, VoxelShape>> createShapesByFacing() {
        Map<Direction, Map<GiantFireworkPart, VoxelShape>> byFacing = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            Map<GiantFireworkPart, VoxelShape> shapes = new EnumMap<>(GiantFireworkPart.class);
            for (GiantFireworkPart part : GiantFireworkPart.values()) {
                shapes.put(part, slicePartShape(part, facing));
            }
            byFacing.put(facing, shapes);
        }
        return byFacing;
    }

    /**
     * 把 {@link #GLOBAL_SHAPE} 先按朝向旋转，再裁剪到 {@code part} 所在的那一格，
     * 并平移回该方块的局部坐标。
     *
     * <p>旋转的是<b>几何本身</b>而不是「取哪一格」——该形状并不左右对称
     * （侧壁两个小凸起只在一侧），只旋转格位会让碰撞箱与旋转后的模型错位。
     * 旋转中心取 3×3 的中点（即 8/16 格处），与模型绕 Y 轴自转的中心一致。
     */
    private static VoxelShape slicePartShape(GiantFireworkPart part, Direction facing) {
        double minX = part.getOffsetX() * 16.0;
        double minY = part.getOffsetY() * 16.0;
        double minZ = part.getOffsetZ() * 16.0;
        double maxX = minX + 16.0;
        double maxY = minY + 16.0;
        double maxZ = minZ + 16.0;
        List<AABB> localParts = new ArrayList<>();
        for (AABB source : GLOBAL_SHAPE) {
            AABB globalPart = rotateShape(source, facing);
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

    /**
     * 绕 3×3 中点（X/Z = 8）把形状旋转到该朝向，Y 不变。
     *
     * <p>旋转方向以原版熔炉为准：{@code facing=east} 对应 blockstate 的 {@code y=90}，
     * 把模型的北面（局部 -Z）转到世界 +X。由此得出
     * {@code east: (x,z) -> (16-z, x)}、{@code south: (x,z) -> (16-x, 16-z)}、
     * {@code west: (x,z) -> (z, 16-x)}。
     */
    private static AABB rotateShape(AABB box, Direction facing) {
        return switch (facing) {
            case EAST -> new AABB(16.0 - box.maxZ, box.minY, box.minX,
                                  16.0 - box.minZ, box.maxY, box.maxX);
            case SOUTH -> new AABB(16.0 - box.maxX, box.minY, 16.0 - box.maxZ,
                                   16.0 - box.minX, box.maxY, 16.0 - box.minZ);
            case WEST -> new AABB(box.minZ, box.minY, 16.0 - box.maxX,
                                  box.maxZ, box.maxY, 16.0 - box.minX);
            default -> box;
        };
    }
}
