package dev.anvilcraft.pigeonplus.client;

import dev.anvilcraft.lib.v2.cube.client.CubeSelection;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;

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
    private static boolean excluded;

    private AddonCubeSelection() {
    }

    /**
     * 登记本模组命名空间。
     *
     * <p>只处理命名空间，因为它在模组构造期就能调用（不需要方块实例）；
     * 需要方块实例的黑名单见 {@link #excludeFromPrecisePicking()}。
     *
     * <p>必须早于 {@code ModelEvent.ModifyBakingResult}——cube 模块在该事件里烘焙快照。
     */
    public static void enableOwnNamespace() {
        if (enabled) {
            return;
        }
        enabled = true;
        CubeSelection.enableNamespace(AnvilCraftPigeonPlus.MOD_ID);
    }

    /**
     * 把个别方块列入精确拾取黑名单。
     *
     * <p><b>不能在模组构造期调用</b>：{@code AddonBlocks.LARGE_FIREWORKS_ROCKET.get()} 在方块注册完成前
     * 会抛 {@code NullPointerException: Trying to access unbound value}。
     * 因此由 {@code ModelEvent.ModifyBakingResult} 的 HIGHEST 监听调用——
     * cube 模块的烘焙在同事件的 LOWEST（见其 {@code CubeModelEvents#prepare}），
     * 所以此处必定先于烘焙执行。
     *
     * <p>用 anvillib 自带的 {@link CubeSelection#exclude}：它在 {@code isEnabled} 里
     * <b>先于</b>命名空间判断被检查，所以即使命名空间已登记，被排除的方块仍会退回原版整格拾取。
     */
    public static void excludeFromPrecisePicking() {
        if (excluded) {
            return;
        }
        excluded = true;
        // 巨型烟花是 3×3×9 的整体结构，逐格精确拾取反而难以点到想要的位置，
        // 因此退回原版整格拾取。
        CubeSelection.exclude(AddonBlocks.LARGE_FIREWORKS_ROCKET.get());
    }
}