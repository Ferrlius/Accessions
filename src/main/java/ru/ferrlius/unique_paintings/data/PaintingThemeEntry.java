package ru.ferrlius.unique_paintings.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Rarity;

public record PaintingThemeEntry(ResourceLocation variantId, int weight, Rarity rarity) {
}
