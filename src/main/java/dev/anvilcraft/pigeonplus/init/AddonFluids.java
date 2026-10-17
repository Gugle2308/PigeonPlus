package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.block.EvaporatingLiquidBlock;
import dev.anvilcraft.pigeonplus.block.MixedBiomassBlock;
import dev.anvilcraft.pigeonplus.fluid.GasFluid;
import dev.anvilcraft.pigeonplus.fluid.PotionFluid;
import dev.anvilcraft.pigeonplus.fluid.PotionFluidType;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.joml.Vector3f;

public class AddonFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(
        NeoForgeRegistries.FLUID_TYPES,
        AnvilCraftPigeonPlus.MOD_ID
    );
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(
        Registries.FLUID,
        AnvilCraftPigeonPlus.MOD_ID
    );
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
        Registries.BLOCK,
        AnvilCraftPigeonPlus.MOD_ID
    );

    public static final DeferredHolder<FluidType, FluidType> GASEOUS_BIOGAS_TYPE = FLUID_TYPES.register(
        "gaseous_biogas",
        () -> new FluidType(FluidType.Properties.create()
            .descriptionId("block.%s.gaseous_biogas".formatted(AnvilCraftPigeonPlus.MOD_ID))
            .density(-100)
            .viscosity(100)
            .temperature(310)
            .canDrown(false)
            .canSwim(false)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY))
    );

    public static final DeferredHolder<Fluid, GasFluid> GASEOUS_BIOGAS = FLUIDS.register(
        "gaseous_biogas",
        () -> new GasFluid(GASEOUS_BIOGAS_TYPE, AddonItems.GASEOUS_BIOGAS_BUCKET)
    );

    public static final DeferredHolder<FluidType, FluidType> LIQUEFIED_BIOGAS_TYPE = FLUID_TYPES.register(
        "liquefied_biogas",
        () -> new FluidType(FluidType.Properties.create()
            .descriptionId("block.%s.liquefied_biogas".formatted(AnvilCraftPigeonPlus.MOD_ID))
            .density(450)
            .viscosity(700)
            .temperature(112)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY))
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LIQUEFIED_BIOGAS = FLUIDS.register(
        "liquefied_biogas",
        () -> new BaseFlowingFluid.Source(liquefiedBiogasProperties())
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> LIQUEFIED_BIOGAS_FLOWING = FLUIDS.register(
        "flowing_liquefied_biogas",
        () -> new BaseFlowingFluid.Flowing(liquefiedBiogasProperties())
    );

    public static final DeferredHolder<Block, LiquidBlock> LIQUEFIED_BIOGAS_BLOCK = BLOCKS.register(
        "liquefied_biogas",
        () -> new EvaporatingLiquidBlock(LIQUEFIED_BIOGAS.get(), BlockBehaviour.Properties.of()
            .replaceable()
            .noCollission()
            .strength(100.0f)
            .pushReaction(PushReaction.DESTROY)
            .noLootTable()
            .liquid()
            .randomTicks(),
            new Vector3f(0.42f, 0.56f, 0.24f))
    );

    public static final DeferredHolder<FluidType, FluidType> COMPRESSED_AIR_TYPE = FLUID_TYPES.register(
        "compressed_air",
        () -> new FluidType(FluidType.Properties.create()
            .descriptionId("block.%s.compressed_air".formatted(AnvilCraftPigeonPlus.MOD_ID))
            .density(-100)
            .viscosity(50)
            .temperature(300)
            .canDrown(false)
            .canSwim(false)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY))
    );

    public static final DeferredHolder<Fluid, GasFluid> COMPRESSED_AIR = FLUIDS.register(
        "compressed_air",
        () -> new GasFluid(COMPRESSED_AIR_TYPE, AddonItems.COMPRESSED_AIR_BUCKET)
    );

    public static final DeferredHolder<FluidType, FluidType> MIXED_BIOMASS_TYPE = FLUID_TYPES.register(
        "mixed_biomass",
        () -> new FluidType(FluidType.Properties.create()
            .descriptionId("block.%s.mixed_biomass".formatted(AnvilCraftPigeonPlus.MOD_ID))
            .density(1200)
            .viscosity(1800)
            .temperature(305)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY))
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> MIXED_BIOMASS = FLUIDS.register(
        "mixed_biomass",
        () -> new BaseFlowingFluid.Source(mixedBiomassProperties())
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> MIXED_BIOMASS_FLOWING = FLUIDS.register(
        "flowing_mixed_biomass",
        () -> new BaseFlowingFluid.Flowing(mixedBiomassProperties())
    );

    public static final DeferredHolder<Block, MixedBiomassBlock> MIXED_BIOMASS_BLOCK = BLOCKS.register(
        "mixed_biomass",
        () -> new MixedBiomassBlock(MIXED_BIOMASS.get(), BlockBehaviour.Properties.of()
            .replaceable()
            .noCollission()
            .strength(100.0f)
            .pushReaction(PushReaction.DESTROY)
            .noLootTable()
            .liquid())
    );

    public static final DeferredHolder<FluidType, FluidType> LIQUID_OXYGEN_TYPE = FLUID_TYPES.register(
        "liquid_oxygen",
        () -> new FluidType(FluidType.Properties.create()
            .descriptionId("block.%s.liquid_oxygen".formatted(AnvilCraftPigeonPlus.MOD_ID))
            .density(1140)
            .viscosity(900)
            .temperature(90)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY))
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LIQUID_OXYGEN = FLUIDS.register(
        "liquid_oxygen",
        () -> new BaseFlowingFluid.Source(liquidOxygenProperties())
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> LIQUID_OXYGEN_FLOWING = FLUIDS.register(
        "flowing_liquid_oxygen",
        () -> new BaseFlowingFluid.Flowing(liquidOxygenProperties())
    );

    public static final DeferredHolder<Block, LiquidBlock> LIQUID_OXYGEN_BLOCK = BLOCKS.register(
        "liquid_oxygen",
        () -> new EvaporatingLiquidBlock(LIQUID_OXYGEN.get(), BlockBehaviour.Properties.of()
            .replaceable()
            .noCollission()
            .strength(100.0f)
            .pushReaction(PushReaction.DESTROY)
            .noLootTable()
            .liquid()
            .randomTicks(),
            new Vector3f(0.85f, 0.95f, 1.0f))
    );

    public static final DeferredHolder<FluidType, FluidType> LIQUID_HYDROGEN_TYPE = FLUID_TYPES.register(
        "liquid_hydrogen",
        () -> new FluidType(FluidType.Properties.create()
            .descriptionId("block.%s.liquid_hydrogen".formatted(AnvilCraftPigeonPlus.MOD_ID))
            .density(70)
            .viscosity(50)
            .temperature(20)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY))
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LIQUID_HYDROGEN = FLUIDS.register(
        "liquid_hydrogen",
        () -> new BaseFlowingFluid.Source(liquidHydrogenProperties())
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> LIQUID_HYDROGEN_FLOWING = FLUIDS.register(
        "flowing_liquid_hydrogen",
        () -> new BaseFlowingFluid.Flowing(liquidHydrogenProperties())
    );

    public static final DeferredHolder<Block, LiquidBlock> LIQUID_HYDROGEN_BLOCK = BLOCKS.register(
        "liquid_hydrogen",
        () -> new EvaporatingLiquidBlock(LIQUID_HYDROGEN.get(), BlockBehaviour.Properties.of()
            .replaceable()
            .noCollission()
            .strength(100.0f)
            .pushReaction(PushReaction.DESTROY)
            .noLootTable()
            .liquid()
            .randomTicks(),
            new Vector3f(0.78f, 0.92f, 1.0f))
    );

    public static final DeferredHolder<FluidType, FluidType> LIQUID_CHOCOLATE_TYPE = FLUID_TYPES.register(
        "liquid_chocolate",
        () -> new FluidType(FluidType.Properties.create()
            .descriptionId("block.%s.liquid_chocolate".formatted(AnvilCraftPigeonPlus.MOD_ID))
            .density(1400)
            .viscosity(3000)
            .temperature(320)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY))
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LIQUID_CHOCOLATE = FLUIDS.register(
        "liquid_chocolate",
        () -> new BaseFlowingFluid.Source(liquidChocolateProperties())
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> LIQUID_CHOCOLATE_FLOWING = FLUIDS.register(
        "flowing_liquid_chocolate",
        () -> new BaseFlowingFluid.Flowing(liquidChocolateProperties())
    );

    public static final DeferredHolder<Block, LiquidBlock> LIQUID_CHOCOLATE_BLOCK = BLOCKS.register(
        "liquid_chocolate",
        () -> new LiquidBlock(LIQUID_CHOCOLATE.get(), BlockBehaviour.Properties.of()
            .replaceable()
            .noCollission()
            .strength(100.0f)
            .pushReaction(PushReaction.DESTROY)
            .noLootTable()
            .liquid())
    );

    public static final DeferredHolder<FluidType, FluidType> LIQUID_CHOCOLATE_DARK_TYPE = FLUID_TYPES.register(
        "liquid_chocolate_dark",
        () -> chocolateFluidType("liquid_chocolate_dark")
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LIQUID_CHOCOLATE_DARK = FLUIDS.register(
        "liquid_chocolate_dark",
        () -> new BaseFlowingFluid.Source(liquidChocolateDarkProperties())
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> LIQUID_CHOCOLATE_DARK_FLOWING = FLUIDS.register(
        "flowing_liquid_chocolate_dark",
        () -> new BaseFlowingFluid.Flowing(liquidChocolateDarkProperties())
    );

    public static final DeferredHolder<Block, LiquidBlock> LIQUID_CHOCOLATE_DARK_BLOCK = BLOCKS.register(
        "liquid_chocolate_dark",
        () -> chocolateBlock(LIQUID_CHOCOLATE_DARK.get())
    );

    public static final DeferredHolder<FluidType, FluidType> LIQUID_CHOCOLATE_WHITE_TYPE = FLUID_TYPES.register(
        "liquid_chocolate_white",
        () -> chocolateFluidType("liquid_chocolate_white")
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LIQUID_CHOCOLATE_WHITE = FLUIDS.register(
        "liquid_chocolate_white",
        () -> new BaseFlowingFluid.Source(liquidChocolateWhiteProperties())
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> LIQUID_CHOCOLATE_WHITE_FLOWING = FLUIDS.register(
        "flowing_liquid_chocolate_white",
        () -> new BaseFlowingFluid.Flowing(liquidChocolateWhiteProperties())
    );

    public static final DeferredHolder<Block, LiquidBlock> LIQUID_CHOCOLATE_WHITE_BLOCK = BLOCKS.register(
        "liquid_chocolate_white",
        () -> chocolateBlock(LIQUID_CHOCOLATE_WHITE.get())
    );

    public static final DeferredHolder<FluidType, FluidType> POTION_TYPE = FLUID_TYPES.register(
        "potion",
        () -> new PotionFluidType(FluidType.Properties.create()
            .descriptionId("block.%s.potion".formatted(AnvilCraftPigeonPlus.MOD_ID))
            .density(1000)
            .viscosity(1000)
            .temperature(300)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BOTTLE_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BOTTLE_EMPTY))
    );

    public static final DeferredHolder<Fluid, PotionFluid> POTION = FLUIDS.register(
        "potion",
        PotionFluid::new
    );

    private static BaseFlowingFluid.Properties mixedBiomassProperties() {        return new BaseFlowingFluid.Properties(MIXED_BIOMASS_TYPE, MIXED_BIOMASS, MIXED_BIOMASS_FLOWING)
            .bucket(AddonItems.MIXED_BIOMASS_BUCKET)
            .block(MIXED_BIOMASS_BLOCK)
            .slopeFindDistance(3)
            .levelDecreasePerBlock(2)
            .tickRate(10)
            .explosionResistance(100.0f);
    }

    private static BaseFlowingFluid.Properties liquefiedBiogasProperties() {
        return new BaseFlowingFluid.Properties(LIQUEFIED_BIOGAS_TYPE, LIQUEFIED_BIOGAS, LIQUEFIED_BIOGAS_FLOWING)
            .bucket(AddonItems.LIQUEFIED_BIOGAS_BUCKET)
            .block(LIQUEFIED_BIOGAS_BLOCK)
            .slopeFindDistance(4)
            .levelDecreasePerBlock(1)
            .tickRate(5)
            .explosionResistance(100.0f);
    }

    private static BaseFlowingFluid.Properties liquidOxygenProperties() {
        return new BaseFlowingFluid.Properties(LIQUID_OXYGEN_TYPE, LIQUID_OXYGEN, LIQUID_OXYGEN_FLOWING)
            .bucket(AddonItems.LIQUID_OXYGEN_BUCKET)
            .block(LIQUID_OXYGEN_BLOCK)
            .slopeFindDistance(4)
            .levelDecreasePerBlock(1)
            .tickRate(5)
            .explosionResistance(100.0f);
    }

    private static BaseFlowingFluid.Properties liquidHydrogenProperties() {
        return new BaseFlowingFluid.Properties(LIQUID_HYDROGEN_TYPE, LIQUID_HYDROGEN, LIQUID_HYDROGEN_FLOWING)
            .bucket(AddonItems.LIQUID_HYDROGEN_BUCKET)
            .block(LIQUID_HYDROGEN_BLOCK)
            .slopeFindDistance(4)
            .levelDecreasePerBlock(1)
            .tickRate(5)
            .explosionResistance(100.0f);
    }

    private static FluidType chocolateFluidType(String name) {
        return new FluidType(FluidType.Properties.create()
            .descriptionId("block.%s.%s".formatted(AnvilCraftPigeonPlus.MOD_ID, name))
            .density(1400)
            .viscosity(3000)
            .temperature(320)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY));
    }

    private static LiquidBlock chocolateBlock(FlowingFluid fluid) {
        return new LiquidBlock(fluid, BlockBehaviour.Properties.of()
            .replaceable()
            .noCollission()
            .strength(100.0f)
            .pushReaction(PushReaction.DESTROY)
            .noLootTable()
            .liquid());
    }

    private static BaseFlowingFluid.Properties chocolateProperties(
        DeferredHolder<FluidType, FluidType> type,
        DeferredHolder<Fluid, BaseFlowingFluid.Source> source,
        DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing,
        DeferredHolder<Block, LiquidBlock> block,
        DeferredHolder<Item, ? extends Item> bucket
    ) {
        return new BaseFlowingFluid.Properties(type, source, flowing)
            .bucket(bucket)
            .block(block)
            .slopeFindDistance(2)
            .levelDecreasePerBlock(3)
            .tickRate(20)
            .explosionResistance(100.0f);
    }

    private static BaseFlowingFluid.Properties liquidChocolateDarkProperties() {
        return chocolateProperties(
            LIQUID_CHOCOLATE_DARK_TYPE,
            LIQUID_CHOCOLATE_DARK,
            LIQUID_CHOCOLATE_DARK_FLOWING,
            LIQUID_CHOCOLATE_DARK_BLOCK,
            AddonItems.LIQUID_CHOCOLATE_DARK_BUCKET
        );
    }

    private static BaseFlowingFluid.Properties liquidChocolateWhiteProperties() {
        return chocolateProperties(
            LIQUID_CHOCOLATE_WHITE_TYPE,
            LIQUID_CHOCOLATE_WHITE,
            LIQUID_CHOCOLATE_WHITE_FLOWING,
            LIQUID_CHOCOLATE_WHITE_BLOCK,
            AddonItems.LIQUID_CHOCOLATE_WHITE_BUCKET
        );
    }

    private static BaseFlowingFluid.Properties liquidChocolateProperties() {
        return chocolateProperties(
            LIQUID_CHOCOLATE_TYPE,
            LIQUID_CHOCOLATE,
            LIQUID_CHOCOLATE_FLOWING,
            LIQUID_CHOCOLATE_BLOCK,
            AddonItems.LIQUID_CHOCOLATE_BUCKET
        );
    }

    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
        BLOCKS.register(modEventBus);
    }
}
