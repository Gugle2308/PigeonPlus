package dev.anvilcraft.pigeonplus.fluid;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.jspecify.annotations.NonNull;

public class PotionFluidType extends FluidType {
    private static final String FLAVORED_KEY = "fluid.anvilcraft_pigeon_plus.potion.of";
    private static final String LONG_KEY = "fluid.anvilcraft_pigeon_plus.potion.long";
    private static final String STRONG_KEY = "fluid.anvilcraft_pigeon_plus.potion.strong";

    // Potion.getName 直接拼接 descriptionId 与注册名，所以结尾的点不能省。
    private static final String POTION_NAME_PREFIX = "item.minecraft.potion.effect.";

    public PotionFluidType(FluidType.Properties properties) {
        super(properties);
    }

    @Override
    public @NonNull Component getDescription(FluidStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null || contents.potion().isEmpty()) {
            return super.getDescription(stack);
        }
        Component base = Component.translatable(Potion.getName(contents.potion(), POTION_NAME_PREFIX));
        return Component.translatable(FLAVORED_KEY, variantPrefix(contents.potion().get()), base);
    }

    // 原版把变种的 Potion.name 设成基名，靠注册名前缀才能区分。
    private static Component variantPrefix(Holder<Potion> potion) {
        if (PotionVariants.isLong(potion)) {
            return Component.translatable(LONG_KEY);
        }
        if (PotionVariants.isStrong(potion)) {
            return Component.translatable(STRONG_KEY);
        }
        return Component.empty();
    }
}
