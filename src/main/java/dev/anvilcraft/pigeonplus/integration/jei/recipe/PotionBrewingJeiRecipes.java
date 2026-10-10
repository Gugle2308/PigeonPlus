package dev.anvilcraft.pigeonplus.integration.jei.recipe;

import dev.anvilcraft.pigeonplus.fluid.PotionFluidUtil;
import dev.anvilcraft.pigeonplus.fluid.PotionSoakingRecipes;
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
        List<PotionBrewingJeiRecipe> basic = new ArrayList<>();
        List<PotionBrewingJeiRecipe> corruption = new ArrayList<>();
        List<PotionBrewingJeiRecipe> enhancement = new ArrayList<>();
        reagentStacks.forEach((key, stack) -> {
            List<FluidStack> ins = inputs.get(key);
            List<FluidStack> outs = outputs.get(key);
            // 只有「输入与输出一一对应」时才折叠成一条。
            //
            // 发酵蛛眼会把跳跃和迅捷都变成缓慢（多对一），折叠后输入输出无法配对，
            // 所以这类试剂拆成逐条列出。判据用「输出是否有重复」而不是写死物品，
            // 这样模组新增的试剂也会被正确判断。
            if (hasDuplicateResults(resultPotions.get(key))) {
                for (int i = 0; i < ins.size(); i++) {
                    corruption.add(new PotionBrewingJeiRecipe(stack, List.of(ins.get(i)), List.of(outs.get(i))));
                }
                return;
            }
            PotionBrewingJeiRecipe recipe = new PotionBrewingJeiRecipe(stack, ins, outs);
            // 红石/萤石只改时长或等级，单独排在最后。
            if (isEnhancementReagent(resultPotions.get(key), ins)) {
                enhancement.add(recipe);
            } else {
                basic.add(recipe);
            }
        });
        // 顺序：基础酿造 -> 蜘蛛眼反转 -> 红石/萤石强化。
        List<PotionBrewingJeiRecipe> recipes = new ArrayList<>(basic);
        recipes.addAll(corruption);
        recipes.addAll(enhancement);
        return recipes;
    }

    // 强化试剂：它的每一条都是「输入本身的长效/强力版本」，即只改时长或等级。
    // 红石与萤石满足这个条件；糖、烈焰粉等改变效果的试剂不满足。
    private static boolean isEnhancementReagent(List<Holder<Potion>> results, List<FluidStack> inputs) {
        if (results.isEmpty()) {
            return false;
        }
        for (int i = 0; i < results.size(); i++) {
            Holder<Potion> result = results.get(i);
            if (!PotionVariants.isEnhanced(result)) {
                return false;
            }
            if (!PotionVariants.basePath(result).equals(potionPathOf(inputs.get(i)))) {
                return false;
            }
        }
        return true;
    }

    private static String potionPathOf(FluidStack fluid) {
        PotionContents contents = PotionFluidUtil.getContents(fluid);
        if (contents == null || contents.potion().isEmpty()) {
            return "";
        }
        return PotionVariants.basePath(contents.potion().get());
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

    /**
     * 把这些流体对应的药水物品取出来，供 JEI 的物品查询登记为不可见成分。
     */
    public static List<ItemStack> potionItemsOf(List<FluidStack> fluids) {
        List<ItemStack> items = new ArrayList<>();
        for (FluidStack fluid : fluids) {
            items.add(potionItemOf(fluid));
        }
        return items;
    }

    private static ItemStack potionItemOf(FluidStack fluid) {
        if (fluid.is(Fluids.WATER)) {
            // 水在物品层就是「水瓶」，这样查水瓶也能找到酿造链的起点。
            return potionItem(new PotionContents(Potions.WATER));
        }
        return potionItem(PotionFluidUtil.getContents(fluid));
    }

    private static ItemStack potionItem(PotionContents contents) {
        ItemStack item = new ItemStack(Items.POTION);
        item.set(DataComponents.POTION_CONTENTS, contents);
        return item;
    }

    /**
     * 泡药：把可浸泡的物品放进药水里，注入对应的药水效果。
     */
    public static List<PotionSoakingJeiRecipe> buildSoaking(int amount) {
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
        // 只有能作为酿造结果的药水才可能用来浸泡：用同一套规则筛一遍。
        Map<String, PotionContents> reachable = new LinkedHashMap<>();
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
                if (result.potion().get().is(Potions.MUNDANE) || result.potion().get().is(Potions.THICK)) {
                    continue;
                }
                reachable.putIfAbsent(PotionVariants.path(result.potion().get()), result);
            }
        });
        // 水本身也算一种浸泡液，它是整条链的起点。
        reachable.putIfAbsent("water", new PotionContents(Potions.WATER));
        List<PotionSoakingJeiRecipe> recipes = new ArrayList<>();
        // 可浸泡物品取自共享定义，与实际酿造用的是同一份，不会两边漂移。
        for (PotionSoakingRecipes.Soakable soakable : PotionSoakingRecipes.all()) {
            List<FluidStack> potions = new ArrayList<>();
            List<ItemStack> results = new ArrayList<>();
            reachable.forEach((path, contents) -> {
                potions.add(inputFluidOf(contents, amount));
                results.add(soakable.soakedWith(contents));
            });
            recipes.add(new PotionSoakingJeiRecipe(soakable.blank(), potions, results));
        }
        return recipes;
    }

    private static FluidStack inputFluidOf(PotionContents contents, int amount) {
        if (contents.potion().isPresent() && contents.potion().get().is(Potions.WATER)) {
            return new FluidStack(Fluids.WATER, amount);
        }
        return toFluid(contents, amount);
    }
}
