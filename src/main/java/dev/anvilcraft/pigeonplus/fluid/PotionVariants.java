package dev.anvilcraft.pigeonplus.fluid;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.alchemy.Potion;

public final class PotionVariants {
    public static final String LONG_PREFIX = "long_";
    public static final String STRONG_PREFIX = "strong_";

    private PotionVariants() {
    }

    public static String path(Holder<Potion> potion) {
        return potion.unwrapKey()
            .map(ResourceKey::location)
            .map(ResourceLocation::getPath)
            .orElse("");
    }

    public static boolean isLong(Holder<Potion> potion) {
        return path(potion).startsWith(LONG_PREFIX);
    }

    public static boolean isStrong(Holder<Potion> potion) {
        return path(potion).startsWith(STRONG_PREFIX);
    }

    // 长效与强力只改时长或等级，不改变效果本身。
    public static boolean isEnhanced(Holder<Potion> potion) {
        return isLong(potion) || isStrong(potion);
    }
}
