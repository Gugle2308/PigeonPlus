package dev.anvilcraft.pigeonplus.fluid;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.fluids.FluidStack;

public final class PotionFluidUtil {
    private PotionFluidUtil() {
    }

    public static boolean isPotion(FluidStack stack) {
        return stack.getFluid() instanceof PotionFluid
            && stack.get(DataComponents.POTION_CONTENTS) != null;
    }

    public static PotionContents getContents(FluidStack stack) {
        return stack.getFluid() instanceof PotionFluid
            ? stack.get(DataComponents.POTION_CONTENTS)
            : null;
    }

    public static FluidStack withContents(FluidStack stack, PotionContents contents) {
        stack.set(DataComponents.POTION_CONTENTS, contents);
        return stack;
    }
}
