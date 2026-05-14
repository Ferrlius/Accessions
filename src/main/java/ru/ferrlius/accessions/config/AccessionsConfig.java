package ru.ferrlius.accessions.config;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class AccessionsConfig {
    public static final ModConfigSpec SPEC;
    private static final List<String> DEFAULT_BLACKLIST = List.of("supplementaries:jar");

    private static final ModConfigSpec.ConfigValue<List<? extends String>> TRADER_BLACKLIST;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> LOOT_BLACKLIST;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment(
                "Painting variant blacklists.",
                "Use full ids like namespace:path."
        ).push("blacklists");

        TRADER_BLACKLIST = builder.comment(
                        "Variants that wandering traders will never sell."
                )
                .defineListAllowEmpty(
                        "trader_variants",
                        DEFAULT_BLACKLIST,
                        AccessionsConfig::isVariantId
                );

        LOOT_BLACKLIST = builder.comment(
                        "Variants that chest loot generation will never add."
                )
                .defineListAllowEmpty(
                        "loot_variants",
                        DEFAULT_BLACKLIST,
                        AccessionsConfig::isVariantId
                );

        builder.pop();
        SPEC = builder.build();
    }

    private AccessionsConfig() {
    }

    public static boolean isTraderBlacklisted(ResourceLocation variantId) {
        return parseVariantSet(TRADER_BLACKLIST.get()).contains(variantId);
    }

    public static boolean isLootBlacklisted(ResourceLocation variantId) {
        return parseVariantSet(LOOT_BLACKLIST.get()).contains(variantId);
    }

    public static String getTraderBlacklistText() {
        return joinVariantList(TRADER_BLACKLIST.get());
    }

    public static String getLootBlacklistText() {
        return joinVariantList(LOOT_BLACKLIST.get());
    }

    public static List<String> getTraderBlacklist() {
        return List.copyOf(TRADER_BLACKLIST.get());
    }

    public static List<String> getLootBlacklist() {
        return List.copyOf(LOOT_BLACKLIST.get());
    }

    public static void setTraderBlacklistText(String value) {
        TRADER_BLACKLIST.set(parseVariantList(value));
    }

    public static void setLootBlacklistText(String value) {
        LOOT_BLACKLIST.set(parseVariantList(value));
    }

    public static void setTraderBlacklist(List<String> values) {
        TRADER_BLACKLIST.set(sanitizeVariantList(values));
    }

    public static void setLootBlacklist(List<String> values) {
        LOOT_BLACKLIST.set(sanitizeVariantList(values));
    }

    public static void save() {
        TRADER_BLACKLIST.save();
        LOOT_BLACKLIST.save();
    }

    public static List<String> getDefaultBlacklist() {
        return DEFAULT_BLACKLIST;
    }

    private static boolean isVariantId(Object value) {
        return value instanceof String text && ResourceLocation.tryParse(text) != null;
    }

    private static Set<ResourceLocation> parseVariantSet(List<? extends String> values) {
        Set<ResourceLocation> variants = new LinkedHashSet<>();
        for (String value : values) {
            ResourceLocation id = ResourceLocation.tryParse(value);
            if (id != null) {
                variants.add(id);
            }
        }
        return variants;
    }

    private static List<String> parseVariantList(String raw) {
        return raw.lines()
                .flatMap(line -> List.of(line.split(",")).stream())
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .filter(text -> ResourceLocation.tryParse(text) != null)
                .distinct()
                .toList();
    }

    private static String joinVariantList(List<? extends String> values) {
        return values.stream().collect(Collectors.joining(", "));
    }

    private static List<String> sanitizeVariantList(List<String> values) {
        LinkedHashSet<String> cleaned = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (!trimmed.isEmpty() && ResourceLocation.tryParse(trimmed) != null) {
                cleaned.add(trimmed);
            }
        }
        return new LinkedList<>(cleaned);
    }
}
