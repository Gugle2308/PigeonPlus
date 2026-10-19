package dev.anvilcraft.pigeonplus.block.state;

import dev.dubhe.anvilcraft.block.state.IFlexibleMultiPartBlockState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public enum LargeCreamyBreadRollPart implements IFlexibleMultiPartBlockState<LargeCreamyBreadRollPart, Direction> {
    X0_Y0_Z0("x0_y0_z0", -1, -1), X1_Y0_Z0("x1_y0_z0", 0, -1), X2_Y0_Z0("x2_y0_z0", 1, -1),
    X0_Y1_Z0("x0_y1_z0", -1, 0), X1_Y1_Z0("x1_y1_z0", 0, 0), X2_Y1_Z0("x2_y1_z0", 1, 0),
    X0_Y2_Z0("x0_y2_z0", -1, 1), X1_Y2_Z0("x1_y2_z0", 0, 1), X2_Y2_Z0("x2_y2_z0", 1, 1);

    private final String name;
    private final int gridX;
    private final int gridY;
    private LargeCreamyBreadRollPart clockwise90;
    private LargeCreamyBreadRollPart clockwise180;
    private LargeCreamyBreadRollPart clockwise270;
    private LargeCreamyBreadRollPart mirrorX;
    private LargeCreamyBreadRollPart mirrorZ;

    static {
        for (LargeCreamyBreadRollPart part : values()) {
            int x = part.gridX;
            int y = part.gridY;
            part.clockwise90 = findByGrid(-y, x);
            part.clockwise180 = findByGrid(-x, -y);
            part.clockwise270 = findByGrid(y, -x);
            part.mirrorX = findByGrid(-x, y);
            part.mirrorZ = findByGrid(x, -y);
        }
    }

    LargeCreamyBreadRollPart(String name, int gridX, int gridY) {
        this.name = name;
        this.gridX = gridX;
        this.gridY = gridY;
    }

    @Nullable
    private static LargeCreamyBreadRollPart findByGrid(int gridX, int gridY) {
        return Arrays.stream(values())
            .filter(part -> part.gridX == gridX)
            .filter(part -> part.gridY == gridY)
            .findFirst()
            .orElse(null);
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    @Override
    public int getOffsetX(Direction facing) {
        return switch (facing) {
            case NORTH, SOUTH, UP, DOWN -> this.gridX;
            case EAST, WEST -> 0;
        };
    }

    @Override
    public int getOffsetY(Direction facing) {
        return switch (facing) {
            case NORTH, SOUTH, EAST, WEST -> this.gridY;
            case UP, DOWN -> 0;
        };
    }

    @Override
    public int getOffsetZ(Direction facing) {
        return switch (facing) {
            case NORTH, SOUTH -> 0;
            case EAST -> this.gridX;
            case WEST -> -this.gridX;
            case UP -> -this.gridY;
            case DOWN -> this.gridY;
        };
    }

    @Override
    public boolean isMain() {
        return this.gridX == 0 && this.gridY == 0;
    }

    public String getName() {
        return this.name;
    }

    public int getGridX() {
        return this.gridX;
    }

    public int getGridY() {
        return this.gridY;
    }

    public LargeCreamyBreadRollPart rotate(Rotation rotation) {
        LargeCreamyBreadRollPart result = switch (rotation) {
            case NONE -> this;
            case CLOCKWISE_90 -> this.clockwise90;
            case CLOCKWISE_180 -> this.clockwise180;
            case COUNTERCLOCKWISE_90 -> this.clockwise270;
        };
        return result == null ? this : result;
    }

    public LargeCreamyBreadRollPart mirror(Mirror mirror) {
        LargeCreamyBreadRollPart result = switch (mirror) {
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
}
