package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.lib.v2.registrum.util.entry.EntityEntry;
import dev.anvilcraft.pigeonplus.client.renderer.entity.LargeFireworksRocketRenderer;
import dev.anvilcraft.pigeonplus.entity.LargeFireworksRocketEntity;
import dev.anvilcraft.pigeonplus.entity.LargeFireworksRocketSeatEntity;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.world.entity.MobCategory;

import static dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus.REGISTRUM;

/**
 * 本模组的实体。
 *
 * <p>渲染器由 {@code .renderer(...)} 交给 Registrum，它内部用
 * {@code RegistrumDistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)} 注册，
 * 所以这里直接引用客户端类是安全的。
 */
public class AddonEntities {
    /** 尺寸 3×3×9，与模型轮廓一致。{@code updateInterval(1)} 是因为实体一直在加速。 */
    public static final EntityEntry<LargeFireworksRocketEntity> LARGE_FIREWORKS_ROCKET = REGISTRUM
        .<LargeFireworksRocketEntity>entity("large_fireworks_rocket", LargeFireworksRocketEntity::new, MobCategory.MISC)
        .properties(builder -> builder
            .sized(3.0F, 9.0F)
            .clientTrackingRange(16)
            .updateInterval(1)
            .fireImmune())
        .renderer(() -> LargeFireworksRocketRenderer::new)
        .register();

    /**
     * 火箭顶上的座位，让玩家能坐到火箭顶面。
     */
    public static final EntityEntry<LargeFireworksRocketSeatEntity> LARGE_FIREWORKS_ROCKET_SEAT = REGISTRUM
        .<LargeFireworksRocketSeatEntity>entity(
            "large_fireworks_rocket_seat", LargeFireworksRocketSeatEntity::new, MobCategory.MISC)
        .properties(builder -> builder
            .sized(0.5F, 0.5F)
            .clientTrackingRange(16)
            .noSummon()
            .fireImmune())
        .renderer(() -> NoopRenderer::new)
        .register();

    public static void register() {
    }
}
