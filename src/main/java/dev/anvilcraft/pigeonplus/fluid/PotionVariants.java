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

    // 去掉长效/强力前缀，用于判断两个药水是不是同一种效果。
    public static String basePath(Holder<Potion> potion) {
        String path = path(potion);
        if (path.startsWith(LONG_PREFIX)) {
            return path.substring(LONG_PREFIX.length());
        }
        if (path.startsWith(STRONG_PREFIX)) {
            return path.substring(STRONG_PREFIX.length());
        }
        return path;
    }

    // 强化：结果与输入是同一种效果，只是时长或等级更高。
    // 发酵蛛眼的转化（长效夜视 -> 长效隐身）虽然也带前缀，但换了效果，不算强化。
    public static boolean isEnhancementOf(Holder<Potion> result, Holder<Potion> input) {
        return isEnhanced(result) && basePath(result).equals(basePath(input));
    }
}
