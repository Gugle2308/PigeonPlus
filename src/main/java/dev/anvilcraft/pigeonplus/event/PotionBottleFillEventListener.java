package dev.anvilcraft.pigeonplus.event;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.fluid.PotionBottleTypes;
import dev.anvilcraft.pigeonplus.fluid.PotionFluid;
import dev.anvilcraft.pigeonplus.fluid.PotionFluidUtil;
import dev.anvilcraft.pigeonplus.init.AddonFluids;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

@EventBusSubscriber(modid = AnvilCraftPigeonPlus.MOD_ID)
public class PotionBottleFillEventListener {
    private static final int FLUID_PER_BOTTLE = 250;

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        ItemStack held = event.getItemStack();
        // 创造流体储罐自己会用「物品的流体能力」来交互：读到容器里的流体后把储量设成无限，
        // 以此完成「标记流体」。我们若抢先灌入并取消事件，它的 useItemOn 就跑不到，于是永远标不上。
        //
        // 只对「手持装药水的瓶子」让路——那正是标记路径。
        // 手持空瓶时反而要由我们处理：它的容器交互不认我们的药水，只有我们能从罐里装出药水。
        // 以后遇到同类方块，往这里加。
        if (PotionBottleTypes.isPotion(held)
            && level.getBlockState(pos).is(ModBlocks.CREATIVE_FLUID_TANK)) {
            return;
        }
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, event.getFace());
        if (handler == null) {
            return;
        }
        if (!withdrawPotion(event, player, level, pos, handler, held)) {
            depositPotion(event, player, level, pos, handler, held);
        }
    }

    private static boolean withdrawPotion(
        PlayerInteractEvent.RightClickBlock event,
        Player player,
        Level level,
        BlockPos pos,
        IFluidHandler handler,
        ItemStack bottle
    ) {
        Item filledItem = PotionBottleTypes.filledFor(bottle);
        if (filledItem == null) {
            return false;
        }
        FluidStack simulated = handler.drain(FLUID_PER_BOTTLE, IFluidHandler.FluidAction.SIMULATE);
        if (!(simulated.getFluid() instanceof PotionFluid)) {
            return false;
        }
        if (simulated.getAmount() < FLUID_PER_BOTTLE) {
            return false;
        }
        PotionContents contents = PotionFluidUtil.getContents(simulated);
        if (contents == null) {
            return false;
        }
        if (!level.isClientSide()) {
            handler.drain(FLUID_PER_BOTTLE, IFluidHandler.FluidAction.EXECUTE);
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        ItemStack filled = new ItemStack(filledItem);
        filled.set(DataComponents.POTION_CONTENTS, contents);
        player.setItemInHand(event.getHand(), ItemUtils.createFilledResult(bottle, player, filled));
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        return true;
    }

    private static void depositPotion(
        PlayerInteractEvent.RightClickBlock event,
        Player player,
        Level level,
        BlockPos pos,
        IFluidHandler handler,
        ItemStack potion
    ) {
        if (!PotionBottleTypes.isPotion(potion)) {
            return;
        }
        PotionContents contents = potion.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return;
        }
        FluidStack toFill =
            PotionFluidUtil.withContents(new FluidStack(AddonFluids.POTION.get(), FLUID_PER_BOTTLE), contents);
        if (handler.fill(toFill, IFluidHandler.FluidAction.SIMULATE) < FLUID_PER_BOTTLE) {
            return;
        }
        if (!level.isClientSide()) {
            handler.fill(toFill, IFluidHandler.FluidAction.EXECUTE);
            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        player.setItemInHand(
            event.getHand(),
            ItemUtils.createFilledResult(potion, player, PotionBottleTypes.emptyFor(potion))
        );
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
