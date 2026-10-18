package dev.anvilcraft.pigeonplus.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.dubhe.anvilcraft.integration.jei.category.multiblock.MultiBlockCraftingCategory;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MultiBlockCraftingCategory.class)
public class MultiBlockCraftingCategoryMixin {
    @ModifyExpressionValue(
        method = "draw",
        at = @At(
            value = "INVOKE",
            target = "Ldev/dubhe/anvilcraft/recipe/multiblock/MultiblockUtil;size"
                   + "(Ldev/anvilcraft/lib/v2/multiblock/dynamic/definition/MultiblockDefinition;)I"
        )
    )
    private int pigeonplus$useHorizontalSizeForFireworks(
        int original,
        @Local(argsOnly = true) RecipeHolder<MultiblockRecipe> recipe
    ) {
        if (recipe.value().getResult().is(AddonBlocks.LARGE_FIREWORKS_ROCKET.asItem())) {
            return 3;
        }
        return original;
    }
}
