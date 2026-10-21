package dev.anvilcraft.pigeonplus.util;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

/**
 * 铁砧泵的流体接口。
 *
 * <p>铁砧泵本身不存储流体：它只是流体网络中的一个泵节点，流体由管网移送。
 * 但管道判断“邻居是否可连接”时会走 {@code PipeBlock#isFluidHandler}，
 * 那里只认 {@code FluidContainerLookup#find} 能否取到 {@code IFluidHandler}，
 * 因此必须暴露一个接口，泵才会被管道当作可连接端点。
 *
 * <p>所有读写都保持为空/零，泵不会因此成为流体的来源或去处。
 */
public final class AnvilPumpFluidHandler implements IFluidHandler {
    public static final AnvilPumpFluidHandler INSTANCE = new AnvilPumpFluidHandler();

    private AnvilPumpFluidHandler() {
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return 0;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return false;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return 0;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        return FluidStack.EMPTY;
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        return FluidStack.EMPTY;
    }
}
