package dev.anvilcraft.pigeonplus.event;

import dev.anvilcraft.pigeonplus.fluid.PotionBottleTypes;
import dev.anvilcraft.pigeonplus.fluid.PotionFluid;
import dev.anvilcraft.pigeonplus.fluid.PotionFluidUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * 让发射器能用玻璃瓶从储罐取出药水。
 *
 * <p>刻意<b>直接实现</b> {@link DispenseItemBehavior} 而不继承
 * {@link DefaultDispenseItemBehavior}：AnvilCraft 把它的炼药锅交互注入在
 * {@code DefaultDispenseItemBehavior#execute} 上，一旦覆写该方法就会整段绕过。
 * 这里只加一条药水分支，其余情况原样交给原行为实例，保证既有行为完全不变。
 */
public class PotionBottleDispenseBehavior implements DispenseItemBehavior {
    private static final int FLUID_PER_BOTTLE = 250;

    private final DispenseItemBehavior fallback;

    public PotionBottleDispenseBehavior(DispenseItemBehavior fallback) {
        this.fallback = fallback;
    }

    @Override
    public ItemStack dispense(BlockSource source, ItemStack stack) {
        ItemStack filled = this.tryFillFromTank(source, stack);
        return filled != null ? filled : this.fallback.dispense(source, stack);
    }

    private ItemStack tryFillFromTank(BlockSource source, ItemStack stack) {
        Item filledItem = PotionBottleTypes.filledFor(stack);
        if (filledItem == null) {
            return null;
        }
        Direction facing = source.state().getValue(DispenserBlock.FACING);
        BlockPos pos = source.pos().relative(facing);
        IFluidHandler handler =
            source.level().getCapability(Capabilities.FluidHandler.BLOCK, pos, facing.getOpposite());
        if (handler == null) {
            return null;
        }
        FluidStack simulated = handler.drain(FLUID_PER_BOTTLE, IFluidHandler.FluidAction.SIMULATE);
        if (!(simulated.getFluid() instanceof PotionFluid)) {
            return null;
        }
        if (simulated.getAmount() < FLUID_PER_BOTTLE) {
            return null;
        }
        PotionContents contents = PotionFluidUtil.getContents(simulated);
        if (contents == null) {
            return null;
        }
        handler.drain(FLUID_PER_BOTTLE, IFluidHandler.FluidAction.EXECUTE);
        ItemStack potion = new ItemStack(filledItem);
        potion.set(DataComponents.POTION_CONTENTS, contents);
        source.level().levelEvent(1000, source.pos(), 0);
        source.level().levelEvent(2000, source.pos(), facing.get3DDataValue());
        source.level().playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        stack.shrink(1);
        if (stack.isEmpty()) {
            return potion;
        }
        ItemStack leftover = source.blockEntity().insertItem(potion);
        if (!leftover.isEmpty()) {
            DefaultDispenseItemBehavior.spawnItem(
                source.level(), leftover, 6, facing, DispenserBlock.getDispensePosition(source));
        }
        return stack;
    }
}
