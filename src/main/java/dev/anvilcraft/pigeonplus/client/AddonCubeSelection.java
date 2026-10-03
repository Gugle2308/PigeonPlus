package dev.anvilcraft.pigeonplus.client;

import dev.anvilcraft.lib.v2.cube.client.CubeSelection;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;

/**
 * 启用 anvillib cube 模块对本模组命名空间的支持（精确拾取 + 模型形状准星描边）。
 *
 * <h3>为什么必须登记</h3>
 * anvillib 的 cube 模块只对显式登记过的命名空间生效：
 * {@code ModelSelections#bake} 遍历方块时写着
 * {@code if (!CubeSelection.isEnabled(block)) continue;}，而
 * {@code isEnabled} 要求方块所属命名空间出现在 {@code NAMESPACES} 集合里
 * （或该方块单独注册过 {@code registerDynamic}）。
 *
 * <p>AnvilCraft 在客户端构造时只给自己登记了 {@code "anvilcraft"}。
 * 本模组此前没有任何登记，于是：
 * <ul>
 *   <li>{@code models} 为空 → {@code CubeSelection.active()} 为 false →
 *       {@code CubePicking#pick} 直接退回原版整格拾取，<b>精确拾取失效</b>；</li>
 *   <li>{@code searchRadius()} 为 0，且 {@code extendsBlock(state)} 恒为 false，
 *       准星也不会画模型形状的描边。</li>
 * </ul>
 *
 * <h3>与放置预览那个 mixin 的分工</h3>
 * 两者互不替代，各修一条链路：
 * <ul>
 *   <li>本类（登记命名空间）→ 走 anvillib 的 {@code CubeSelection} /
 *       {@code CubePicking} / {@code CubeHighlightEvents}，负责<b>精确拾取</b>
 *       与<b>准星高亮</b>；</li>
 *   <li>{@code ModelSelectionBakeryMixin} → 放宽 AnvilCraft 自己那份
 *       {@code ModelBlockSelection} 的命名空间写死判断，负责<b>放置预览的框线</b>
 *       （{@code LargeBlockPlacePreviewEventListener} 读的是 AnvilCraft 的
 *       {@code multipartOutline}，不是 anvillib 的 outlines）。</li>
 * </ul>
 *
 * <h3>调用时机</h3>
 * 必须在 {@code ModelEvent.ModifyBakingResult} 之前。cube 模块是在该事件里
 * （{@code CubeModelEvents#prepare}，LOWEST 优先级）烘焙 {@code ModelSelections} 快照的；
 * 登记晚了这一轮就没有本模组的几何，要等下次资源重载才生效。
 * 放在客户端模组构造里即可满足。
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