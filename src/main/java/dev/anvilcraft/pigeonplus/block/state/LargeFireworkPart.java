package dev.anvilcraft.pigeonplus.block.state;

import dev.dubhe.anvilcraft.block.state.ISimpleMultiPartBlockState;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * 巨型烟花的部件位置：3 宽 × 3 深 × 9 高，共 81 个格位。
 *
 * <p>命名规则为 {@code <段>_<y>_<方位>}，其中 {@code y} 是相对主部件（底层中心，
 * 偏移 {@code (0,0,0)}）的垂直距离 0~8，方位沿用 AnvilCraft 的 3×3 约定：
 * {@code C}=中心、{@code N}/{@code S}/{@code W}/{@code E}=四边、{@code WN}/{@code WS}/
 * {@code EN}/{@code ES}=四角。
 *
 * <p>由枚举常量顺序保证 {@link #values()} 的「底层 3×3 → 逐层向上」排列，
 * {@code getParts()} 依赖这一点来做放置与拆除。
 */
public enum LargeFireworkPart implements ISimpleMultiPartBlockState<LargeFireworkPart> {
    // Y = 0（底层，主部件所在层）
    Y0_WN(-1, 0, -1), Y0_N(0, 0, -1), Y0_EN(1, 0, -1),
    Y0_W(-1, 0, 0), Y0_C(0, 0, 0), Y0_E(1, 0, 0),
    Y0_WS(-1, 0, 1), Y0_S(0, 0, 1), Y0_ES(1, 0, 1),
    // Y = 1
    Y1_WN(-1, 1, -1), Y1_N(0, 1, -1), Y1_EN(1, 1, -1),
    Y1_W(-1, 1, 0), Y1_C(0, 1, 0), Y1_E(1, 1, 0),
    Y1_WS(-1, 1, 1), Y1_S(0, 1, 1), Y1_ES(1, 1, 1),
    // Y = 2
    Y2_WN(-1, 2, -1), Y2_N(0, 2, -1), Y2_EN(1, 2, -1),
    Y2_W(-1, 2, 0), Y2_C(0, 2, 0), Y2_E(1, 2, 0),
    Y2_WS(-1, 2, 1), Y2_S(0, 2, 1), Y2_ES(1, 2, 1),
    // Y = 3
    Y3_WN(-1, 3, -1), Y3_N(0, 3, -1), Y3_EN(1, 3, -1),
    Y3_W(-1, 3, 0), Y3_C(0, 3, 0), Y3_E(1, 3, 0),
    Y3_WS(-1, 3, 1), Y3_S(0, 3, 1), Y3_ES(1, 3, 1),
    // Y = 4
    Y4_WN(-1, 4, -1), Y4_N(0, 4, -1), Y4_EN(1, 4, -1),
    Y4_W(-1, 4, 0), Y4_C(0, 4, 0), Y4_E(1, 4, 0),
    Y4_WS(-1, 4, 1), Y4_S(0, 4, 1), Y4_ES(1, 4, 1),
    // Y = 5
    Y5_WN(-1, 5, -1), Y5_N(0, 5, -1), Y5_EN(1, 5, -1),
    Y5_W(-1, 5, 0), Y5_C(0, 5, 0), Y5_E(1, 5, 0),
    Y5_WS(-1, 5, 1), Y5_S(0, 5, 1), Y5_ES(1, 5, 1),
    // Y = 6
    Y6_WN(-1, 6, -1), Y6_N(0, 6, -1), Y6_EN(1, 6, -1),
    Y6_W(-1, 6, 0), Y6_C(0, 6, 0), Y6_E(1, 6, 0),
    Y6_WS(-1, 6, 1), Y6_S(0, 6, 1), Y6_ES(1, 6, 1),
    // Y = 7
    Y7_WN(-1, 7, -1), Y7_N(0, 7, -1), Y7_EN(1, 7, -1),
    Y7_W(-1, 7, 0), Y7_C(0, 7, 0), Y7_E(1, 7, 0),
    Y7_WS(-1, 7, 1), Y7_S(0, 7, 1), Y7_ES(1, 7, 1),
    // Y = 8（顶层）
    Y8_WN(-1, 8, -1), Y8_N(0, 8, -1), Y8_EN(1, 8, -1),
    Y8_W(-1, 8, 0), Y8_C(0, 8, 0), Y8_E(1, 8, 0),
    Y8_WS(-1, 8, 1), Y8_S(0, 8, 1), Y8_ES(1, 8, 1);

    /** 高度，供模型分段使用。 */
    public static final int HEIGHT = 9;

    private final String name;
    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;
    private LargeFireworkPart clockwise90;
    private LargeFireworkPart clockwise180;
    private LargeFireworkPart clockwise270;
    private LargeFireworkPart mirrorX;
    private LargeFireworkPart mirrorZ;

    static {
        for (LargeFireworkPart part : LargeFireworkPart.values()) {
            int x = part.offsetX;
            int y = part.offsetY;
            int z = part.offsetZ;
            part.clockwise90 = findByOffset(-z, y, x);
            part.clockwise180 = findByOffset(-x, y, -z);
            part.clockwise270 = findByOffset(z, y, -x);
            part.mirrorX = findByOffset(-x, y, z);
            part.mirrorZ = findByOffset(x, y, -z);
        }
    }

    LargeFireworkPart(int offsetX, int offsetY, int offsetZ) {
        this.name = "y" + offsetY + "_" + horizontalSuffix(offsetX, offsetZ);
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    private static String horizontalSuffix(int x, int z) {
        String vertical = z < 0 ? "n" : z > 0 ? "s" : "";
        String horizontal = x < 0 ? "w" : x > 0 ? "e" : "";
        if (vertical.isEmpty() && horizontal.isEmpty()) return "c";
        return horizontal + vertical;
    }

    /** 按偏移查部件，越界返回 {@code null}。 */
    @Nullable
    public static LargeFireworkPart findByOffset(int offsetX, int offsetY, int offsetZ) {
        return Arrays.stream(LargeFireworkPart.values())
            .filter(part -> part.offsetX == offsetX)
            .filter(part -> part.offsetY == offsetY)
            .filter(part -> part.offsetZ == offsetZ)
            .findFirst()
            .orElse(null);
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    @Override
    public int getOffsetX() {
        return this.offsetX;
    }

    @Override
    public int getOffsetY() {
        return this.offsetY;
    }

    @Override
    public int getOffsetZ() {
        return this.offsetZ;
    }

    @Override
    public Vec3i getOffset() {
        return new Vec3i(this.offsetX, this.offsetY, this.offsetZ);
    }

    public String getName() {
        return this.name;
    }

    /** 结构方块旋转时的部件换算，目标偏移不存在则回退到自身（避免返回 null 破坏状态定义）。 */
    public LargeFireworkPart rotate(Rotation rotation) {
        LargeFireworkPart result = switch (rotation) {
            case NONE -> this;
            case CLOCKWISE_90 -> this.clockwise90;
            case CLOCKWISE_180 -> this.clockwise180;
            case COUNTERCLOCKWISE_90 -> this.clockwise270;
        };
        return result == null ? this : result;
    }

    /** 结构方块镜像时的部件换算，同样对不存在的目标做了回退。 */
    public LargeFireworkPart mirror(Mirror mirror) {
        LargeFireworkPart result = switch (mirror) {
            case NONE -> this;
            case LEFT_RIGHT -> this.mirrorZ;
            case FRONT_BACK -> this.mirrorX;
        };
        return result == null ? this : result;
    }

    @Override
    public String toString() {
        return this.name;
    }

    /** 该部件所处的模型分段（0=底段 Y0-2，1=中段 Y3-5，2=顶段 Y6-8）。 */
    public int getSegment() {
        return this.offsetY / 3;
    }

    /** 该部件是否是其所在 3×3 层里承载整段模型的那一格（每段中列的中心，Y=1/4/7）。 */
    public boolean isModelAnchor() {
        return this.offsetX == 0 && this.offsetZ == 0 && this.offsetY % 3 == 1;
    }

    /**
     * 模型后缀：锚点部件返回 {@code bottom/middle/top}，其余返回 {@code part}（空模型）。
     *
     * <p>把模型选择编码在 {@code part} 属性里，AnvilCraft 的 {@code ModelSelectionBakery}
     * 遍历部件时才能解析到正确模型，多方块描边（放置预览框线）才拿得到几何。
     */
    public String getModelName() {
        if (!this.isModelAnchor()) {
            return "part";
        }
        return switch (this.getSegment()) {
            case 0 -> "bottom";
            case 1 -> "middle";
            default -> "top";
        };
    }

    /** 该部件是否为主部件（底层中心），用于碰撞箱与掉落判定。 */
    public boolean isMainPart() {
        return this.offsetX == 0 && this.offsetY == 0 && this.offsetZ == 0;
    }
}
