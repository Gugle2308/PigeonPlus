package dev.anvilcraft.pigeonplus.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.integration.jei.category.multiblock.MultiBlockConversionCategory;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockConversionRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MultiBlockConversionCategory.class)
public class MultiBlockConversionCategoryMixin {
    private static final String FIREWORKS_RECIPE_PATH = "large_fireworks_rocket";

    @ModifyExpressionValue(
        method = "draw",
        at = @At(
            value = "INVOKE",
            target = "Ldev/dubhe/anvilcraft/recipe/multiblock/MultiblockConversionRecipe;getSize()I"
        )
    )
    private int pigeonplus$useHorizontalSizeForFireworks(
        int original,
        @Local(argsOnly = true) RecipeHolder<MultiblockConversionRecipe> recipe
    ) {
        if (recipe.id().getPath().endsWith(FIREWORKS_RECIPE_PATH)) {
            return 3;
        }
        return original;
    }
}
