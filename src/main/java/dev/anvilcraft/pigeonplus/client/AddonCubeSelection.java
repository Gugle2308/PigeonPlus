package dev.anvilcraft.pigeonplus.client;

import dev.anvilcraft.lib.v2.cube.client.CubeSelection;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;

/**
 * 启用 anvillib cube 模块对本模组命名空间的支持（精确拾取 + 模型形状准星描边）。
 *
 * <p>anvillib 只对显式登记过的命名空间生效（{@code ModelSelections#bake} 里有
 * {@code if (!CubeSelection.isEnabled(block)) continue;}），而 AnvilCraft 只登记了
 * {@code "anvilcraft"}。不登记则 {@code CubeSelection.active()} 为 false，
 * 精确拾取会退回原版整格拾取、准星也不画模型形状描边。
 *
 * <p>与 {@code ModelSelectionBakeryMixin} 分工不同：本类负责 anvillib 那条链路
 * （精确拾取 + 准星高亮），mixin 负责 AnvilCraft 自己的放置预览框线。
 *
 * <p>必须在 {@code ModelEvent.ModifyBakingResult} 之前调用——cube 模块在该事件里
 * 烘焙 {@code ModelSelections} 快照，晚了要等下次资源重载才生效。
 */
public final class AddonCubeSelection {
    private static boolean enabled;

    private AddonCubeSelection() {
    }

    public static void enableOwnNamespace() {
        if (enabled) {
            return;
        }
        enabled = true;
        CubeSelection.enableNamespace(AnvilCraftPigeonPlus.MOD_ID);
    }
}