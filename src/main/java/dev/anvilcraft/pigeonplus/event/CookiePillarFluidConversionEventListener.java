package dev.anvilcraft.pigeonplus.event;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.anvilcraft.pigeonplus.init.AddonFluids;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = AnvilCraftPigeonPlus.MOD_ID)
public class CookiePillarFluidConversionEventListener {
    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        BlockState placed = event.getPlacedBlock();
        if (!placed.is(ModBlocks.COOKIE_PILLAR)) {
            return;
        }
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }
        BlockState before = event.getBlockSnapshot().getState();
        Block replacement = filledFor(before);
        if (replacement == null) {
            return;
        }
        BlockPos pos = event.getPos();
        level.setBlockAndUpdate(
            pos,
            replacement.defaultBlockState().setValue(
                RotatedPillarBlock.AXIS,
                placed.getValue(RotatedPillarBlock.AXIS)
            )
        );
    }

    private static Block filledFor(BlockState before) {
        if (!before.getFluidState().isSource()) {
            return null;
        }
        Fluid fluid = before.getFluidState().getType();
        if (fluid.isSame(AddonFluids.LIQUID_CHOCOLATE.get())) {
            return AddonBlocks.CHOCOLATE_COOKIE_PILLAR.get();
        }
        if (fluid.isSame(AddonFluids.LIQUID_CHOCOLATE_DARK.get())) {
            return AddonBlocks.DARK_CHOCOLATE_COOKIE_PILLAR.get();
        }
        if (fluid.isSame(AddonFluids.LIQUID_CHOCOLATE_WHITE.get())) {
            return AddonBlocks.WHITE_CHOCOLATE_COOKIE_PILLAR.get();
        }
        return null;
    }
}
