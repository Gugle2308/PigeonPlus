package dev.anvilcraft.pigeonplus.fluid;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PotionFluidUtil {
    private PotionFluidUtil() {
    }

    public static boolean isPotion(FluidStack stack) {
        return stack.getFluid() instanceof PotionFluid
            && stack.get(DataComponents.POTION_CONTENTS) != null;
    }

    @Nullable
    public static PotionContents getContents(FluidStack stack) {
        return stack.getFluid() instanceof PotionFluid
            ? stack.get(DataComponents.POTION_CONTENTS)
            : null;
    }

    public static FluidStack withContents(FluidStack stack, PotionContents contents) {
        stack.set(DataComponents.POTION_CONTENTS, contents);
        return stack;
    }

    // 锅内可能同时存在多种药水，全部取出：粒子要放出所有颜色，生物也要获得所有效果。
    // 按药水去重——同一种药水占着多个储罐时不该让粒子翻倍、效果重复。
    // PotionContents 是 record，equals 逐字段比较，可以安全地当去重键。
    // 返回 FluidStack 而非 PotionContents，因为抽取时要靠组件精确匹配到那一层。
    public static List<FluidStack> allPotions(@Nullable IFluidHandler fluids) {
        if (fluids == null) {
            return List.of();
        }
        Map<PotionContents, FluidStack> found = new LinkedHashMap<>();
        for (int tank = 0; tank < fluids.getTanks(); tank++) {
            FluidStack stack = fluids.getFluidInTank(tank);
            if (stack.isEmpty()) {
                continue;
            }
            PotionContents contents = getContents(stack);
            if (contents != null) {
                found.putIfAbsent(contents, stack.copy());
            }
        }
        return List.copyOf(found.values());
    }
}
