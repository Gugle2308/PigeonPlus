package dev.anvilcraft.pigeonplus.client.selection;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;

import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

public final class AddonModelSelectionDenylist {
    public static final ResourceLocation LOCATION = AnvilCraftPigeonPlus.of("model_selection.json");
    private static volatile Rules rules = Rules.EMPTY;

    private AddonModelSelectionDenylist() {
    }

    public static void reload(ResourceManager resources) {
        Rules loaded = Rules.EMPTY;
        for (Resource resource : resources.getResourceStack(LOCATION)) {
            try (Reader reader = resource.openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                loaded = new Rules(
                    readBlocks(json, "disable_precise_picking"),
                    readBlocks(json, "disable_model_outline"),
                    readBlocks(json, "disable_ber_selection"),
                    readBlocks(json, "dynamic_selection")
                );
            } catch (IOException | RuntimeException exception) {
                AnvilCraftPigeonPlus.LOGGER.warn(
                    "Unable to load {} from {}; keeping lower-priority denylist",
                    LOCATION,
                    resource.sourcePackId(),
                    exception
                );
            }
        }
        rules = loaded;
    }

    public static boolean usesOriginalOutline(Block block) {
        return rules.outline().contains(block);
    }

    public static boolean excludesBlockEntity(Block block) {
        return rules.blockEntity().contains(block);
    }

    public static Set<Block> disabledPrecisePicking() {
        Set<Block> blocks = new HashSet<>(rules.picking());
        blocks.addAll(rules.outline());
        return Set.copyOf(blocks);
    }

    public static Set<Block> dynamicSelectionBlocks() {
        Set<Block> blocks = new HashSet<>(rules.dynamic());
        blocks.removeAll(disabledPrecisePicking());
        blocks.removeAll(rules.blockEntity());
        return Set.copyOf(blocks);
    }

    private static Set<Block> readBlocks(JsonObject json, String name) {
        if (!json.has(name)) {
            return Set.of();
        }
        Set<Block> blocks = new HashSet<>();
        for (JsonElement entry : GsonHelper.getAsJsonArray(json, name)) {
            ResourceLocation id = ResourceLocation.parse(GsonHelper.convertToString(entry, name));
            if (!AnvilCraftPigeonPlus.MOD_ID.equals(id.getNamespace())) {
                throw new IllegalArgumentException(
                    "Model selection denylist only supports anvilcraft_pigeon_plus blocks: " + id);
            }
            blocks.add(BuiltInRegistries.BLOCK.getOptional(id)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Unknown block in model selection denylist: " + id)));
        }
        return Set.copyOf(blocks);
    }

    private record Rules(Set<Block> picking, Set<Block> outline, Set<Block> blockEntity, Set<Block> dynamic) {
        private static final Rules EMPTY = new Rules(Set.of(), Set.of(), Set.of(), Set.of());
    }
}
