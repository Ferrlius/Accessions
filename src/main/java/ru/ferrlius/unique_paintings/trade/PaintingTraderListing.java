package ru.ferrlius.unique_paintings.trade;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import ru.ferrlius.unique_paintings.data.PaintingThemeManager;
import ru.ferrlius.unique_paintings.registry.ModPaintingVariants;
import ru.ferrlius.unique_paintings.util.PaintingStackHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class PaintingTraderListing {
    private PaintingTraderListing() {
    }

    public static Optional<MerchantOffer> createOffer(Entity trader, RandomSource random, Set<ResourceLocation> excludedVariants) {
        Registry<PaintingVariant> registry = trader.registryAccess().registryOrThrow(Registries.PAINTING_VARIANT);
        List<ResourceLocation> pool = new ArrayList<>(PaintingThemeManager.getAllVariantIds());
        pool.removeIf(excludedVariants::contains);
        pool.removeIf(ModPaintingVariants::isErrorVariant);
        pool.removeIf(variantId -> registry.getHolder(ResourceKey.create(Registries.PAINTING_VARIANT, variantId)).isEmpty());

        if (pool.isEmpty()) {
            return Optional.empty();
        }

        ResourceLocation chosenId = pool.get(random.nextInt(pool.size()));
        Optional<net.minecraft.core.Holder.Reference<PaintingVariant>> holder = registry.getHolder(
                ResourceKey.create(Registries.PAINTING_VARIANT, chosenId)
        );

        if (holder.isEmpty()) {
            return Optional.empty();
        }

        ItemStack forSale = new ItemStack(Items.PAINTING);
        PaintingStackHelper.saveVariant(forSale, trader.registryAccess(), holder.get());
        Rarity rarity = PaintingThemeManager.getRarity(chosenId);
        return Optional.of(new MerchantOffer(
                new ItemCost(Items.EMERALD, emeraldCostFor(rarity, random)),
                forSale,
                1,
                1,
                0.0F
        ));
    }

    // randomized pricing based on vanilla rarity.
    private static int emeraldCostFor(Rarity rarity, RandomSource random) {
        int base = switch (rarity) {
            case COMMON -> 6;
            case UNCOMMON -> 10;
            case RARE -> 16;
            case EPIC -> 24;
        };

        // ±20% variation
        float variation = 0.8f + (random.nextFloat() * 0.4f); // 0.8 → 1.2
        return Math.max(1, Math.round(base * variation));
    }
}
