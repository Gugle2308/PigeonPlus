package dev.anvilcraft.pigeonplus.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.anvilcraft.pigeonplus.fluid.PotionBottleTypes;
import dev.anvilcraft.pigeonplus.fluid.PotionFluidUtil;
import dev.dubhe.anvilcraft.api.fluid.FluidHandlerWrapper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FluidHandlerWrapper.class)
public class FluidHandlerWrapperMixin {
    @ModifyExpressionValue(
        method = "determineFilledContainer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"
        )
    )
    private static boolean pigeonplus$acceptOwnBottles(
        boolean original,
        @Local(argsOnly = true) ItemStack emptyContainer
    ) {
        if (original) {
            return true;
        }
        return PotionBottleTypes.isEmptyBottle(emptyContainer);
    }

    @ModifyExpressionValue(
        method = "determineFilledContainer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/alchemy/PotionContents;createItemStack(Lnet/minecraft/world/item/Item;Lnet/minecraft/core/Holder;)Lnet/minecraft/world/item/ItemStack;"
        )
    )
    private static ItemStack pigeonplus$waterInOwnBottle(
        ItemStack original,
        @Local(argsOnly = true) ItemStack emptyContainer
    ) {
        Item filled = PotionBottleTypes.filledFor(emptyContainer);
        if (filled == null || filled == original.getItem()) {
            return original;
        }
        ItemStack water = new ItemStack(filled);
        water.set(DataComponents.POTION_CONTENTS, new PotionContents(net.minecraft.world.item.alchemy.Potions.WATER));
        return water;
    }

    @Inject(method = "determineFilledContainer", at = @At("RETURN"), cancellable = true)
    private static void pigeonplus$determinePotionBottle(
        FluidStack drained,
        ItemStack emptyContainer,
        CallbackInfoReturnable<ItemStack> cir
    ) {
        ItemStack resolved = cir.getReturnValue();
        if (resolved != null && !resolved.isEmpty()) {
            return;
        }
        PotionContents contents = PotionFluidUtil.getContents(drained);
        if (contents == null || contents.potion().isEmpty()) {
            return;
        }
        Item filled = PotionBottleTypes.filledFor(emptyContainer);
        if (filled == null) {
            return;
        }
        ItemStack potion = new ItemStack(filled);
        potion.set(DataComponents.POTION_CONTENTS, contents);
        cir.setReturnValue(potion);
    }
}
