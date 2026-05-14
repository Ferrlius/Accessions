package ru.ferrlius.accessions.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Rarity;

public record PaintingThemeEntry(ResourceLocation variantId, int weight, Rarity rarity) {
}


