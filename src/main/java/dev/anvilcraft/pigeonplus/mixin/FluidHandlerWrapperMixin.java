package dev.anvilcraft.pigeonplus.mixin;

import dev.anvilcraft.pigeonplus.fluid.PotionBottleTypes;
import dev.anvilcraft.pigeonplus.fluid.PotionFluidUtil;
import dev.dubhe.anvilcraft.api.fluid.FluidHandlerWrapper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FluidHandlerWrapper.class)
public class FluidHandlerWrapperMixin {
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
