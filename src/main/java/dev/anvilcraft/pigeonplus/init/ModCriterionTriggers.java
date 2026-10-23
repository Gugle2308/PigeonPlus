package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.advancement.criterion.BreadRollEatenTrigger;
import dev.anvilcraft.pigeonplus.advancement.criterion.CookiePillarFilledTrigger;
import dev.anvilcraft.pigeonplus.advancement.criterion.NozzleExplosionTrigger;
import dev.anvilcraft.pigeonplus.advancement.criterion.NozzleGasActivatedTrigger;
import dev.anvilcraft.pigeonplus.advancement.criterion.RocketLaunchedTrigger;
import dev.anvilcraft.pigeonplus.advancement.criterion.RocketMoonArrivalTrigger;
import dev.anvilcraft.pigeonplus.advancement.criterion.RocketRiderKilledTrigger;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCriterionTriggers {
    private static final DeferredRegister<CriterionTrigger<?>> REGISTER =
        DeferredRegister.create(Registries.TRIGGER_TYPE, AnvilCraftPigeonPlus.MOD_ID);

    public static final DeferredHolder<CriterionTrigger<?>, NozzleExplosionTrigger> NOZZLE_EXPLOSION =
        REGISTER.register("nozzle_explosion", NozzleExplosionTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, NozzleGasActivatedTrigger> NOZZLE_GAS_ACTIVATED =
        REGISTER.register("nozzle_gas_activated", NozzleGasActivatedTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, BreadRollEatenTrigger> BREAD_ROLL_EATEN =
        REGISTER.register("bread_roll_eaten", BreadRollEatenTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, RocketLaunchedTrigger> ROCKET_LAUNCHED =
        REGISTER.register("rocket_launched", RocketLaunchedTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, CookiePillarFilledTrigger> COOKIE_PILLAR_FILLED =
        REGISTER.register("cookie_pillar_filled", CookiePillarFilledTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, RocketMoonArrivalTrigger> ROCKET_MOON_ARRIVAL =
        REGISTER.register("rocket_moon_arrival", RocketMoonArrivalTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, RocketRiderKilledTrigger> ROCKET_RIDER_KILLED =
        REGISTER.register("rocket_rider_killed", RocketRiderKilledTrigger::new);

    private ModCriterionTriggers() {
    }

    public static void register(IEventBus eventBus) {
        REGISTER.register(eventBus);
    }
}
