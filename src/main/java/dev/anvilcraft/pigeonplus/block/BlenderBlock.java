package dev.anvilcraft.pigeonplus.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.pigeonplus.block.entity.BlenderBlockEntity;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.anvilcraft.pigeonplus.block.entity.ModBlockEntities;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BlenderBlock extends BaseEntityBlock implements IHammerRemovable {
    public static final MapCodec<BlenderBlock> CODEC = simpleCodec(BlenderBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty WORKING = BooleanProperty.create("working");
    /** 已安装的搅拌头。新放置的搅拌机没有搅拌头（{@link BlenderHead#NONE}），需玩家手动安装。 */
    public static final EnumProperty<BlenderHead> HEAD = EnumProperty.create("head", BlenderHead.class);

    /**
     * 搅拌头类型，每个变体对应一个独立的头部模型（由 {@code BlenderBlockEntityRenderer} 叠加渲染）。
     *
     * <p>{@link #NONE} 时搅拌机不能运转。
     */
    public enum BlenderHead implements StringRepresentable {
        /** 未安装搅拌头。 */
        NONE(null),
        /** 铁锹安装的普通十字搅拌头。 */
        IRON(Items.IRON_SHOVEL),
        /** 烈焰棒安装的烈焰搅拌头。 */
        BLAZE(Items.BLAZE_ROD);

        @Nullable
        private final Item item;

        BlenderHead(@Nullable Item item) {
            this.item = item;
        }

        /** 安装本头所需、也是拆下时返还的物品；{@link #NONE} 返回空。 */
        public ItemStack asStack() {
            return this.item == null ? ItemStack.EMPTY : new ItemStack(this.item);
        }

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    public BlenderBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(WORKING, false)
            .setValue(HEAD, BlenderHead.NONE));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WORKING, HEAD);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getOpposite())
            .setValue(WORKING, false)
            .setValue(HEAD, BlenderHead.NONE);
    }

    /**
     * 交互分流：铁锹 / 烈焰棒安装搅拌头，潜行空手拆下搅拌头。
     *
     * <p>铁砧锤必须让路返回 {@code PASS}：方块交互先于物品 {@code useOn} 执行
     * （见 {@code ServerPlayerGameMode#useItemOn}），否则会抢在 {@link IHammerRemovable} 的拆除逻辑之前。
     */
    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        if (stack.getItem() instanceof AnvilHammerItem) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        BlenderHead installed = state.getValue(HEAD);

        // 潜行空手：拆下已安装的搅拌头
        if (stack.isEmpty() && player.isShiftKeyDown()) {
            if (installed == BlenderHead.NONE) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(HEAD, BlenderHead.NONE), Block.UPDATE_ALL);
                ItemStack removed = installed.asStack();
                if (!player.addItem(removed)) {
                    player.drop(removed, false);
                }
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        // 已装头则不再响应安装
        if (installed != BlenderHead.NONE) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        BlenderHead target = stack.is(Items.IRON_SHOVEL)
            ? BlenderHead.IRON
            : stack.is(Items.BLAZE_ROD) ? BlenderHead.BLAZE : null;
        if (target == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(HEAD, target), Block.UPDATE_ALL);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.7F, 1.2F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * 除战利品表给出的搅拌机本体外，额外掉落已安装的搅拌头。
     *
     * <p>用 {@code getDrops} 而不是 {@code playerDestroy}，这样被爆炸、活塞等
     * 非玩家方式破坏时同样会返还搅拌头。
     */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
        ItemStack head = state.getValue(HEAD).asStack();
        if (!head.isEmpty()) {
            drops.add(head);
        }
        return drops;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(WORKING)) {
            return;
        }

        Direction inputSide = state.getValue(FACING).getOpposite();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (direction == inputSide || random.nextFloat() > 0.65f) {
                continue;
            }

            double x = pos.getX() + 0.5 + direction.getStepX() * 0.56 + (random.nextDouble() - 0.5) * 0.22;
            double y = pos.getY() + 0.45 + random.nextDouble() * 0.35;
            double z = pos.getZ() + 0.5 + direction.getStepZ() * 0.56 + (random.nextDouble() - 0.5) * 0.22;
            double xSpeed = direction.getStepX() * 0.045 + (random.nextDouble() - 0.5) * 0.012;
            double ySpeed = 0.006 + random.nextDouble() * 0.01;
            double zSpeed = direction.getStepZ() * 0.045 + (random.nextDouble() - 0.5) * 0.012;

            level.addParticle(ParticleTypes.CLOUD, x, y, z, xSpeed, ySpeed, zSpeed);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlenderBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level,
        BlockState state,
        BlockEntityType<T> blockEntityType
    ) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(
            blockEntityType,
            ModBlockEntities.BLENDER.get(),
            BlenderBlockEntity::serverTick
        );
    }
}
