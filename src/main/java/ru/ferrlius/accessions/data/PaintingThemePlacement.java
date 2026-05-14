package ru.ferrlius.accessions.data;

import net.minecraft.resources.ResourceLocation;

public record PaintingThemePlacement(
        ResourceLocation theme,
        ResourceLocation lootTableId,
        float chance
) {
}


