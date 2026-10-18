package dev.anvilcraft.pigeonplus.event;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.anvilcraft.pigeonplus.init.AddonFluids;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = AnvilCraftPigeonPlus.MOD_ID)
public class CookiePillarFluidConversionEventListener {
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack held = event.getItemStack();
        if (!(held.getItem() instanceof BlockItem blockItem)) {
            return;
        }
        if (!blockItem.getBlock().equals(ModBlocks.COOKIE_PILLAR.get())) {
            return;
        }
        Level level = event.getLevel();
        BlockPlaceContext context =
            new BlockPlaceContext(event.getEntity(), event.getHand(), held, event.getHitVec());
        BlockPos target = context.getClickedPos();
        Block replacement = filledFor(level.getBlockState(target));
        if (replacement == null) {
            return;
        }
        if (!level.isClientSide()) {
            BlockState placed = replacement.defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, context.getClickedFace().getAxis());
            level.setBlockAndUpdate(target, placed);
            level.playSound(
                null,
                target,
                SoundType.NETHERRACK.getPlaceSound(),
                SoundSource.BLOCKS,
                1.0F,
                1.0F
            );
            if (!event.getEntity().getAbilities().instabuild) {
                held.shrink(1);
            }
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static Block filledFor(BlockState state) {
        if (!state.getFluidState().isSource()) {
            return null;
        }
        Fluid fluid = state.getFluidState().getType();
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
