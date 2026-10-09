package dev.anvilcraft.pigeonplus.integration.jei.recipe;

import dev.anvilcraft.pigeonplus.fluid.PotionFluidUtil;
import dev.anvilcraft.pigeonplus.fluid.PotionVariants;
import dev.anvilcraft.pigeonplus.init.AddonFluids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PotionBrewingJeiRecipes {
    private PotionBrewingJeiRecipes() {
    }

    /**
     * @param amount 展示用的流体容量，小锅传 1000、大锅传 8000
     */
    public static List<PotionBrewingJeiRecipe> build(int amount) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return List.of();
        }
        PotionBrewing brewing = connection.potionBrewing();
        List<ItemStack> reagents = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (brewing.isIngredient(stack)) {
                reagents.add(stack);
            }
        }
        // 先收集全部 (试剂, 输入, 输出) 三元组，再决定哪些试剂可以折叠。
        Map<Item, ItemStack> reagentStacks = new LinkedHashMap<>();
        Map<Item, List<FluidStack>> inputs = new LinkedHashMap<>();
        Map<Item, List<FluidStack>> outputs = new LinkedHashMap<>();
        Map<Item, List<Holder<Potion>>> resultPotions = new LinkedHashMap<>();
        BuiltInRegistries.POTION.holders().forEach(potion -> {
            ItemStack potionStack = new ItemStack(Items.POTION);
            potionStack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
            for (ItemStack reagent : reagents) {
                if (!brewing.hasMix(potionStack, reagent)) {
                    continue;
                }
                ItemStack mixed = brewing.mix(reagent, potionStack);
                // 容器配方（火药、龙息）只换瓶子形态，流体层表示不了，与酿造实现保持一致地略过。
                if (!mixed.is(Items.POTION)) {
                    continue;
                }
                PotionContents result = mixed.get(DataComponents.POTION_CONTENTS);
                if (result == null || result.potion().isEmpty()) {
                    continue;
                }
                // 平凡与浓稠在实际酿造里被拒绝，列出来会误导。
                if (result.potion().get().is(Potions.MUNDANE) || result.potion().get().is(Potions.THICK)) {
                    continue;
                }
                Item key = reagent.getItem();
                reagentStacks.putIfAbsent(key, reagent);
                inputs.computeIfAbsent(key, k -> new ArrayList<>()).add(inputFluid(potion, amount));
                outputs.computeIfAbsent(key, k -> new ArrayList<>()).add(toFluid(result, amount));
                resultPotions.computeIfAbsent(key, k -> new ArrayList<>()).add(result.potion().get());
            }
        });
        List<PotionBrewingJeiRecipe> folded = new ArrayList<>();
        List<PotionBrewingJeiRecipe> unfolded = new ArrayList<>();
        reagentStacks.forEach((key, stack) -> {
            List<FluidStack> ins = inputs.get(key);
            List<FluidStack> outs = outputs.get(key);
            // 只有「输入与输出一一对应」时才折叠成一条。
            //
            // 发酵蛛眼会把跳跃和迅捷都变成缓慢（多对一），折叠后输入输出无法配对，
            // 所以这类试剂拆成逐条列出。判据用「输出是否有重复」而不是写死物品，
            // 这样模组新增的试剂也会被正确判断。
            //
            // 拆开的条目统一排到最后：它们数量多（发酵蛛眼就有 12 条），
            // 混在折叠条目之间会把「一种试剂一条」的主干冲散。
            if (hasDuplicateResults(resultPotions.get(key))) {
                for (int i = 0; i < ins.size(); i++) {
                    unfolded.add(new PotionBrewingJeiRecipe(stack, List.of(ins.get(i)), List.of(outs.get(i))));
                }
            } else {
                folded.add(new PotionBrewingJeiRecipe(stack, ins, outs));
            }
        });
        folded.addAll(unfolded);
        return folded;
    }

    // 用注册名判定重复。不能用 FluidStack 去重：它没有覆写 equals/hashCode，
    // 集合去重会退化成对象身份比较，永远判不出重复。
    private static boolean hasDuplicateResults(List<Holder<Potion>> outputs) {
        Set<String> seen = new HashSet<>();
        for (Holder<Potion> output : outputs) {
            if (!seen.add(PotionVariants.path(output))) {
                return true;
            }
        }
        return false;
    }

    private static FluidStack toFluid(PotionContents contents, int amount) {
        return PotionFluidUtil.withContents(new FluidStack(AddonFluids.POTION.get(), amount), contents);
    }

    // 水在锅里是真正的水流体（PotionBrewingHelper#resolveContents 也是这么认的），
    // 若按药水流体展示会经 Potion.getName 显示成「水瓶」。
    private static FluidStack inputFluid(Holder<Potion> potion, int amount) {
        if (potion.is(Potions.WATER)) {
            return new FluidStack(Fluids.WATER, amount);
        }
        return toFluid(new PotionContents(potion), amount);
    }
}
