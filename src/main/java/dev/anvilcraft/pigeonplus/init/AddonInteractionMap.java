package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntry;
import dev.anvilcraft.pigeonplus.block.AddonLiquidCauldronBlock;
import dev.anvilcraft.pigeonplus.block.MixedBiomassCauldronBlock;
import dev.dubhe.anvilcraft.block.Layered4LevelCauldronBlock;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

public class AddonInteractionMap {
    public static final CauldronInteraction.InteractionMap MIXED_BIOMASS =
        CauldronInteraction.newInteractionMap("mixed_biomass");
    public static final CauldronInteraction.InteractionMap LIQUID_CHOCOLATE =
        CauldronInteraction.newInteractionMap("liquid_chocolate");
    public static final CauldronInteraction.InteractionMap LIQUID_CHOCOLATE_DARK =
        CauldronInteraction.newInteractionMap("liquid_chocolate_dark");
    public static final CauldronInteraction.InteractionMap LIQUID_CHOCOLATE_WHITE =
        CauldronInteraction.newInteractionMap("liquid_chocolate_white");

    public static void init() {
        var mixedBiomassInteractionMap = MIXED_BIOMASS.map();
        mixedBiomassInteractionMap.put(
            Items.BUCKET,
            (state, level, pos, player, hand, stack) -> CauldronInteraction.fillBucket(
                state,
                level,
                pos,
                player,
                hand,
                stack,
                AddonItems.MIXED_BIOMASS_BUCKET.asStack(),
                s -> AddonBlocks.MIXED_BIOMASS_CAULDRON.get().isFull(state),
                SoundEvents.BUCKET_FILL
            )
        );

        CauldronInteraction.EMPTY.map().put(
            AddonItems.MIXED_BIOMASS_BUCKET.get(),
            (state, level, pos, player, hand, stack) -> CauldronInteraction.emptyBucket(
                level,
                pos,
                player,
                hand,
                stack,
                AddonBlocks.MIXED_BIOMASS_CAULDRON.get()
                    .defaultBlockState()
                    .setValue(MixedBiomassCauldronBlock.LEVEL, MixedBiomassCauldronBlock.MAX_LEVEL),
                SoundEvents.BUCKET_EMPTY
            )
        );

        liquidCauldron(
            LIQUID_CHOCOLATE,
            AddonBlocks.LIQUID_CHOCOLATE_CAULDRON,
            AddonItems.LIQUID_CHOCOLATE_BUCKET
        );
        liquidCauldron(
            LIQUID_CHOCOLATE_DARK,
            AddonBlocks.LIQUID_CHOCOLATE_DARK_CAULDRON,
            AddonItems.LIQUID_CHOCOLATE_DARK_BUCKET
        );
        liquidCauldron(
            LIQUID_CHOCOLATE_WHITE,
            AddonBlocks.LIQUID_CHOCOLATE_WHITE_CAULDRON,
            AddonItems.LIQUID_CHOCOLATE_WHITE_BUCKET
        );
    }

    private static void liquidCauldron(
        CauldronInteraction.InteractionMap interactions,
        BlockEntry<AddonLiquidCauldronBlock> cauldron,
        Supplier<? extends Item> bucket
    ) {
        interactions.map().put(
            Items.BUCKET,
            (state, level, pos, player, hand, stack) -> CauldronInteraction.fillBucket(
                state,
                level,
                pos,
                player,
                hand,
                stack,
                new ItemStack(bucket.get()),
                cauldron.get()::isFull,
                SoundEvents.BUCKET_FILL
            )
        );

        CauldronInteraction.EMPTY.map().put(
            bucket.get(),
            (state, level, pos, player, hand, stack) -> CauldronInteraction.emptyBucket(
                level,
                pos,
                player,
                hand,
                stack,
                cauldron.get()
                    .defaultBlockState()
                    .setValue(Layered4LevelCauldronBlock.LEVEL, Layered4LevelCauldronBlock.MAX_LEVEL),
                SoundEvents.BUCKET_EMPTY
            )
        );
    }
}
