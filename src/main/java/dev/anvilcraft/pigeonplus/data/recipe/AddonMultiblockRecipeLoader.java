package dev.anvilcraft.pigeonplus.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.anvilcraft.pigeonplus.multiblock.LargeFireworkStructure;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockBuilder;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockRecipe;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;

import java.util.ArrayList;
import java.util.List;

/**
 * 大型烟花的「多方块合成」配方，用于 JEI 展示。
 */
public class AddonMultiblockRecipeLoader {
    private static final char[] STAIR_SYMBOLS =
        {'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O'};

    private AddonMultiblockRecipeLoader() {
    }

    public static void init(RegistrumRecipeProvider provider) {
        // 展示图案：把每个楼梯格换成「按 (朝向, 形状) 区分」的符号。
        String[][] grid = new String[LargeFireworkStructure.HEIGHT][3];
        List<StairVariant> variants = new ArrayList<>();
        for (int y = 0; y < grid.length; y++) {
            for (int z = 0; z < 3; z++) {
                StringBuilder row = new StringBuilder();
                for (int x = 0; x < 3; x++) {
                    char symbol = LargeFireworkStructure.PATTERN[y][z].charAt(x);
                    if (!LargeFireworkStructure.isStair(symbol)) {
                        row.append(symbol);
                        continue;
                    }
                    StairVariant variant = new StairVariant(
                        LargeFireworkStructure.baseFacing(symbol),
                        LargeFireworkStructure.expectedShape(x, z)
                    );
                    int index = variants.indexOf(variant);
                    if (index < 0) {
                        index = variants.size();
                        variants.add(variant);
                    }
                    row.append(STAIR_SYMBOLS[index]);
                }
                grid[y][z] = row.toString();
            }
        }

        MultiblockBuilder builder = MultiblockRecipe.builder(AddonBlocks.LARGE_FIREWORK, 1);
        for (String[] layer : grid) {
            builder.layer(layer);
        }
        builder
            .symbol('A', BlockStatePredicate.builder()
                .of(Blocks.STRIPPED_OAK_LOG)
                .with(BlockStateProperties.AXIS, Direction.Axis.Y))
            .symbol('B', AddonBlocks.PAPER_BLOCK.get())
            .symbol('C', ModBlocks.GUNPOWDER_BLOCK.get());
        for (int i = 0; i < variants.size(); i++) {
            builder.symbol(STAIR_SYMBOLS[i], variants.get(i).builder());
        }
        builder.save(provider, AnvilCraftPigeonPlus.of("multiblock/large_firework"));
    }

    /** 一种楼梯展示状态：朝向与形状的组合。 */
    private record StairVariant(Direction facing, StairsShape shape) {
        /**
         * 构造对应的谓词 builder。
         */
        BlockStatePredicate.Builder builder() {
            return BlockStatePredicate.builder()
                .of(AddonBlocks.PAPER_BLOCK_STAIRS)
                .with(StairBlock.FACING, this.facing)
                .with(StairBlock.HALF, Half.BOTTOM)
                .with(StairBlock.SHAPE, this.shape);
        }
    }
}
