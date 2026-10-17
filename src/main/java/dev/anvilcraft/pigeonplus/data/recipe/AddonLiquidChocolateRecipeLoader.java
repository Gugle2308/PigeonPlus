package dev.anvilcraft.pigeonplus.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.init.AddonFluids;
import dev.anvilcraft.pigeonplus.recipe.anvil.wrap.BlendingRecipe;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SolidLiquidRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SuperHeatingRecipe;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;

public class AddonLiquidChocolateRecipeLoader {
    private static final int PRODUCE = 1000;

    public static void init(RegistrumRecipeProvider provider) {
        melt(provider, "liquid_chocolate", ModBlocks.CHOCOLATE_BLOCK.get(), AddonFluids.LIQUID_CHOCOLATE.get());
        melt(
            provider,
            "liquid_chocolate_dark",
            ModBlocks.BLACK_CHOCOLATE_BLOCK.get(),
            AddonFluids.LIQUID_CHOCOLATE_DARK.get()
        );
        melt(
            provider,
            "liquid_chocolate_white",
            ModBlocks.WHITE_CHOCOLATE_BLOCK.get(),
            AddonFluids.LIQUID_CHOCOLATE_WHITE.get()
        );

        BlendingRecipe.builder()
            .requires(ModFoodItems.COCOA_LIQUOR, 8)
            .requires(ModFoodItems.COCOA_BUTTER, 4)
            .requires(ModFoodItems.CREAM,4)
            .requires(Items.SUGAR, 2)
            .transform(AnvilCraftPigeonPlus.of("liquid_chocolate"), PRODUCE)
            .requiresHeat()
            .save(provider, AnvilCraftPigeonPlus.of("blending/liquid_chocolate"));
        BlendingRecipe.builder()
            .requires(ModFoodItems.COCOA_LIQUOR, 12)
            .requires(ModFoodItems.COCOA_BUTTER, 4)
            .requires(Items.SUGAR ,2)
            .transform(AnvilCraftPigeonPlus.of("liquid_chocolate_dark"), PRODUCE)
            .requiresHeat()
            .save(provider, AnvilCraftPigeonPlus.of("blending/liquid_chocolate_dark"));
        BlendingRecipe.builder()
            .requires(ModFoodItems.COCOA_BUTTER, 12)
            .requires(ModFoodItems.CREAM, 4)
            .requires(Items.SUGAR, 2)
            .transform(AnvilCraftPigeonPlus.of("liquid_chocolate_white"), PRODUCE)
            .requiresHeat()
            .save(provider, AnvilCraftPigeonPlus.of("blending/liquid_chocolate_white"));

        solidify(provider, "liquid_chocolate", ModBlocks.CHOCOLATE_BLOCK.get(), AddonFluids.LIQUID_CHOCOLATE.get());
        solidify(
            provider,
            "liquid_chocolate_dark",
            ModBlocks.BLACK_CHOCOLATE_BLOCK.get(),
            AddonFluids.LIQUID_CHOCOLATE_DARK.get()
        );
        solidify(
            provider,
            "liquid_chocolate_white",
            ModBlocks.WHITE_CHOCOLATE_BLOCK.get(),
            AddonFluids.LIQUID_CHOCOLATE_WHITE.get()
        );
    }

    private static void melt(RegistrumRecipeProvider provider, String path, ItemLike block, Fluid fluid) {
        SuperHeatingRecipe.builder()
            .requires(block)
            .transform(fluid, PRODUCE)
            .save(provider, AnvilCraftPigeonPlus.of("super_heating/" + path));
    }

    private static void solidify(RegistrumRecipeProvider provider, String path, ItemLike block, Fluid fluid) {
        SolidLiquidRecipe.builder()
            .cauldron(fluid)
            .consume(PRODUCE)
            .result(block, 1)
            .save(provider, AnvilCraftPigeonPlus.of("solid_liquid/" + path));
    }
}
