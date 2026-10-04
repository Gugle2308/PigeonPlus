package dev.anvilcraft.pigeonplus.event;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.block.LargeFireworksRocketBlock;
import dev.anvilcraft.pigeonplus.block.state.LargeFireworksRocketPart;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.anvilcraft.pigeonplus.multiblock.LargeFireworksRocketStructure;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.api.event.GiantAnvilEvent;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.AnvilUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;

import java.util.List;

/**
 * 大型烟花的多方块转换／合成（特判）。
 * {@code mainPos.offset(x - 1, y, z - 1)}。
 */
@EventBusSubscriber(modid = AnvilCraftPigeonPlus.MOD_ID)
public class LargeFireworksRocketConversionEventListener {
    private static final int HEIGHT = LargeFireworksRocketStructure.HEIGHT;
    private static final int HALF = LargeFireworksRocketStructure.HALF;
    private static final String[][] PATTERN = LargeFireworksRocketStructure.PATTERN;
    /** 图案中楼梯所在的层。 */
    private static final int FIN_LAYER = 7;

    private LargeFireworksRocketConversionEventListener() {
    }

    /** 巨型铁砧砸下时检查正下方结构；四个水平朝向都试，玩家搭反也能触发。 */
    @SubscribeEvent
    public static void onGiantAnvilMultiblock(GiantAnvilEvent.Multiblock event) {
        AnvilEvent.GiantOnLand landing = event.getLanding();
        Level level = landing.getLevel();
        if (level.isClientSide()) {
            return;
        }
        BlockPos centerPos = landing.getPos().below(2);
        BlockState center = level.getBlockState(centerPos);
        // 工作台 => 就地转换；空间超压机 => 多方块合成的掉落物形式。
        boolean craft = center.is(ModBlocks.SPACE_OVERCOMPRESSOR.get());
        if (!craft && !center.is(Tags.Blocks.PLAYER_WORKSTATIONS_CRAFTING_TABLES)) {
            return;
        }
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            if (!matches(level, centerPos, rotationFor(facing))) {
                continue;
            }
            if (craft) {
                craftItem(level, centerPos);
            } else {
                placeFirework(level, centerPos, facing);
            }
            // 已接管，阻止 AnvilCraft 再跑一遍它自己的匹配与转化。
            event.setCanceled(true);
            return;
        }
    }

    /**
     * 逐格核对结构。
     */
    private static boolean matches(Level level, BlockPos centerPos, Rotation rotation) {
        BlockPos mainPos = mainPos(centerPos);
        for (int y = 0; y < HEIGHT; y++) {
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 3; x++) {
                    char symbol = PATTERN[y][z].charAt(x);
                    Vec3i offset = rotateOffset(rotation, x - HALF, y, z - HALF);
                    BlockPos pos = mainPos.offset(offset);
                    BlockState actual = level.getBlockState(pos);
                    if (symbol == ' ') {
                        if (!actual.isAir()) {
                            return false;
                        }
                    } else if (isStair(symbol)) {
                        if (!matchesStair(actual, rotation, symbol, x, z)) {
                            return false;
                        }
                    } else if (!accepts(symbol, actual)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean isStair(char symbol) {
        return LargeFireworksRocketStructure.isStair(symbol);
    }

    /**
     * 楼梯格是否与图案相符——<b>比对的是外观
     */
    private static boolean matchesStair(BlockState actual, Rotation rotation, char symbol, int x, int z) {
        if (!actual.is(AddonBlocks.PAPER_BLOCK_STAIRS.get())
            || actual.getValue(StairBlock.HALF) != Half.BOTTOM) {
            return false;
        }
        Direction expected = LargeFireworksRocketStructure.appearance(
            rotation.rotate(LargeFireworksRocketStructure.baseFacing(symbol)),
            LargeFireworksRocketStructure.expectedShape(x, z)
        );
        Direction got = LargeFireworksRocketStructure.appearance(
            actual.getValue(StairBlock.FACING), actual.getValue(StairBlock.SHAPE)
        );
        return expected == got;
    }

    private static boolean accepts(char symbol, BlockState actual) {
        return switch (symbol) {
            case 'A' -> actual.is(Blocks.STRIPPED_OAK_LOG)
                && actual.getValue(BlockStateProperties.AXIS) == Direction.Axis.Y;
            case 'B' -> actual.is(AddonBlocks.PAPER_BLOCK.get());
            case 'C' -> actual.is(ModBlocks.GUNPOWDER_BLOCK.get());
            default -> throw new IllegalStateException("未知符号: " + symbol);
        };
    }

    /**
     * 工作台中心：就地放置完整的 3×3×9 大型烟花。
     */
    private static void placeFirework(Level level, BlockPos centerPos, Direction facing) {
        BlockPos mainPos = mainPos(centerPos);
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        for (LargeFireworksRocketPart part : LargeFireworksRocketPart.values()) {
            BlockState state = AddonBlocks.LARGE_FIREWORKS_ROCKET.get().defaultBlockState()
                .setValue(LargeFireworksRocketBlock.PART, part)
                .setValue(LargeFireworksRocketBlock.FACING, facing);
            level.setBlock(mainPos.offset(part.getOffset()), state, flags);
        }
        for (BlockPos pos : fireworkBounds(mainPos)) {
            level.updateNeighborsAt(pos, AddonBlocks.LARGE_FIREWORKS_ROCKET.get());
        }
        playSound(level, centerPos);
    }

    /** 空间超压机中心：消耗结构并掉落大型烟花物品，即「多方块合成」的产物形式。 */
    private static void craftItem(Level level, BlockPos centerPos) {
        BlockPos mainPos = mainPos(centerPos);
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int y = 0; y < HEIGHT; y++) {
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 3; x++) {
                    if (PATTERN[y][z].charAt(x) != ' ') {
                        level.setBlock(mainPos.offset(x - HALF, y, z - HALF), air, flags);
                    }
                }
            }
        }
        // 掉落点必须避开巨型铁砧：铁砧占据 pos±1，即 centerPos.above() 那一层，
        // 物品生成在实心方块里会被挤出去（看起来就是「被挤飞」）。
        // 这里照搬 AnvilCraft 的 MultiblockRecipe#assemble：
        // base.relative(Axis.Y, -size / 2).getCenter()，也就是掉在结构的中部。
        AnvilUtil.dropItems(
            List.of(new ItemStack(AddonBlocks.LARGE_FIREWORKS_ROCKET)),
            level,
            centerPos.relative(Direction.Axis.Y, -HEIGHT / 2).getCenter()
        );
        playSound(level, centerPos);
    }

    private static void playSound(Level level, BlockPos centerPos) {
        level.playSound(null, centerPos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    /** 结构覆盖的全部 81 格。 */
    private static Iterable<BlockPos> fireworkBounds(BlockPos mainPos) {
        return BlockPos.betweenClosed(
            mainPos.offset(-HALF, 0, -HALF),
            mainPos.offset(HALF, HEIGHT - 1, HALF)
        );
    }

    /** 主部件（底层中心）：结构顶部紧贴中心方块下方。 */
    private static BlockPos mainPos(BlockPos centerPos) {
        return centerPos.below(HEIGHT);
    }

    /** 朝向到水平旋转，约定同原版熔炉（{@code east} 即 blockstate 的 {@code y=90}）。 */
    private static Rotation rotationFor(Direction facing) {
        return switch (facing) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /**
     * 按水平旋转换算偏移，Y 不变。
     *
     * <p>公式须与 {@link LargeFireworksRocketPart} 的静态换算同手性（{@code clockwise90 = (-z, x)}），
     * 这样匹配用的旋转与写进 {@code FACING} 的朝向才是同一套。
     */
    private static Vec3i rotateOffset(Rotation rotation, int x, int y, int z) {
        return switch (rotation) {
            case CLOCKWISE_90 -> new Vec3i(-z, y, x);
            case CLOCKWISE_180 -> new Vec3i(-x, y, -z);
            case COUNTERCLOCKWISE_90 -> new Vec3i(z, y, -x);
            default -> new Vec3i(x, y, z);
        };
    }
}
