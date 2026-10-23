package dev.anvilcraft.pigeonplus.client;

import dev.anvilcraft.lib.v2.cube.client.CubeSelection;
import dev.anvilcraft.lib.v2.cube.client.SelectionPart;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.client.selection.AddonModelSelectionDenylist;
import dev.dubhe.anvilcraft.client.selection.ModelBlockSelection;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class AddonCubeSelection {
    private static final AABB DYNAMIC_BOUNDS = new AABB(-2, -2, -2, 3, 3, 3);
    private static boolean enabled;
    private static boolean excluded;

    private AddonCubeSelection() {
    }

    public static void enableOwnNamespace() {
        if (enabled) {
            return;
        }
        enabled = true;
        CubeSelection.enableNamespace(AnvilCraftPigeonPlus.MOD_ID);
    }

    public static void excludeFromPrecisePicking() {
        if (excluded) {
            return;
        }
        excluded = true;
        for (Block block : AddonModelSelectionDenylist.disabledPrecisePicking()) {
            CubeSelection.exclude(block);
        }
        for (Block block : AddonModelSelectionDenylist.dynamicSelectionBlocks()) {
            CubeSelection.registerDynamic(block, DYNAMIC_BOUNDS, true, AddonCubeSelection::rendererParts);
        }
    }

    private static List<SelectionPart> rendererParts(
        ClientLevel level,
        BlockPos pos,
        BlockState state,
        float partialTick
    ) {
        BlockEntity entity = level.getBlockEntity(pos);
        return entity == null ? List.of() : ModelBlockSelection.rendererParts(entity, partialTick);
    }
}
