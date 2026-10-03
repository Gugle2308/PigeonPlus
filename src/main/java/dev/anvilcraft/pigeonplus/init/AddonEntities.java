package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.lib.v2.registrum.util.entry.EntityEntry;
import dev.anvilcraft.pigeonplus.client.renderer.entity.GiantFireworkRenderer;
import dev.anvilcraft.pigeonplus.entity.GiantFireworkEntity;
import net.minecraft.world.entity.MobCategory;

import static dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus.REGISTRUM;

/**
 * 本模组的实体。
 *
 * <p>实体本身在双端注册；渲染器由 {@code .renderer(...)} 交由 Registrum 处理——
 * 它内部通过 {@code RegistrumDistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)} 注册，
 * 专职服务端不会去加载 {@link GiantFireworkRenderer}，因此这里可以安全地直接引用客户端类。
 */
public class AddonEntities {
    /**
     * 巨型烟花实体的尺寸是 3×3×9，与模型轮廓一致。
     *
     * <p>{@code updateInterval(1)} 是必须的：实体每 tick 都在加速，
     * 默认 3 tick 一次的同步会让客户端看到明显的跳跃。
     */
    public static final EntityEntry<GiantFireworkEntity> GIANT_FIREWORK = REGISTRUM
        .<GiantFireworkEntity>entity("giant_firework", GiantFireworkEntity::new, MobCategory.MISC)
        .properties(builder -> builder
            .sized(3.0F, 9.0F)
            .clientTrackingRange(16)
            .updateInterval(1)
            .fireImmune())
        .renderer(() -> GiantFireworkRenderer::new)
        .register();

    public static void register() {
    }
}
