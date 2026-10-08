package dev.anvilcraft.pigeonplus.event;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.fluid.PotionFluid;
import dev.anvilcraft.pigeonplus.fluid.PotionFluidUtil;
import dev.anvilcraft.pigeonplus.init.AddonFluids;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.block.BurningHeaterBlock;
import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

@EventBusSubscriber(modid = AnvilCraftPigeonPlus.MOD_ID)
public class FishTankBrewingEventListener {
    @SubscribeEvent
    public static void onAnvilLand(AnvilEvent.OnLand event) {
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        BlockPos tankPos = event.getPos().below();
        if (!(level.getBlockEntity(tankPos) instanceof FishTankBlockEntity tank)) {
            return;
        }
        if (!hasActiveHeaterBelow(level, tankPos)) {
            return;
        }
        IFluidHandler fluids = tank.getFluidHandler();
        FluidStack stored = fluids.getFluidInTank(0);
        if (!(stored.getFluid() instanceof PotionFluid)) {
            return;
        }
        PotionContents contents = PotionFluidUtil.getContents(stored);
        if (contents == null) {
            return;
        }
        PotionBrewing brewing = level.potionBrewing();
        ItemStack potionItem = new ItemStack(Items.POTION);
        potionItem.set(DataComponents.POTION_CONTENTS, contents);
        IItemHandler input = tank.getInput();
        for (int slot = 0; slot < input.getSlots(); slot++) {
            ItemStack ingredient = input.getStackInSlot(slot);
            if (ingredient.isEmpty()) {
                continue;
            }
            // hasMix 是 (药水, 材料)，而 mix 是 (材料, 药水)——两者参数顺序相反。
            if (!brewing.hasMix(potionItem, ingredient)) {
                continue;
            }
            ItemStack brewed = brewing.mix(ingredient, potionItem);
            if (!brewed.is(potionItem.getItem())) {
                // 容器配方（火药、龙息）只改变瓶子形态，流体层表示不了，跳过以免白白消耗材料。
                continue;
            }
            PotionContents result = brewed.get(DataComponents.POTION_CONTENTS);
            if (result == null) {
                return;
            }
            int amount = stored.getAmount();
            input.extractItem(slot, 1, false);
            fluids.drain(amount, IFluidHandler.FluidAction.EXECUTE);
            fluids.fill(
                PotionFluidUtil.withContents(new FluidStack(AddonFluids.POTION.get(), amount), result),
                IFluidHandler.FluidAction.EXECUTE
            );
            level.playSound(null, tankPos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F);
            return;
        }
    }

    private static boolean hasActiveHeaterBelow(Level level, BlockPos tankPos) {
        BlockState state = level.getBlockState(tankPos.below());
        if (state.is(ModBlocks.HEATER)) {
            return !state.getValue(HeaterBlock.OVERLOAD);
        }
        return state.is(ModBlocks.BURNING_HEATER) && state.getValue(BurningHeaterBlock.LEVEL) == 2;
    }
}
