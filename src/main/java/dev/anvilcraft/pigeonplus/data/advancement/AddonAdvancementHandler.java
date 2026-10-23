package dev.anvilcraft.pigeonplus.data.advancement;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumAdvancementProvider;

public class AddonAdvancementHandler {
    public static void init(RegistrumAdvancementProvider provider) {
        provider.accept(AddonAdvancements.ROOT);
        provider.accept(AddonAdvancements.PAPER_BLOCK);
        provider.accept(AddonAdvancements.COMPRESSED_BOTTLE);
        provider.accept(AddonAdvancements.LIQUID_CHOCOLATE);
        provider.accept(AddonAdvancements.CHOCOLATE_COOKIE_PILLAR);
        provider.accept(AddonAdvancements.LARGE_CREAMY_BREAD_ROLL);
        provider.accept(AddonAdvancements.EAT_BREAD_ROLL);
        provider.accept(AddonAdvancements.FEED_SPREADER);
        provider.accept(AddonAdvancements.MIXED_BIOMASS);
        provider.accept(AddonAdvancements.GASEOUS_BIOGAS);
        provider.accept(AddonAdvancements.LIQUEFIED_BIOGAS);
        provider.accept(AddonAdvancements.ANVIL_PUMP);
        provider.accept(AddonAdvancements.COMPRESSED_AIR);
        provider.accept(AddonAdvancements.LIQUID_OXYGEN);
        provider.accept(AddonAdvancements.LIQUID_HYDROGEN);
        provider.accept(AddonAdvancements.BLENDER);
        provider.accept(AddonAdvancements.ELECTRIC_BLENDER);
        provider.accept(AddonAdvancements.STASIS_BEACON);
        provider.accept(AddonAdvancements.IGNITE_NOZZLE);
        provider.accept(AddonAdvancements.NOZZLE_EXPLOSION);
        provider.accept(AddonAdvancements.LARGE_FIREWORKS_ROCKET);
        provider.accept(AddonAdvancements.ROCKET_LAUNCH);
        provider.accept(AddonAdvancements.ROCKET_RIDER_KILLED);
        provider.accept(AddonAdvancements.ROCKET_MOON_ARRIVAL);
    }
}
