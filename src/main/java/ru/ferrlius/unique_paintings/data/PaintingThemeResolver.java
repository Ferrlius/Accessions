package ru.ferrlius.unique_paintings.data;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.level.Level;

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
            return registry.getAny()
                    .orElseThrow();
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
                resolvedVariants.add(holder.get());
                weights.add(entry.weight());
                totalWeight += entry.weight();
            }
        }

        if (resolvedVariants.isEmpty()) {
            return registry.getAny().orElseThrow();
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
