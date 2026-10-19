package dev.anvilcraft.pigeonplus.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.pigeonplus.block.state.LargeCreamyBreadRollPart;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

public class LargeCreamyBreadRollBlock
    extends FlexibleMultiPartBlock<LargeCreamyBreadRollPart, DirectionProperty, Direction>
    implements IHammerRemovable {
    public static final MapCodec<LargeCreamyBreadRollBlock> CODEC = simpleCodec(LargeCreamyBreadRollBlock::new);
    public static final EnumProperty<LargeCreamyBreadRollPart> PART =
        EnumProperty.create("part", LargeCreamyBreadRollPart.class);
    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    public static final BooleanProperty CONNECTED_FRONT = BooleanProperty.create("connected_front");
    public static final BooleanProperty CONNECTED_BACK = BooleanProperty.create("connected_back");

    public LargeCreamyBreadRollBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(PART, LargeCreamyBreadRollPart.X1_Y1_Z0)
            .setValue(FACING, Direction.NORTH)
            .setValue(CONNECTED_FRONT, false)
            .setValue(CONNECTED_BACK, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CONNECTED_FRONT, CONNECTED_BACK);
    }

    @Override
    public EnumProperty<LargeCreamyBreadRollPart> getPart() {
        return PART;
    }

    @Override
    public LargeCreamyBreadRollPart[] getParts() {
        return LargeCreamyBreadRollPart.values();
    }

    @Override
    public DirectionProperty getAdditionalProperty() {
        return FACING;
    }

    public static Direction frontFacing(Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.SOUTH;
            case SOUTH -> Direction.NORTH;
            case EAST -> Direction.WEST;
            case WEST -> Direction.EAST;
            case UP -> Direction.UP;
            case DOWN -> Direction.DOWN;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(
                FACING,
                context.getPlayer() != null && context.getPlayer().isShiftKeyDown()
                    ? context.getNearestLookingDirection().getOpposite()
                    : context.getNearestLookingDirection()
            );
    }

    @Override
    public BlockState getPlacementState(BlockPlaceContext context) {
        return this.getStateForPlacement(context);
    }

    @Override
    public void setPlacedBy(
        Level level,
        BlockPos pos,
        BlockState state,
        @Nullable LivingEntity placer,
        ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);
        for (LargeCreamyBreadRollPart part : this.getParts()) {
            BlockPos partPos = pos.offset(this.offsetFrom(state, part));
            BlockState partState = level.getBlockState(partPos);
            if (partState.is(this)) {
                level.setBlockAndUpdate(partPos, this.withConnections(level, partPos, partState));
            }
        }
    }

    @Override
    public BlockState updateShape(
        BlockState state,
        Direction directionToNeighbour,
        BlockState neighbourState,
        LevelAccessor level,
        BlockPos pos,
        BlockPos neighbourPos
    ) {
        BlockState updated = super.updateShape(
            state, directionToNeighbour, neighbourState, level, pos, neighbourPos);
        if (!updated.is(this)) {
            return updated;
        }
        Direction front = frontFacing(state.getValue(FACING));
        if (directionToNeighbour == front) {
            return updated.setValue(CONNECTED_FRONT, this.connects(neighbourState, state));
        }
        if (directionToNeighbour == front.getOpposite()) {
            return updated.setValue(CONNECTED_BACK, this.connects(neighbourState, state));
        }
        return updated;
    }

    private BlockState withConnections(Level level, BlockPos pos, BlockState state) {
        Direction front = frontFacing(state.getValue(FACING));
        return state
            .setValue(CONNECTED_FRONT, this.connects(level.getBlockState(pos.relative(front)), state))
            .setValue(CONNECTED_BACK, this.connects(level.getBlockState(pos.relative(front.getOpposite())), state));
    }

    private boolean connects(BlockState neighbourState, BlockState state) {
        return neighbourState.is(this) && neighbourState.getValue(FACING) == state.getValue(FACING);
    }

    public static String getModelName(BlockState state) {
        if (!state.getValue(PART).isMain()) {
            return "large_creamy_bread_roll_part";
        }
        boolean front = state.getValue(CONNECTED_FRONT);
        boolean back = state.getValue(CONNECTED_BACK);
        if (front && back) {
            return "large_creamy_bread_roll_part2";
        }
        if (front) {
            return "large_creamy_bread_roll_part1";
        }
        if (back) {
            return "large_creamy_bread_roll_part3";
        }
        return "large_creamy_bread_roll";
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(PART, state.getValue(PART).rotate(rotation))
            .setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(PART, state.getValue(PART).mirror(mirror))
            .setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }
}
