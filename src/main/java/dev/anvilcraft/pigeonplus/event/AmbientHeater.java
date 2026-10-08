package dev.anvilcraft.pigeonplus.event;

import dev.dubhe.anvilcraft.block.BurningHeaterBlock;
import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class AmbientHeater {
    private static final int MAX_STRUCTURE_HEIGHT = 16;

    private AmbientHeater() {
    }

    // 与本体炼药锅的既有判定保持一致：电加热器不超载，或燃烧加热器达到 2 档。
    public static boolean isActive(BlockState state) {
        if (state.is(ModBlocks.HEATER)) {
            return !state.getValue(HeaterBlock.OVERLOAD);
        }
        return state.is(ModBlocks.BURNING_HEATER) && state.getValue(BurningHeaterBlock.LEVEL) == 2;
    }

    public static boolean hasActiveHeaterBelow(Level level, BlockPos structurePos) {
        return isActive(level.getBlockState(structurePos.below()));
    }

    public static boolean hasActiveHeaterArrayBelow(Level level, BlockPos mainPos, BlockState structureBlock) {
        BlockPos bottom = mainPos;
        for (int guard = 0; guard < MAX_STRUCTURE_HEIGHT; guard++) {
            BlockState below = level.getBlockState(bottom.below());
            if (!below.is(structureBlock.getBlock())) {
                break;
            }
            bottom = bottom.below();
        }
        BlockPos center = bottom.below();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
            if (isActive(level.getBlockState(pos))) {
                return true;
            }
        }
        return false;
    }
}
