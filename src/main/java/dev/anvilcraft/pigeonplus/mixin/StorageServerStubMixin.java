package dev.anvilcraft.pigeonplus.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.anvilcraft.pigeonplus.fluid.PotionBottleTypes;
import dev.anvilcraft.pigeonplus.fluid.PotionFluidUtil;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "dev.dubhe.anvilcraft.rpc.StorageServerStub")
public class StorageServerStubMixin {
    @Unique
    private static final ThreadLocal<BaseStorage<?>> PIGEONPLUS_STORAGE = new ThreadLocal<>();

    @ModifyExpressionValue(
        method = "fillBucketFromStorage(Lnet/minecraft/server/level/ServerPlayer;Ldev/dubhe/anvilcraft/rpc/StorageServerStub$StorageView;Lnet/neoforged/neoforge/fluids/FluidStack;ZZ)Ldev/dubhe/anvilcraft/rpc/StorageServerStub$FilledBucket;",
        at = @At(
            value = "INVOKE",
            target = "Ldev/dubhe/anvilcraft/rpc/StorageServerStub$StorageView;primary()Ldev/dubhe/anvilcraft/saved/storage/BaseStorage;"
        )
    )
    private static BaseStorage<?> pigeonplus$rememberStorage(BaseStorage<?> original) {
        StorageServerStubMixin.PIGEONPLUS_STORAGE.set(original);
        return original;
    }

    @Inject(
        method = "fillBucketFromStorage(Lnet/minecraft/server/level/ServerPlayer;Ldev/dubhe/anvilcraft/rpc/StorageServerStub$StorageView;Lnet/neoforged/neoforge/fluids/FluidStack;ZZ)Ldev/dubhe/anvilcraft/rpc/StorageServerStub$FilledBucket;",
        at = @At("RETURN")
    )
    private static void pigeonplus$clearStorage(CallbackInfoReturnable<?> cir) {
        StorageServerStubMixin.PIGEONPLUS_STORAGE.remove();
    }

    @ModifyExpressionValue(
        method = "fillBucketFromStorage(Lnet/minecraft/server/level/ServerPlayer;Ldev/dubhe/anvilcraft/rpc/StorageServerStub$StorageView;Lnet/neoforged/neoforge/fluids/FluidStack;ZZ)Ldev/dubhe/anvilcraft/rpc/StorageServerStub$FilledBucket;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/Item;getDefaultInstance()Lnet/minecraft/world/item/ItemStack;"
        )
    )
    private static ItemStack pigeonplus$preferPotionBottle(
        ItemStack original,
        @Local(argsOnly = true) ServerPlayer player,
        @Local(argsOnly = true) FluidStack fluid
    ) {
        if (original.isEmpty() || !PotionFluidUtil.isPotion(fluid)) {
            return original;
        }
        ItemStack carried = player.containerMenu.getCarried();
        if (PotionBottleTypes.isEmptyBottle(carried) && !carried.is(Items.GLASS_BOTTLE)) {
            return carried.copyWithCount(1);
        }
        if (pigeonplus$hasGlassBottle(player)) {
            return original;
        }
        ItemStack inInventory = pigeonplus$potionBottleIn(player.getInventory());
        if (inInventory != null) {
            return inInventory;
        }
        ItemStack inStorage = PotionBottleTypes.emptyPotionBottleIn(
            StorageServerStubMixin.PIGEONPLUS_STORAGE.get());
        if (inStorage != null) {
            return inStorage;
        }
        return original;
    }

    @Unique
    private static boolean pigeonplus$hasGlassBottle(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            if (inventory.getItem(slot).is(Items.GLASS_BOTTLE)) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private static ItemStack pigeonplus$potionBottleIn(Inventory inventory) {
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (PotionBottleTypes.isEmptyBottle(stack) && !stack.is(Items.GLASS_BOTTLE)) {
                return stack.copyWithCount(1);
            }
        }
        return null;
    }
}
