package ru.ferrlius.accessions.data;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.level.Level;
import ru.ferrlius.accessions.config.AccessionsConfig;
import ru.ferrlius.accessions.registry.ModPaintingVariants;

import java.util.List;

public class PaintingThemeResolver {

    public static Holder<PaintingVariant> resolve(
            Level level,
            ResourceLocation theme
    ) {
        List<PaintingThemeEntry> variants =
                PaintingThemeManager.get(theme);

        Registry<PaintingVariant> registry =
                level.registryAccess()
                        .registryOrThrow(Registries.PAINTING_VARIANT);

        if (variants.isEmpty()) {
            return registry.holders()
                    .filter(holder -> holder.unwrapKey().map(key ->
                            !ModPaintingVariants.isErrorVariant(key.location())
                                    && !AccessionsConfig.isLootBlacklisted(key.location())
                    ).orElse(true))
                    .findFirst()
                    .orElseGet(() -> registry.holders()
                            .filter(holder -> holder.unwrapKey().map(key -> !ModPaintingVariants.isErrorVariant(key.location())).orElse(true))
                            .findFirst()
                            .orElseThrow());
        }

        RandomSource random = level.random;
        int totalWeight = 0;
        List<Holder<PaintingVariant>> resolvedVariants = new java.util.ArrayList<>();
        List<Integer> weights = new java.util.ArrayList<>();

        for (PaintingThemeEntry entry : variants) {
            var holder = registry.getHolder(
                    net.minecraft.resources.ResourceKey.create(
                            Registries.PAINTING_VARIANT,
                            entry.variantId()
                    )
            );

            if (holder.isPresent()) {
                if (AccessionsConfig.isLootBlacklisted(entry.variantId())) {
                    continue;
                }
                resolvedVariants.add(holder.get());
                weights.add(entry.weight());
                totalWeight += entry.weight();
            }
        }

        if (resolvedVariants.isEmpty()) {
            return registry.holders()
                    .filter(holder -> holder.unwrapKey().map(key ->
                            !ModPaintingVariants.isErrorVariant(key.location())
                                    && !AccessionsConfig.isLootBlacklisted(key.location())
                    ).orElse(true))
                    .findFirst()
                    .orElseGet(() -> registry.holders()
                            .filter(holder -> holder.unwrapKey().map(key -> !ModPaintingVariants.isErrorVariant(key.location())).orElse(true))
                            .findFirst()
                            .orElseThrow());
        }

        int chosenWeight = random.nextInt(totalWeight);
        for (int i = 0; i < resolvedVariants.size(); i++) {
            chosenWeight -= weights.get(i);
            if (chosenWeight < 0) {
                return resolvedVariants.get(i);
            }
        }

        return resolvedVariants.getLast();
    }
}

