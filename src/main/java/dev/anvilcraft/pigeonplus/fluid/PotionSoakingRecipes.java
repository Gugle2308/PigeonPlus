package dev.anvilcraft.pigeonplus.fluid;

import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 泡药配方的唯一真相源。
 */
public final class PotionSoakingRecipes {
    // 250mB，正好是一瓶药水的量（见 PotionBottleFluidHandler.FLUID_PER_BOTTLE）。
    public static final int FLUID_COST = 250;

    private static final int PILL_COUNT = 2;
    private static final int ARROW_COUNT = 8;

    private PotionSoakingRecipes() {
    }

    public static List<Soakable> all() {
        return SOAKABLE;
    }

    private static final List<Soakable> SOAKABLE = List.of(
        new Soakable(
            () -> new ItemStack(ModFoodItems.PILL.get(), PILL_COUNT),
            PotionSoakingRecipes::pillWith
        ),
        new Soakable(
            () -> new ItemStack(Items.ARROW, ARROW_COUNT),
            PotionSoakingRecipes::tippedArrowWith
        )
    );

    // 可浸泡的物品：空白态 + 注入效果后的形态。
    // 数量不变，只是被注入效果，所以消耗与产出同样多。
    public record Soakable(
        Supplier<ItemStack> blankSupplier,
        Function<PotionContents, ItemStack> soaker
    ) {
        public ItemStack blank() {
            return this.blankSupplier.get();
        }

        public ItemStack soakedWith(PotionContents contents) {
            return this.soaker.apply(contents);
        }

        // 一份配方要消耗多少个该物品。
        public int count() {
            return this.blank().getCount();
        }

        public boolean matches(ItemStack stack) {
            return stack.is(this.blank().getItem());
        }
    }

    private static ItemStack pillWith(PotionContents contents) {
        ItemStack pills = new ItemStack(ModFoodItems.PILL.get(), PILL_COUNT);
        pills.set(DataComponents.POTION_CONTENTS, contents);
        return pills;
    }

    // 药水箭就是原版 TippedArrowItem：同样靠 POTION_CONTENTS 组件携带药水。
    private static ItemStack tippedArrowWith(PotionContents contents) {
        ItemStack arrows = new ItemStack(Items.TIPPED_ARROW, ARROW_COUNT);
        arrows.set(DataComponents.POTION_CONTENTS, contents);
        return arrows;
    }
}
