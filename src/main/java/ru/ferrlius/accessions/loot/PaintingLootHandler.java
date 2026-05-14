package ru.ferrlius.accessions.loot;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import ru.ferrlius.accessions.data.PaintingThemeResolver;
import ru.ferrlius.accessions.util.PaintingStackHelper;

public class PaintingLootHandler {

    public static ItemStack createThemedPainting(
            LootContext context,
            ResourceLocation theme
    ) {
        Holder<PaintingVariant> variant =
                PaintingThemeResolver.resolve(
                        context.getLevel(),
                        theme
                );

        ItemStack stack = new ItemStack(Items.PAINTING);

        PaintingStackHelper.saveVariant(
                stack,
                context.getLevel().registryAccess(),
                variant
        );

        return stack;
    }
}

