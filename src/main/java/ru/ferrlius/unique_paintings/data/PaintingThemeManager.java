package ru.ferrlius.unique_paintings.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Rarity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PaintingThemeManager {

    private static final Map<ResourceLocation, PaintingDefinition> PAINTINGS = new HashMap<>();
    private static final Map<ResourceLocation, List<PaintingThemeEntry>> THEMES = new HashMap<>();
    private static final Map<ResourceLocation, Rarity> VARIANT_RARITIES = new HashMap<>();
    private static final Map<ResourceLocation, List<PaintingThemePlacement>> LOOT_TABLES = new HashMap<>();

    public static void clear() {
        PAINTINGS.clear();
        THEMES.clear();
        VARIANT_RARITIES.clear();
        LOOT_TABLES.clear();
    }

    public static void registerPainting(PaintingDefinition definition) {
        PAINTINGS.merge(definition.variantId(), definition, PaintingThemeManager::mergeDefinitions);
    }

    public static void registerPlacements(Collection<PaintingThemePlacement> placements) {
        for (PaintingThemePlacement placement : placements) {
            LOOT_TABLES.computeIfAbsent(placement.lootTableId(), ignored -> new ArrayList<>())
                    .add(placement);
        }
    }

    public static void rebuildThemes() {
        THEMES.clear();
        VARIANT_RARITIES.clear();

        for (PaintingDefinition definition : PAINTINGS.values()) {
            VARIANT_RARITIES.merge(
                    definition.variantId(),
                    definition.rarity(),
                    (left, right) -> right.ordinal() > left.ordinal() ? right : left
            );

            for (ResourceLocation theme : definition.themes()) {
                THEMES.computeIfAbsent(theme, ignored -> new ArrayList<>())
                        .add(new PaintingThemeEntry(
                                definition.variantId(),
                                definition.weight(),
                                definition.rarity()
                        ));
            }
        }

        for (List<PaintingThemeEntry> entries : THEMES.values()) {
            entries.sort(Comparator.comparing(entry -> entry.variantId().toString()));
        }
    }

    public static List<PaintingThemeEntry> get(ResourceLocation id) {
        return THEMES.getOrDefault(id, List.of());
    }

    public static Rarity getRarity(ResourceLocation variantId) {
        return VARIANT_RARITIES.getOrDefault(variantId, Rarity.COMMON);
    }

    public static List<PaintingThemeEntry> getAllEntries() {
        return THEMES.values().stream()
                .flatMap(List::stream)
                .toList();
    }

    public static Set<ResourceLocation> getAllVariantIds() {
        return Set.copyOf(PAINTINGS.keySet());
    }

    public static List<PaintingThemePlacement> getPlacements(ResourceLocation lootTableId) {
        return LOOT_TABLES.getOrDefault(lootTableId, List.of());
    }

    private static PaintingDefinition mergeDefinitions(PaintingDefinition left, PaintingDefinition right) {
        LinkedHashSet<ResourceLocation> mergedThemes = new LinkedHashSet<>(left.themes());
        mergedThemes.addAll(right.themes());

        return new PaintingDefinition(
                left.variantId(),
                Math.max(left.weight(), right.weight()),
                right.rarity().ordinal() > left.rarity().ordinal() ? right.rarity() : left.rarity(),
                List.copyOf(mergedThemes)
        );
    }
}
