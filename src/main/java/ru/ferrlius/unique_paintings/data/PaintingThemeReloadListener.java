package ru.ferrlius.unique_paintings.data;

import com.mojang.logging.LogUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Rarity;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

public class PaintingThemeReloadListener extends SimplePreparableReloadListener<Map<ResourceLocation, JsonObject>> {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    protected Map<ResourceLocation, JsonObject> prepare(
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        Map<ResourceLocation, JsonObject> loaded = new HashMap<>();

        Map<ResourceLocation, Resource> resources =
                resourceManager.listResources(
                        "painting_theme",
                        path -> path.getPath().endsWith(".json")
                );

        for (var entry : resources.entrySet()) {
            try (var reader = new InputStreamReader(entry.getValue().open())) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                loaded.put(entry.getKey(), json);
            } catch (Exception e) {
                LOGGER.error("Failed to load painting theme {}", entry.getKey(), e);
            }
        }

        return loaded;
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonObject> objects,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        PaintingThemeManager.clear();

        for (var entry : objects.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            JsonObject json = entry.getValue();

            List<PaintingThemeEntry> variants = new ArrayList<>();

            JsonArray array = json.getAsJsonArray("variants");

            for (JsonElement el : array) {
                PaintingThemeEntry themeEntry = parseVariant(el, fileId);
                if (themeEntry != null) {
                    variants.add(themeEntry);
                }
            }

            ResourceLocation themeId = json.has("theme")
                    ? ResourceLocation.parse(json.get("theme").getAsString())
                    : ResourceLocation.fromNamespaceAndPath(
                            fileId.getNamespace(),
                            fileId.getPath().replace("painting_theme/", "").replace(".json", "")
                    );

            PaintingThemeManager.append(themeId, variants);
        }
    }

    private PaintingThemeEntry parseVariant(JsonElement element, ResourceLocation fileId) {
        if (element.isJsonPrimitive()) {
            return new PaintingThemeEntry(ResourceLocation.parse(element.getAsString()), 1, Rarity.UNCOMMON);
        }

        if (!element.isJsonObject()) {
            LOGGER.warn("Skipping malformed painting theme entry in {}", fileId);
            return null;
        }

        JsonObject object = element.getAsJsonObject();
        ResourceLocation variantId = ResourceLocation.parse(object.get("id").getAsString());
        int weight = object.has("weight") ? object.get("weight").getAsInt() : 1;
        Rarity rarity = object.has("rarity")
                ? parseRarity(object.get("rarity").getAsString(), fileId, variantId)
                : Rarity.UNCOMMON;

        if (weight <= 0) {
            LOGGER.warn("Skipping non-positive weighted painting {} in {}", variantId, fileId);
            return null;
        }

        return new PaintingThemeEntry(variantId, weight, rarity);
    }

    private Rarity parseRarity(String rarityName, ResourceLocation fileId, ResourceLocation variantId) {
        return switch (rarityName.toLowerCase(java.util.Locale.ROOT)) {
            case "common" -> Rarity.COMMON;
            case "uncommon" -> Rarity.UNCOMMON;
            case "rare" -> Rarity.RARE;
            case "epic" -> Rarity.EPIC;
            default -> {
                LOGGER.warn("Unknown rarity '{}' for painting {} in {}, defaulting to uncommon", rarityName, variantId, fileId);
                yield Rarity.UNCOMMON;
            }
        };
    }
}
