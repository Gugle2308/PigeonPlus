package dev.anvilcraft.pigeonplus.block;

import dev.dubhe.anvilcraft.block.CookiePillarBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.HitResult;

public class FilledCookiePillarBlock extends CookiePillarBlock {
    private static final int FLOWING_LEVEL = 5;

    private final FlowingFluid fluid;

    public FilledCookiePillarBlock(Properties properties, FlowingFluid fluid) {
        super(properties);
        this.fluid = fluid;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return this.fluid.getSource(false);
    }

    @Override
    public ItemStack getCloneItemStack(
        BlockState state,
        HitResult target,
        LevelReader level,
        BlockPos pos,
        Player player
    ) {
        return new ItemStack(ModBlocks.COOKIE_PILLAR.get());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        this.leak(level, pos, state);
    }

    @Override
    protected void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        BlockPos neighborPos,
        boolean movedByPiston
    ) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        this.leak(level, pos, state);
    }

    private void leak(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }
        for (Direction direction : axisEnds(state.getValue(AXIS))) {
            BlockPos target = pos.relative(direction);
            BlockState targetState = level.getBlockState(target);
            if (!targetState.canBeReplaced()) {
                continue;
            }
            if (targetState.getFluidState().getType().isSame(this.fluid)) {
                continue;
            }
            BlockState flowing = this.fluid.getFlowing(FLOWING_LEVEL, false).createLegacyBlock();
            if (flowing.isAir()) {
                continue;
            }
            level.setBlockAndUpdate(target, flowing);
            return;
        }
    }

    private static Direction[] axisEnds(Direction.Axis axis) {
        return switch (axis) {
            case X -> new Direction[] {Direction.WEST, Direction.EAST};
            case Z -> new Direction[] {Direction.NORTH, Direction.SOUTH};
            default -> new Direction[] {Direction.DOWN};
        };
    }
}
