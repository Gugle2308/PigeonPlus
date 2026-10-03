package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.lib.v2.registrum.util.entry.EntityEntry;
import dev.anvilcraft.pigeonplus.client.renderer.entity.GiantFireworkRenderer;
import dev.anvilcraft.pigeonplus.entity.GiantFireworkEntity;
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
