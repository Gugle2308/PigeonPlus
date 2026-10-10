package dev.anvilcraft.pigeonplus.util;

import dev.dubhe.anvilcraft.api.fluid.LargeCauldronFluidHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * 药水容器的液面几何。
 */
public final class PotionContainerGeometry {
    // 取自 LargeCauldronBlockEntityRenderer。
    public static final float CAULDRON_WALL = 0.25F + 0.001F;
    public static final float CAULDRON_MIN_XZ = -1.0F + CAULDRON_WALL;
    public static final float CAULDRON_MAX_XZ = 2.0F - CAULDRON_WALL;
    private static final float CAULDRON_MIN_Y = -0.5F + 0.001F;
    private static final float CAULDRON_MAX_Y = 1.75F - 0.001F;

    // 取自 FishTankBlockEntityRenderer。
    public static final float TANK_WALL = 1 / 16F + 0.001F;
    public static final float TANK_MIN_XZ = TANK_WALL;
    public static final float TANK_WIDTH = 1 - 2 * TANK_WALL;

    private PotionContainerGeometry() {
    }

    public static double cauldronSurfaceY(BlockPos mainPos, LargeCauldronFluidHandler fluids) {
        int capacity = LargeCauldronFluidHandler.TANK_COUNT * LargeCauldronFluidHandler.tankCapacity();
        return mainPos.getY() + CAULDRON_MIN_Y
            + (CAULDRON_MAX_Y - CAULDRON_MIN_Y) * fill(fluids.getTotalAmount(), capacity);
    }

    public static double fishTankSurfaceY(BlockPos pos, FluidTank tank) {
        return pos.getY() + TANK_WALL + TANK_WIDTH * fill(tank.getFluidAmount(), tank.getCapacity());
    }

    private static float fill(int amount, int capacity) {
        if (capacity <= 0) {
            return 0.0F;
        }
        return Mth.clamp((float) amount / capacity, 0.0F, 1.0F);
    }
}
