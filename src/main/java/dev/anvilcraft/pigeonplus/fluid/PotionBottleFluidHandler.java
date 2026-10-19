package dev.anvilcraft.pigeonplus.fluid;

import dev.anvilcraft.pigeonplus.init.AddonFluids;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

public class PotionBottleFluidHandler implements IFluidHandlerItem {
    public static final int FLUID_PER_BOTTLE = 250;

    private ItemStack container;

    public PotionBottleFluidHandler(ItemStack container) {
        this.container = container;
    }

    @Override
    public ItemStack getContainer() {
        return this.container;
    }

    private boolean isWaterBottle() {
        if (!this.container.is(Items.POTION)) {
            return false;
        }
        PotionContents contents = this.container.get(DataComponents.POTION_CONTENTS);
        return contents != null
            && contents.potion().isPresent()
            && contents.potion().get().is(Potions.WATER);
    }

    private boolean isPotionBottle() {
        if (!this.container.has(DataComponents.POTION_CONTENTS)) {
            return false;
        }
        PotionContents contents = this.container.get(DataComponents.POTION_CONTENTS);
        return contents != null
            && contents.potion().isPresent()
            && !contents.potion().get().is(Potions.WATER);
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        if (this.isWaterBottle()) {
            return new FluidStack(Fluids.WATER, FLUID_PER_BOTTLE);
        }
        if (!this.isPotionBottle()) {
            return FluidStack.EMPTY;
        }
        PotionContents contents = this.container.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return FluidStack.EMPTY;
        }
        return PotionFluidUtil.withContents(
            new FluidStack(AddonFluids.POTION.get(), FLUID_PER_BOTTLE), contents);
    }

    @Override
    public int getTankCapacity(int tank) {
        return FLUID_PER_BOTTLE;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return false;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return this.drain(FLUID_PER_BOTTLE, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain < FLUID_PER_BOTTLE) {
            return FluidStack.EMPTY;
        }
        if (!this.isPotionBottle() && !this.isWaterBottle()) {
            return FluidStack.EMPTY;
        }
        FluidStack drained = this.getFluidInTank(0);
        if (drained.isEmpty()) {
            return FluidStack.EMPTY;
        }
        if (action.execute()) {
            this.container = PotionBottleTypes.emptyFor(this.container);
        }
        return drained;
    }
}
