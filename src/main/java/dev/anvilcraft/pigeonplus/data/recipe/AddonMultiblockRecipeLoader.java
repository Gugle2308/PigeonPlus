package dev.anvilcraft.pigeonplus.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockBuilder;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockRecipe;

import java.util.Map;

/**
 * 大型烟花的「多方块合成」配方，用于 JEI 展示。
 */
public class AddonMultiblockRecipeLoader {
    private AddonMultiblockRecipeLoader() {
    }

    public static void init(RegistrumRecipeProvider provider) {
        AddonMultiblockRecipeLoader.largeFireworksRocket(provider);
        AddonMultiblockRecipeLoader.largeCreamyBreadRoll(provider);
    }

    private static void largeFireworksRocket(RegistrumRecipeProvider provider) {
        MultiblockBuilder builder = MultiblockRecipe.builder(AddonBlocks.LARGE_FIREWORKS_ROCKET, 1);
        for (String[] layer : LargeFireworksRocketDisplayRecipe.INPUT_GRID) {
            builder.layer(layer);
        }
        for (Map.Entry<Character, BlockStatePredicate.Builder> entry
            : LargeFireworksRocketDisplayRecipe.INPUT_MAPPING.entrySet()) {
            builder.symbol(entry.getKey(), entry.getValue());
        }
        builder.save(provider, AnvilCraftPigeonPlus.of("multiblock/large_fireworks_rocket"));
    }

    private static void largeCreamyBreadRoll(RegistrumRecipeProvider provider) {
        MultiblockBuilder builder = MultiblockRecipe.builder(AddonBlocks.LARGE_CREAMY_BREAD_ROLL, 3);
        for (String[] layer : LargeCreamyBreadRollDisplayRecipe.INPUT_GRID) {
            builder.layer(layer);
        }
        for (Map.Entry<Character, BlockStatePredicate.Builder> entry
            : LargeCreamyBreadRollDisplayRecipe.INPUT_MAPPING.entrySet()) {
            builder.symbol(entry.getKey(), entry.getValue());
        }
        builder.save(provider, AnvilCraftPigeonPlus.of("multiblock/large_creamy_bread_roll"));
    }
}
