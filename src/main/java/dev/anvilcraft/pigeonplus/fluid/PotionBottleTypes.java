package dev.anvilcraft.pigeonplus.fluid;

import dev.anvilcraft.pigeonplus.init.AddonItems;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.jetbrains.annotations.Nullable;

public final class PotionBottleTypes {
    private PotionBottleTypes() {
    }

    @Nullable
    public static Item filledFor(ItemStack bottle) {
        if (bottle.is(Items.GLASS_BOTTLE)) {
            return Items.POTION;
        }
        if (bottle.is(AddonItems.SPLASH_BOTTLE.get())) {
            return Items.SPLASH_POTION;
        }
        if (bottle.is(AddonItems.LINGERING_BOTTLE.get())) {
            return Items.LINGERING_POTION;
        }
        return null;
    }

    public static boolean isEmptyBottle(ItemStack stack) {
        return stack.is(Items.GLASS_BOTTLE)
            || stack.is(AddonItems.SPLASH_BOTTLE.get())
            || stack.is(AddonItems.LINGERING_BOTTLE.get());
    }

    @Nullable
    public static ItemStack emptyPotionBottleIn(@Nullable BaseStorage<?> storage) {
        if (storage == null) {
            return null;
        }
        UnlimitedItemStacksResourceHandler items = storage.getItems();
        for (int slot = 0; slot < items.size(); slot++) {
            if (items.getAmountAsLong(slot) <= 0) {
                continue;
            }
            ItemStack stack = items.getUnlimitedStackInSlot(slot).toStack();
            if (isEmptyBottle(stack) && !stack.is(Items.GLASS_BOTTLE)) {
                return stack.copyWithCount(1);
            }
        }
        return null;
    }

    public static boolean isPotion(ItemStack stack) {
        if (!stack.is(Items.POTION)
            && !stack.is(Items.SPLASH_POTION)
            && !stack.is(Items.LINGERING_POTION)) {
            return false;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && contents.potion().isPresent() && !contents.potion().get().is(Potions.WATER);
    }

    public static ItemStack emptyFor(ItemStack potion) {
        if (potion.is(Items.SPLASH_POTION)) {
            return new ItemStack(AddonItems.SPLASH_BOTTLE.get());
        }
        if (potion.is(Items.LINGERING_POTION)) {
            return new ItemStack(AddonItems.LINGERING_BOTTLE.get());
        }
        return new ItemStack(Items.GLASS_BOTTLE);
    }
}
