package ru.ferrlius.accessions.util;

import net.minecraft.resources.ResourceLocation;

public interface MissingPaintingVariantHolder {
    ResourceLocation uniquePaintings$getMissingVariantId();

    int uniquePaintings$getMissingWidth();

    int uniquePaintings$getMissingHeight();

    void uniquePaintings$setMissingVariant(ResourceLocation variantId, int width, int height);

    boolean uniquePaintings$hasMissingVariant();
}

