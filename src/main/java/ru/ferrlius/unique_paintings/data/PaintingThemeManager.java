package ru.ferrlius.unique_paintings.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Rarity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PaintingThemeManager {

    private static final Map<ResourceLocation, List<PaintingThemeEntry>> THEMES = new HashMap<>();
    private static final Map<ResourceLocation, Rarity> VARIANT_RARITIES = new HashMap<>();

    public static void clear() {
        THEMES.clear();
        VARIANT_RARITIES.clear();
    }

    public static void append(ResourceLocation themeId, List<PaintingThemeEntry> variants) {
        THEMES.computeIfAbsent(themeId, ignored -> new ArrayList<>())
                .addAll(variants);

        for (PaintingThemeEntry entry : variants) {
            VARIANT_RARITIES.merge(
                    entry.variantId(),
                    entry.rarity(),
                    (left, right) -> right.ordinal() > left.ordinal() ? right : left
            );
        }
    }

    public static List<PaintingThemeEntry> get(ResourceLocation id) {
        return THEMES.getOrDefault(id, List.of());
    }

    public static Rarity getRarity(ResourceLocation variantId) {
        return VARIANT_RARITIES.getOrDefault(variantId, Rarity.UNCOMMON);
    }

    public static List<PaintingThemeEntry> getAllEntries() {
        return THEMES.values().stream()
                .flatMap(List::stream)
                .toList();
    }

    public static Set<ResourceLocation> getAllVariantIds() {
        return Set.copyOf(VARIANT_RARITIES.keySet());
    }
}
