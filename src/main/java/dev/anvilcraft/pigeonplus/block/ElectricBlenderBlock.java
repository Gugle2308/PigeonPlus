package dev.anvilcraft.pigeonplus.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.pigeonplus.block.entity.ElectricBlenderBlockEntity;
import dev.anvilcraft.pigeonplus.block.entity.ModBlockEntities;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

/**
 * 电搅拌机：与 {@link BlenderBlock} 唯一区别是使用电力而非压缩空气驱动。
 *
 * <p>安装 / 拆卸搅拌头、掉落返还等交互与外观逻辑全部继承自 {@link BlenderBlock}，
 * 只是改为消耗 {@link ElectricBlenderBlockEntity#POWER} kW 电力。
 *
 * <p>它同时还是一台<b>电加热器</b>：只要通电（电网未过载）就向上方供热，
 * 与是否装有搅拌头无关。该行为由下面的 {@link #HEATING} 状态表达。
 */
public class ElectricBlenderBlock extends BlenderBlock {
    public static final MapCodec<ElectricBlenderBlock> CODEC = simpleCodec(ElectricBlenderBlock::new);
    /** 电网过载标记，由 {@code IPowerComponent#flushState} 维护。 */
    public static final BooleanProperty OVERLOAD = IPowerComponent.OVERLOAD;
    /**
     * 是否正在供热。通电（电网未过载）即为 {@code true}，无需玩家手动开关，
     * 与搅拌头无关；{@link dev.anvilcraft.pigeonplus.init.AddonHeaterInfos} 以本属性为供热条件。
     */
    public static final BooleanProperty HEATING = BooleanProperty.create("heating");

    public ElectricBlenderBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(WORKING, false)
            .setValue(HEAD, BlenderHead.NONE)
            .setValue(OVERLOAD, true)
            .setValue(HEATING, false));
    }

    @Override
    protected MapCodec<? extends BlenderBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OVERLOAD, HEATING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        // 刚放下时尚未接入电网，供热状态交给第一个 serverTick 判定。
        return state == null ? null : state.setValue(OVERLOAD, true).setValue(HEATING, false);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectricBlenderBlockEntity(pos, state);
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
            ModBlockEntities.ELECTRIC_BLENDER.get(),
            ElectricBlenderBlockEntity::serverTick
        );
    }
}
