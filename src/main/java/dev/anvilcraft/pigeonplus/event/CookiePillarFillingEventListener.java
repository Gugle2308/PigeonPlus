package dev.anvilcraft.pigeonplus.event;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.anvilcraft.pigeonplus.init.AddonItems;
import dev.anvilcraft.pigeonplus.init.ModCriterionTriggers;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = AnvilCraftPigeonPlus.MOD_ID)
public class CookiePillarFillingEventListener {
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        if (state.is(ModBlocks.COOKIE_PILLAR)) {
            Block filled = filledPillarFor(event.getItemStack());
            if (filled != null) {
                swap(event, level, pos, state, filled, new ItemStack(Items.BUCKET), SoundEvents.BUCKET_EMPTY);
            }
            return;
        }

        Item chocolate = chocolateBucketFor(state);
        if (chocolate != null && event.getItemStack().is(Items.BUCKET)) {
            swap(
                event,
                level,
                pos,
                state,
                ModBlocks.COOKIE_PILLAR.get(),
                new ItemStack(chocolate),
                SoundEvents.BUCKET_FILL
            );
        }
    }

    private static void swap(
        PlayerInteractEvent.RightClickBlock event,
        Level level,
        BlockPos pos,
        BlockState state,
        Block replacement,
        ItemStack result,
        net.minecraft.sounds.SoundEvent sound
    ) {
        if (!level.isClientSide()) {
            level.setBlockAndUpdate(
                pos,
                replacement.defaultBlockState().setValue(
                    RotatedPillarBlock.AXIS,
                    state.getValue(RotatedPillarBlock.AXIS)
                )
            );
            level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        Player player = event.getEntity();
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            ModCriterionTriggers.COOKIE_PILLAR_FILLED.get().trigger(serverPlayer);
        }
        if (!player.getAbilities().instabuild) {
            player.setItemInHand(
                event.getHand(),
                ItemUtils.createFilledResult(event.getItemStack(), player, result)
            );
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static Block filledPillarFor(ItemStack held) {
        if (held.is(AddonItems.LIQUID_CHOCOLATE_BUCKET.get())) {
            return AddonBlocks.CHOCOLATE_COOKIE_PILLAR.get();
        }
        if (held.is(AddonItems.LIQUID_CHOCOLATE_DARK_BUCKET.get())) {
            return AddonBlocks.DARK_CHOCOLATE_COOKIE_PILLAR.get();
        }
        if (held.is(AddonItems.LIQUID_CHOCOLATE_WHITE_BUCKET.get())) {
            return AddonBlocks.WHITE_CHOCOLATE_COOKIE_PILLAR.get();
        }
        return null;
    }

    private static Item chocolateBucketFor(BlockState state) {
        if (state.is(AddonBlocks.CHOCOLATE_COOKIE_PILLAR.get())) {
            return AddonItems.LIQUID_CHOCOLATE_BUCKET.get();
        }
        if (state.is(AddonBlocks.DARK_CHOCOLATE_COOKIE_PILLAR.get())) {
            return AddonItems.LIQUID_CHOCOLATE_DARK_BUCKET.get();
        }
        if (state.is(AddonBlocks.WHITE_CHOCOLATE_COOKIE_PILLAR.get())) {
            return AddonItems.LIQUID_CHOCOLATE_WHITE_BUCKET.get();
        }
        return null;
    }
}