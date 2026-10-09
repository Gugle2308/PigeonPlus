package dev.anvilcraft.pigeonplus.integration.jei.drawable;

import dev.dubhe.anvilcraft.client.support.RenderSupport;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 只画一个 3x3 多方块的 JEI 图标。
 *
 * <p>本体 {@code DrawableBlockStateIcon} 把比例写死成 7，那是给单格方块的，
 * 3x3 多方块（大锅）按这个比例会撑爆 16x16 的图标框，所以要除以 3。
 */
public class MultiPartBlockIcon implements IDrawable {
    // 单格方块用 7 正好铺满 16x16，多方块的模型宽 3 格，所以是它的三分之一。
    private static final float SCALE = 7.0F / 3.0F;

    private final BlockState state;

    public MultiPartBlockIcon(BlockState state) {
        this.state = state;
    }

    @Override
    public int getWidth() {
        return 16;
    }

    @Override
    public int getHeight() {
        return 16;
    }

    @Override
    public void draw(GuiGraphics guiGraphics, int offsetX, int offsetY) {
        RenderSupport.renderBlock(
            guiGraphics, state, offsetX + 8, offsetY + 7, 0, SCALE, RenderSupport.SINGLE_BLOCK);
    }
}
