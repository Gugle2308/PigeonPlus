package dev.anvilcraft.pigeonplus.client.fluid;

import dev.anvilcraft.pigeonplus.fluid.PotionFluidUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

public class PotionFluidClientExtensions implements IClientFluidTypeExtensions {
    private static final ResourceLocation STILL =
        ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation FLOWING =
        ResourceLocation.withDefaultNamespace("block/water_flow");
    private static final int FALLBACK_COLOR = 0xFF385DC6;

    @Override
    public ResourceLocation getStillTexture() {
        return STILL;
    }

    @Override
    public ResourceLocation getFlowingTexture() {
        return FLOWING;
    }

    @Override
    public int getTintColor() {
        return FALLBACK_COLOR;
    }

    @Override
    public int getTintColor(FluidStack stack) {
        PotionContents contents = PotionFluidUtil.getContents(stack);
        if (contents == null) {
            return FALLBACK_COLOR;
        }
        return 0xFF000000 | contents.getColor();
    }
}
