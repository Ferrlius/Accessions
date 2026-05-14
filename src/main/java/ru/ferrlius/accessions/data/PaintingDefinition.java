package ru.ferrlius.accessions.data;

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


