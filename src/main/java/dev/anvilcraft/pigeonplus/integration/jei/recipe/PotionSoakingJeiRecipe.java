package dev.anvilcraft.pigeonplus.integration.jei.recipe;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/**
 * 泡药：把物品浸泡在药水里，注入对应的药水效果。
 */
public record PotionSoakingJeiRecipe(
    ItemStack input,
    List<FluidStack> potions,
    List<ItemStack> results
) {
}
