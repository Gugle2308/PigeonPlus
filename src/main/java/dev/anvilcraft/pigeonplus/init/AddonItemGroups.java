package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.dubhe.anvilcraft.init.item.ModItemGroups;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus.REGISTRUM;


public class AddonItemGroups {
    private static final DeferredRegister<CreativeModeTab> DEFERRED_REGISTER = DeferredRegister.create(
        Registries.CREATIVE_MODE_TAB,
        AnvilCraftPigeonPlus.MOD_ID
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ADDON_ITEMS = DEFERRED_REGISTER.register(
        "addon_items",
        () -> CreativeModeTab.builder()
            .icon(() -> AddonBlocks.NOZZLE.asStack())
            .displayItems((ctx, entries) -> addEnchantedBooks(ctx, entries))
            .title(
                REGISTRUM.addLang(
                    "itemGroup",
                    AnvilCraftPigeonPlus.of("addon_items"),
                    "AnvilCraft: Pigeon+"
                )
            )
            .withTabsBefore(ModItemGroups.ANVILCRAFT_ITEMS.getId())
            .build()
    );

    public static void register(IEventBus modEventBus) {
        DEFERRED_REGISTER.register(modEventBus);
    }

    private static void addEnchantedBooks(
        CreativeModeTab.ItemDisplayParameters ctx, CreativeModeTab.Output entries
    ) {
        ctx.holders()
            .lookup(Registries.ENCHANTMENT)
            .flatMap(lookup -> lookup.get(AddonEnchantments.DOOMFIST_KEY))
            .ifPresent(holder -> entries.accept(
                EnchantedBookItem.createForEnchantment(new EnchantmentInstance(holder, 1))
            ));
    }
}
