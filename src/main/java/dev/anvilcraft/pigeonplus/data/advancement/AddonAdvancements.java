package dev.anvilcraft.pigeonplus.data.advancement;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.advancement.criterion.BreadRollEatenTrigger;
import dev.anvilcraft.pigeonplus.advancement.criterion.CookiePillarFilledTrigger;
import dev.anvilcraft.pigeonplus.advancement.criterion.NozzleExplosionTrigger;
import dev.anvilcraft.pigeonplus.advancement.criterion.NozzleGasActivatedTrigger;
import dev.anvilcraft.pigeonplus.advancement.criterion.RocketLaunchedTrigger;
import dev.anvilcraft.pigeonplus.advancement.criterion.RocketMoonArrivalTrigger;
import dev.anvilcraft.pigeonplus.advancement.criterion.RocketRiderKilledTrigger;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.anvilcraft.pigeonplus.init.AddonItems;
import dev.dubhe.anvilcraft.advancements.criterion.InWorldRecipeTrigger;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemUsedOnLocationTrigger;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public class AddonAdvancements {
    public static final AdvancementHolder ROOT;
    public static final AdvancementHolder PAPER_BLOCK;
    public static final AdvancementHolder COMPRESSED_BOTTLE;
    public static final AdvancementHolder LIQUID_CHOCOLATE;
    public static final AdvancementHolder CHOCOLATE_COOKIE_PILLAR;
    public static final AdvancementHolder LARGE_CREAMY_BREAD_ROLL;
    public static final AdvancementHolder EAT_BREAD_ROLL;
    public static final AdvancementHolder FEED_SPREADER;
    public static final AdvancementHolder MIXED_BIOMASS;
    public static final AdvancementHolder GASEOUS_BIOGAS;
    public static final AdvancementHolder LIQUEFIED_BIOGAS;
    public static final AdvancementHolder ANVIL_PUMP;
    public static final AdvancementHolder COMPRESSED_AIR;
    public static final AdvancementHolder LIQUID_OXYGEN;
    public static final AdvancementHolder LIQUID_HYDROGEN;
    public static final AdvancementHolder BLENDER;
    public static final AdvancementHolder ELECTRIC_BLENDER;
    public static final AdvancementHolder STASIS_BEACON;
    public static final AdvancementHolder IGNITE_NOZZLE;
    public static final AdvancementHolder NOZZLE_EXPLOSION;
    public static final AdvancementHolder LARGE_FIREWORKS_ROCKET;
    public static final AdvancementHolder ROCKET_LAUNCH;
    public static final AdvancementHolder ROCKET_MOON_ARRIVAL;
    public static final AdvancementHolder ROCKET_RIDER_KILLED;

    static {
        ROOT = Advancement.Builder.advancement()
            .display(
                AddonBlocks.PIGEON_ANVIL.get(),
                AddonAdvancements.text("root", "title"),
                AddonAdvancements.text("root", "description"),
                ResourceLocation.fromNamespaceAndPath("anvilcraft", "textures/gui/misc/background/advancement.png"),
                AdvancementType.TASK,
                false,
                true,
                false
            )
            .addCriterion("join", PlayerTrigger.TriggerInstance.tick())
            .build(AnvilCraftPigeonPlus.of("root"));

        PAPER_BLOCK = AddonAdvancements.build(
            ROOT, "paper_block", AddonBlocks.PAPER_BLOCK.get(), AdvancementType.TASK,
            InventoryChangeTrigger.TriggerInstance.hasItems(AddonBlocks.PAPER_BLOCK.get())
        );
        COMPRESSED_BOTTLE = AddonAdvancements.build(
            PAPER_BLOCK, "compressed_bottle", AddonItems.SPLASH_BOTTLE.get(), AdvancementType.TASK,
            InventoryChangeTrigger.TriggerInstance.hasItems(AddonItems.SPLASH_BOTTLE.get())
        );

        LIQUID_CHOCOLATE = AddonAdvancements.build(
            ROOT, "liquid_chocolate", AddonItems.LIQUID_CHOCOLATE_BUCKET.get(), AdvancementType.TASK,
            InWorldRecipeTrigger.TriggerInstance.inWorldRecipeType(AnvilCraftPigeonPlus.of("blending"))
        );
        CHOCOLATE_COOKIE_PILLAR = AddonAdvancements.build(
            LIQUID_CHOCOLATE, "chocolate_cookie_pillar", ModBlocks.COOKIE_PILLAR.get(), AdvancementType.TASK,
            CookiePillarFilledTrigger.TriggerInstance.filled()
        );
        LARGE_CREAMY_BREAD_ROLL = AddonAdvancements.build(
            LIQUID_CHOCOLATE, "large_creamy_bread_roll", AddonBlocks.LARGE_CREAMY_BREAD_ROLL.get(),
            AdvancementType.GOAL,
            InventoryChangeTrigger.TriggerInstance.hasItems(AddonBlocks.LARGE_CREAMY_BREAD_ROLL.get())
        );
        EAT_BREAD_ROLL = AddonAdvancements.build(
            LARGE_CREAMY_BREAD_ROLL, "eat_bread_roll", Items.IRON_SHOVEL, AdvancementType.TASK,
            BreadRollEatenTrigger.TriggerInstance.eaten()
        );
        FEED_SPREADER = AddonAdvancements.build(
            EAT_BREAD_ROLL, "feed_spreader", AddonBlocks.FEED_SPREADER.get(), AdvancementType.TASK,
            ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(AddonBlocks.FEED_SPREADER.get())
        );

        MIXED_BIOMASS = AddonAdvancements.build(
            ROOT, "mixed_biomass", AddonItems.MIXED_BIOMASS_BUCKET.get(), AdvancementType.TASK,
            InventoryChangeTrigger.TriggerInstance.hasItems(AddonItems.MIXED_BIOMASS_BUCKET.get())
        );
        GASEOUS_BIOGAS = AddonAdvancements.build(
            MIXED_BIOMASS, "gaseous_biogas", AddonItems.GASEOUS_BIOGAS_BUCKET.get(), AdvancementType.TASK,
            InventoryChangeTrigger.TriggerInstance.hasItems(AddonItems.GASEOUS_BIOGAS_BUCKET.get())
        );
        LIQUEFIED_BIOGAS = AddonAdvancements.build(
            GASEOUS_BIOGAS, "liquefied_biogas", AddonItems.LIQUEFIED_BIOGAS_BUCKET.get(), AdvancementType.GOAL,
            InventoryChangeTrigger.TriggerInstance.hasItems(AddonItems.LIQUEFIED_BIOGAS_BUCKET.get())
        );

        ANVIL_PUMP = AddonAdvancements.build(
            ROOT, "anvil_pump", AddonBlocks.ANVIL_PUMP.get(), AdvancementType.TASK,
            ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(AddonBlocks.ANVIL_PUMP.get())
        );

        COMPRESSED_AIR = AddonAdvancements.build(
            ANVIL_PUMP, "compressed_air", AddonItems.COMPRESSED_AIR_BUCKET.get(), AdvancementType.TASK,
            InventoryChangeTrigger.TriggerInstance.hasItems(AddonItems.COMPRESSED_AIR_BUCKET.get())
        );
        LIQUID_OXYGEN = AddonAdvancements.build(
            COMPRESSED_AIR, "liquid_oxygen", AddonItems.LIQUID_OXYGEN_BUCKET.get(), AdvancementType.GOAL,
            InventoryChangeTrigger.TriggerInstance.hasItems(AddonItems.LIQUID_OXYGEN_BUCKET.get())
        );
        LIQUID_HYDROGEN = AddonAdvancements.build(
            LIQUID_OXYGEN, "liquid_hydrogen", AddonItems.LIQUID_HYDROGEN_BUCKET.get(), AdvancementType.GOAL,
            InventoryChangeTrigger.TriggerInstance.hasItems(AddonItems.LIQUID_HYDROGEN_BUCKET.get())
        );

        BLENDER = AddonAdvancements.build(
            ANVIL_PUMP, "blender", AddonBlocks.BLENDER.get(), AdvancementType.TASK,
            ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(AddonBlocks.BLENDER.get())
        );
        ELECTRIC_BLENDER = AddonAdvancements.build(
            BLENDER, "electric_blender", AddonBlocks.ELECTRIC_BLENDER.get(), AdvancementType.TASK,
            ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(AddonBlocks.ELECTRIC_BLENDER.get())
        );
        STASIS_BEACON = AddonAdvancements.build(
            BLENDER, "stasis_beacon", AddonBlocks.STASIS_BEACON.get(), AdvancementType.TASK,
            ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(AddonBlocks.STASIS_BEACON.get())
        );

        IGNITE_NOZZLE = AddonAdvancements.build(
            LIQUID_HYDROGEN, "nozzle_ignition", AddonBlocks.NOZZLE.get(), AdvancementType.TASK,
            NozzleGasActivatedTrigger.TriggerInstance.activate()
        );
        NOZZLE_EXPLOSION = AddonAdvancements.build(
            IGNITE_NOZZLE, "nozzle_explosion", AddonBlocks.NOZZLE.get(), AdvancementType.CHALLENGE,
            NozzleExplosionTrigger.TriggerInstance.exploded()
        );
        LARGE_FIREWORKS_ROCKET = AddonAdvancements.build(
            LIQUID_HYDROGEN, "large_fireworks_rocket", AddonBlocks.LARGE_FIREWORKS_ROCKET.get(),
            AdvancementType.GOAL,
            InventoryChangeTrigger.TriggerInstance.hasItems(AddonBlocks.LARGE_FIREWORKS_ROCKET.get())
        );
        ROCKET_LAUNCH = AddonAdvancements.build(
            LARGE_FIREWORKS_ROCKET, "rocket_launch", Items.FIREWORK_ROCKET, AdvancementType.CHALLENGE,
            RocketLaunchedTrigger.TriggerInstance.launched()
        );
        ROCKET_RIDER_KILLED = AddonAdvancements.build(
            ROCKET_LAUNCH, "rocket_rider_killed", Items.TNT_MINECART, AdvancementType.CHALLENGE,
            RocketRiderKilledTrigger.TriggerInstance.killed()
        );
        ROCKET_MOON_ARRIVAL = AddonAdvancements.build(
            ROCKET_LAUNCH, "rocket_moon_arrival", Items.ENDER_EYE, AdvancementType.CHALLENGE,
            RocketMoonArrivalTrigger.TriggerInstance.arrived()
        );
    }

    private AddonAdvancements() {
    }

    private static AdvancementHolder build(
        AdvancementHolder parent,
        String id,
        ItemLike icon,
        AdvancementType frame,
        Criterion<?> criterion
    ) {
        return Advancement.Builder.advancement()
            .parent(parent)
            .display(
                icon,
                AddonAdvancements.text(id, "title"),
                AddonAdvancements.text(id, "description"),
                null,
                frame,
                true,
                true,
                false
            )
            .addCriterion(id, criterion)
            .build(AnvilCraftPigeonPlus.of(id));
    }

    private static Component text(String id, String part) {
        return Component.translatable("advancements.anvilcraft_pigeon_plus." + id + "." + part);
    }
}
