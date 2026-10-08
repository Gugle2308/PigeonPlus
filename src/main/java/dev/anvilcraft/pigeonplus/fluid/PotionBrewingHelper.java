package dev.anvilcraft.pigeonplus.fluid;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;

public final class PotionBrewingHelper {
    private PotionBrewingHelper() {
    }

    // 原版酿造链条从水开始，而水里没有 POTION_CONTENTS，所以要把普通水视作「水药水」才能复用配方。
    public static PotionContents resolveContents(FluidStack stored) {
        if (stored.getFluid() instanceof PotionFluid) {
            return PotionFluidUtil.getContents(stored);
        }
        if (stored.is(Fluids.WATER)) {
            return new PotionContents(Potions.WATER);
        }
        return null;
    }

    // 数值越小越优先：0 = 下界疣制粗制，1 = 酿造材料出效果，2 = 红石/萤石强化。
    // -1 表示无用药水（平凡、浓稠），永不选择——否则水加红石会先变成平凡药水而白白浪费。
    public static Selection select(PotionBrewing brewing, PotionContents contents, IItemHandler input) {
        ItemStack potionItem = new ItemStack(Items.POTION);
        potionItem.set(DataComponents.POTION_CONTENTS, contents);
        int chosenSlot = -1;
        int chosenTier = Integer.MAX_VALUE;
        PotionContents chosenResult = null;
        for (int slot = 0; slot < input.getSlots(); slot++) {
            ItemStack ingredient = input.getStackInSlot(slot);
            if (ingredient.isEmpty()) {
                continue;
            }
            // hasMix 是 (药水, 材料)，而 mix 是 (材料, 药水)——两者参数顺序相反。
            if (!brewing.hasMix(potionItem, ingredient)) {
                continue;
            }
            ItemStack brewed = brewing.mix(ingredient, potionItem);
            if (!brewed.is(potionItem.getItem())) {
                // 容器配方（火药、龙息）只改变瓶子形态，流体层表示不了，跳过以免白白消耗材料。
                continue;
            }
            PotionContents result = brewed.get(DataComponents.POTION_CONTENTS);
            if (result == null || result.potion().isEmpty()) {
                continue;
            }
            // 结果与当前药水相同时说明该配方对本药水无效（药水已到该链条能到达的状态）。
            // 若不拦截，材料会被消耗而锅里的药水毫无变化——即「白白消耗材料」。
            if (contents.potion().isPresent() && result.potion().get().is(contents.potion().get())) {
                continue;
            }
            int tier = brewTier(ingredient, result.potion().get());
            if (tier < 0 || tier >= chosenTier) {
                continue;
            }
            chosenTier = tier;
            chosenSlot = slot;
            chosenResult = result;
        }
        return chosenSlot < 0 ? null : new Selection(chosenSlot, chosenResult);
    }

    private static int brewTier(ItemStack ingredient, Holder<Potion> result) {
        if (result.is(Potions.MUNDANE) || result.is(Potions.THICK)) {
            return -1;
        }
        if (ingredient.is(Items.NETHER_WART)) {
            return 0;
        }
        return PotionVariants.isEnhanced(result) ? 2 : 1;
    }

    public record Selection(int slot, PotionContents result) {
    }
}
