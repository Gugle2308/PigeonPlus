package dev.anvilcraft.pigeonplus.data.recipe;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.pigeonplus.block.LargeCreamyBreadRollBlock;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.world.level.block.Block;

import java.util.LinkedHashMap;
import java.util.Map;

final class LargeCreamyBreadRollDisplayRecipe {
    static final String[][] INPUT_GRID = {
        {"000", "0AA", "000"},
        {"000", "0AA", "000"},
        {"000", "0AA", "000"}
    };

    static final String[][] OUTPUT_GRID = {
        {"0AB", "CDE", "FGH"},
        {"IJK", "LMN", "OPQ"},
        {"RST", "UVW", "XYZ"}
    };

    static final Map<Character, BlockStatePredicate.Builder> INPUT_MAPPING = new LinkedHashMap<>();
    static final Map<Character, BlockStatePredicate.Builder> OUTPUT_MAPPING = new LinkedHashMap<>();

    static {
        INPUT_MAPPING.put('0', BlockStatePredicate.builder().of(ModBlocks.CAKE_BASE_BLOCK.get()));
        INPUT_MAPPING.put('A', BlockStatePredicate.builder().of(ModBlocks.CREAM_BLOCK.get()));

        Block roll = AddonBlocks.LARGE_CREAMY_BREAD_ROLL.get();
        put('0', roll, true, false, "x0_y0_z0");
        put('A', roll, true, false, "x1_y0_z0");
        put('B', roll, true, false, "x2_y0_z0");
        put('C', roll, true, false, "x0_y1_z0");
        put('D', roll, true, false, "x1_y1_z0");
        put('E', roll, true, false, "x2_y1_z0");
        put('F', roll, true, false, "x0_y2_z0");
        put('G', roll, true, false, "x1_y2_z0");
        put('H', roll, true, false, "x2_y2_z0");
        put('I', roll, true, true, "x0_y0_z0");
        put('J', roll, true, true, "x1_y0_z0");
        put('K', roll, true, true, "x2_y0_z0");
        put('L', roll, true, true, "x0_y1_z0");
        put('M', roll, true, true, "x1_y1_z0");
        put('N', roll, true, true, "x2_y1_z0");
        put('O', roll, true, true, "x0_y2_z0");
        put('P', roll, true, true, "x1_y2_z0");
        put('Q', roll, true, true, "x2_y2_z0");
        put('R', roll, false, true, "x0_y0_z0");
        put('S', roll, false, true, "x1_y0_z0");
        put('T', roll, false, true, "x2_y0_z0");
        put('U', roll, false, true, "x0_y1_z0");
        put('V', roll, false, true, "x1_y1_z0");
        put('W', roll, false, true, "x2_y1_z0");
        put('X', roll, false, true, "x0_y2_z0");
        put('Y', roll, false, true, "x1_y2_z0");
        put('Z', roll, false, true, "x2_y2_z0");
    }

    private LargeCreamyBreadRollDisplayRecipe() {
    }

    private static void put(char symbol, Block block, boolean back, boolean front, String part) {
        OUTPUT_MAPPING.put(symbol, BlockStatePredicate.builder()
            .of(block)
            .with(LargeCreamyBreadRollBlock.CONNECTED_BACK, Boolean.toString(back))
            .with(LargeCreamyBreadRollBlock.CONNECTED_FRONT, Boolean.toString(front))
            .with(LargeCreamyBreadRollBlock.FACING, "down")
            .with(LargeCreamyBreadRollBlock.PART, part));
    }
}
