package dev.anvilcraft.pigeonplus.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockConversionRecipe;

import java.util.Map;

/**
 * 大型烟花的「多方块转化」配方，用于 JEI 展示。
 */
public class AddonMultiblockConversionRecipeLoader {
    private AddonMultiblockConversionRecipeLoader() {
    }

    public static void init(RegistrumRecipeProvider provider) {
        MultiblockConversionRecipe.Builder builder = MultiblockConversionRecipe.builder();
        for (String[] layer : LargeFireworksRocketDisplayRecipe.INPUT_GRID) {
            builder.inputLayer(layer);
        }
        for (String[] layer : LargeFireworksRocketDisplayRecipe.OUTPUT_GRID) {
            builder.outputLayer(layer);
        }
        // 输入与输出是两个独立的符号命名空间：同一个字母可在两侧各指不同方块，故必须分别注册。
        for (Map.Entry<Character, BlockStatePredicate.Builder> entry
            : LargeFireworksRocketDisplayRecipe.INPUT_MAPPING.entrySet()) {
            builder.inputSymbol(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<Character, BlockStatePredicate.Builder> entry
            : LargeFireworksRocketDisplayRecipe.OUTPUT_MAPPING.entrySet()) {
            builder.outputSymbol(entry.getKey(), entry.getValue());
        }
        builder.save(provider, AnvilCraftPigeonPlus.of("multiblock_conversion/large_fireworks_rocket"));
    }
}
