package dev.anvilcraft.pigeonplus.block.entity;

import dev.anvilcraft.pigeonplus.block.BlenderBlock;
import dev.anvilcraft.pigeonplus.block.ElectricBlenderBlock;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * 电搅拌器的方块实体：接入 AnvilCraft 电网，耗电 {@link #POWER} kW。
 *
 * <p>只要方块实体实现了 {@link IPowerConsumer}，AnvilCraft 的
 * {@code ServerBlockEntityEventListener} 就会在区块加载 / 卸载时自动把它登记进
 * {@link PowerGrid}，无需任何手动注册。
 *
 * <p>电网的规则是「发电功率 ≥ 用电功率则全部用电器正常工作」，所以这里只需声明
 * 需求功率，过载判定交给 {@link #flushState} 维护的 {@code OVERLOAD} 状态。
 */
public class ElectricBlenderBlockEntity extends BlockEntity implements IPowerConsumer {
    /** 耗电功率，单位 kW。 */
    public static final int POWER = 32;

    private PowerGrid grid;

    public ElectricBlenderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public ElectricBlenderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTRIC_BLENDER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ElectricBlenderBlockEntity blockEntity) {
        // 每 tick 刷新过载标记：电网供电不足时 OVERLOAD 置位。
        blockEntity.flushState(level, pos);

        boolean gridWorking = blockEntity.isGridWorking();

        // 加热与搅拌头无关：只要通电就供热，这样它能单独当作电加热器使用。
        // AddonHeaterInfos 以 HEATING 为供热条件，状态一变供热者名单会在下次扫描时同步。
        blockEntity.setHeating(gridWorking);

        // 搅拌才需要搅拌头。
        blockEntity.setWorking(blockEntity.hasHead() && gridWorking);
    }

    /** 是否已安装搅拌头。没装头时不耗电，也不会运转。 */
    public boolean hasHead() {
        BlockState state = this.getBlockState();
        return state.hasProperty(BlenderBlock.HEAD)
            && state.getValue(BlenderBlock.HEAD) != BlenderBlock.BlenderHead.NONE;
    }

    /**
     * 本设备的用电需求（kW）。恒为 {@link #POWER}——只要通电它就供热，
     * 所以哪怕没装搅拌头、没在搅拌，也一样耗电。
     */
    @Override
    public int getInputPower() {
        return POWER;
    }

    @Override
    public Level getCurrentLevel() {
        return Objects.requireNonNull(this.getLevel());
    }

    @Override
    public BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public void setGrid(@Nullable PowerGrid grid) {
        this.grid = grid;
    }

    @Nullable
    @Override
    public PowerGrid getGrid() {
        return this.grid;
    }

    private void setWorking(boolean working) {
        this.setProperty(BlenderBlock.WORKING, working);
    }

    private void setHeating(boolean heating) {
        this.setProperty(ElectricBlenderBlock.HEATING, heating);
    }

    /**
     * 设置一个布尔状态，并只在实际变化时发包，避免每 tick 都触发方块更新。
     */
    private void setProperty(BooleanProperty property, boolean value) {
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        BlockState state = this.getBlockState();
        if (state.hasProperty(property) && state.getValue(property) != value) {
            this.level.setBlock(this.worldPosition, state.setValue(property, value), Block.UPDATE_CLIENTS);
        }
    }
}
