package ru.ferrlius.unique_paintings.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Rarity;

import java.util.List;

public record PaintingDefinition(
        ResourceLocation variantId,
        int weight,
        Rarity rarity,
        List<ResourceLocation> themes
) {
}

