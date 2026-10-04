package dev.anvilcraft.pigeonplus.multiblock;

import dev.anvilcraft.pigeonplus.block.state.LargeFireworkPart;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.StairsShape;

/**
 * 大型烟花多方块的结构定义与楼梯形状推导，供转换特判与 JEI 展示配方共用。
 */
public final class LargeFireworkStructure {
    /** 结构高度。 */
    public static final int HEIGHT = LargeFireworkPart.HEIGHT;
    /** 3×3 水平居中所需的半宽。 */
    public static final int HALF = 1;
    /** 图案中楼梯所在的层。 */
    public static final int FIN_LAYER = 7;
    public static final String[][] PATTERN = {
        {"   ", "   ", "  A"},   // y0：发射木棍
        {"   ", "   ", "  A"},   // y1
        {"BBB", "BCB", "BBB"},   // y2：箭体
        {"BBB", "BCB", "BBB"},   // y3
        {"BBB", "BCB", "BBB"},   // y4
        {"BBB", "BCB", "BBB"},   // y5
        {"BBB", "BCB", "BBB"},   // y6
        {"DDD", "ECF", "EGG"},   // y7：尾翼
        {"   ", " B ", "   "},   // y8：箭尖
    };

    /**
     * 图案中每个楼梯格的期望 {@code SHAPE}，索引 {@code [x][z]}。
     *
     * <p>这是<b>常数，与旋转无关</b>：旋转会同时改变朝向与邻居的朝向，
     * 左右形关系保持不变，所以四种朝向下这个形状都成立。
     * 特判用它判断外观，JEI 展示配方用它写出正确的模型。
     */
    private static final StairsShape[][] EXPECTED_SHAPES = buildExpectedShapes();

    private LargeFireworkStructure() {
    }

    /** 图案里 {@code (x, z)} 处楼梯的期望形状；非楼梯格返回 {@code null}。 */
    public static StairsShape expectedShape(int x, int z) {
        return EXPECTED_SHAPES[x][z];
    }

    /** 图案符号在未旋转结构（{@code facing=north}）中的基准朝向。 */
    public static Direction baseFacing(char symbol) {
        return switch (symbol) {
            case 'D' -> Direction.SOUTH;
            case 'E' -> Direction.EAST;
            case 'F' -> Direction.WEST;
            case 'G' -> Direction.NORTH;
            default -> throw new IllegalStateException("非楼梯符号: " + symbol);
        };
    }

    /** 该符号是否是楼梯。 */
    public static boolean isStair(char symbol) {
        return symbol == 'D' || symbol == 'E' || symbol == 'F' || symbol == 'G';
    }

    /**
     * 楼梯的外观朝向——即 vanilla blockstate 里决定模型 {@code y} 的方向。
     */
    public static Direction appearance(Direction facing, StairsShape shape) {
        return switch (shape) {
            case INNER_LEFT, OUTER_LEFT -> facing.getCounterClockWise();
            default -> facing;
        };
    }

    private static StairsShape[][] buildExpectedShapes() {
        StairsShape[][] shapes = new StairsShape[3][3];
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                if (isStair(PATTERN[FIN_LAYER][z].charAt(x))) {
                    shapes[x][z] = computeShape(x, z);
                }
            }
        }
        return shapes;
    }

    /**
     * 按图案空间的邻居推导形状。
     */
    private static StairsShape computeShape(int x, int z) {
        Direction dir = baseFacing(PATTERN[FIN_LAYER][z].charAt(x));
        Direction front = facingAt(x + dir.getStepX(), z + dir.getStepZ());
        if (front != null && front.getAxis() != dir.getAxis()
            && canTakeShape(x, z, dir, front.getOpposite())) {
            return front == dir.getCounterClockWise()
                ? StairsShape.OUTER_LEFT : StairsShape.OUTER_RIGHT;
        }
        Direction backDir = dir.getOpposite();
        Direction back = facingAt(x + backDir.getStepX(), z + backDir.getStepZ());
        if (back != null && back.getAxis() != dir.getAxis()
            && canTakeShape(x, z, dir, back)) {
            return back == dir.getCounterClockWise()
                ? StairsShape.INNER_LEFT : StairsShape.INNER_RIGHT;
        }
        return StairsShape.STRAIGHT;
    }

    /** 图案空间里 {@code (x, z)} 的楼梯朝向；越界或非楼梯返回 {@code null}。 */
    private static Direction facingAt(int x, int z) {
        if (x < 0 || x > 2 || z < 0 || z > 2) {
            return null;
        }
        char symbol = PATTERN[FIN_LAYER][z].charAt(x);
        return isStair(symbol) ? baseFacing(symbol) : null;
    }

    /** 复刻 vanilla {@code StairBlock.canTakeShape}，邻居取自图案空间。 */
    private static boolean canTakeShape(int x, int z, Direction dir, Direction face) {
        Direction neighbour = facingAt(x + face.getStepX(), z + face.getStepZ());
        return neighbour == null || neighbour != dir;
    }
}
