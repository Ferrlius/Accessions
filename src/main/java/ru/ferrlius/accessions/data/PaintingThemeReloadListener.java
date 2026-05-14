package ru.ferrlius.accessions.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Rarity;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PaintingThemeReloadListener extends SimplePreparableReloadListener<PaintingThemeReloadListener.PreparedData> {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    protected PreparedData prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        PreparedData preparedData = new PreparedData();
        loadPaintings(resourceManager, preparedData);
        loadThemePlacements(resourceManager, preparedData);
        return preparedData;
    }

    @Override
    protected void apply(PreparedData preparedData, ResourceManager resourceManager, ProfilerFiller profiler) {
        PaintingThemeManager.clear();
        preparedData.paintings.forEach(PaintingThemeManager::registerPainting);
        PaintingThemeManager.registerPlacements(preparedData.placements);
        PaintingThemeManager.rebuildThemes();
    }

    private void loadPaintings(ResourceManager resourceManager, PreparedData preparedData) {
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                "unique_paintings",
                path -> path.getPath().endsWith(".json")
        );

        for (var entry : resources.entrySet()) {
            try (var reader = new InputStreamReader(entry.getValue().open())) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray paintings = json.getAsJsonArray("paintings");
                if (paintings == null) {
                    continue;
                }

                for (JsonElement element : paintings) {
                    PaintingDefinition definition = parsePainting(element, entry.getKey());
                    if (definition != null) {
                        preparedData.paintings.add(definition);
                    }
                }
            } catch (Exception exception) {
                LOGGER.error("Failed to load painting registry {}", entry.getKey(), exception);
            }
        }
    }

    private void loadThemePlacements(ResourceManager resourceManager, PreparedData preparedData) {
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                "painting_themes",
                path -> path.getPath().endsWith(".json")
        );

        for (var entry : resources.entrySet()) {
            try (var reader = new InputStreamReader(entry.getValue().open())) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray themes = json.getAsJsonArray("painting_themes");
                if (themes == null) {
                    continue;
                }

                for (JsonElement element : themes) {
                    preparedData.placements.addAll(parseThemePlacements(element, entry.getKey()));
                }
            } catch (Exception exception) {
                LOGGER.error("Failed to load painting themes {}", entry.getKey(), exception);
            }
        }
    }

    private PaintingDefinition parsePainting(JsonElement element, ResourceLocation fileId) {
        if (!element.isJsonObject()) {
            LOGGER.warn("Skipping malformed painting entry in {}", fileId);
            return null;
        }

        JsonObject object = element.getAsJsonObject();
        if (!object.has("id")) {
            LOGGER.warn("Skipping painting entry without id in {}", fileId);
            return null;
        }

        ResourceLocation variantId = ResourceLocation.parse(object.get("id").getAsString());
        int weight = object.has("weight") ? object.get("weight").getAsInt() : 1;
        if (weight <= 0) {
            LOGGER.warn("Skipping non-positive weighted painting {} in {}", variantId, fileId);
            return null;
        }

        Rarity rarity = object.has("rarity")
                ? parseRarity(object.get("rarity").getAsString(), fileId, variantId)
                : Rarity.COMMON;
        List<ResourceLocation> themes = parseThemeList(object.get("themes"), fileId, variantId);
        return new PaintingDefinition(variantId, weight, rarity, themes);
    }

    private List<ResourceLocation> parseThemeList(JsonElement element, ResourceLocation fileId, ResourceLocation variantId) {
        if (element == null || !element.isJsonArray()) {
            LOGGER.warn("Painting {} in {} has no themes array", variantId, fileId);
            return List.of();
        }

        LinkedHashSet<ResourceLocation> themes = new LinkedHashSet<>();
        for (JsonElement themeElement : element.getAsJsonArray()) {
            if (themeElement.isJsonPrimitive()) {
                themes.add(ResourceLocation.parse(themeElement.getAsString()));
                continue;
            }

            if (themeElement.isJsonObject() && themeElement.getAsJsonObject().has("theme")) {
                themes.add(ResourceLocation.parse(themeElement.getAsJsonObject().get("theme").getAsString()));
                continue;
            }

            LOGGER.warn("Skipping malformed theme reference for painting {} in {}", variantId, fileId);
        }

        return List.copyOf(themes);
    }

    private List<PaintingThemePlacement> parseThemePlacements(JsonElement element, ResourceLocation fileId) {
        if (!element.isJsonObject()) {
            LOGGER.warn("Skipping malformed painting theme entry in {}", fileId);
            return List.of();
        }

        JsonObject object = element.getAsJsonObject();
        if (!object.has("theme")) {
            LOGGER.warn("Skipping painting theme entry without theme in {}", fileId);
            return List.of();
        }

        ResourceLocation theme = ResourceLocation.parse(object.get("theme").getAsString());
        JsonArray lootTables = object.getAsJsonArray("loot_tables");
        if (lootTables == null) {
            return List.of();
        }

        List<PaintingThemePlacement> placements = new ArrayList<>();
        for (JsonElement lootTableElement : lootTables) {
            if (!lootTableElement.isJsonObject()) {
                LOGGER.warn("Skipping malformed loot table placement for {} in {}", theme, fileId);
                continue;
            }

            JsonObject placement = lootTableElement.getAsJsonObject();
            if (!placement.has("loot_table_id") || !placement.has("chance")) {
                LOGGER.warn("Skipping incomplete loot table placement for {} in {}", theme, fileId);
                continue;
            }

            float chance = placement.get("chance").getAsFloat();
            if (chance <= 0.0F) {
                continue;
            }

            placements.add(new PaintingThemePlacement(
                    theme,
                    ResourceLocation.parse(placement.get("loot_table_id").getAsString()),
                    chance
            ));
        }

        return placements;
    }

    private Rarity parseRarity(String rarityName, ResourceLocation fileId, ResourceLocation variantId) {
        return switch (rarityName.toLowerCase(Locale.ROOT)) {
            case "common" -> Rarity.COMMON;
            case "uncommon" -> Rarity.UNCOMMON;
            case "rare" -> Rarity.RARE;
            case "epic" -> Rarity.EPIC;
            default -> {
                LOGGER.warn("Unknown rarity '{}' for painting {} in {}, defaulting to common", rarityName, variantId, fileId);
                yield Rarity.COMMON;
            }
        };
    }

    protected static final class PreparedData {
        private final List<PaintingDefinition> paintings = new ArrayList<>();
        private final List<PaintingThemePlacement> placements = new ArrayList<>();
    }
}

