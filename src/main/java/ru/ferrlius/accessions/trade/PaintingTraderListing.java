package ru.ferrlius.accessions.trade;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import ru.ferrlius.accessions.config.AccessionsConfig;
import ru.ferrlius.accessions.data.PaintingThemeManager;
import ru.ferrlius.accessions.registry.ModPaintingVariants;
import ru.ferrlius.accessions.util.PaintingStackHelper;

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
        pool.removeIf(AccessionsConfig::isTraderBlacklisted);
        pool.removeIf(variantId -> registry.getHolder(ResourceKey.create(Registries.PAINTING_VARIANT, variantId)).isEmpty());

        if (pool.isEmpty()) {
            return Optional.empty();
        }

        ResourceLocation chosenId = pool.get(random.nextInt(pool.size()));
        Optional<Holder.Reference<PaintingVariant>> holder = registry.getHolder(
                ResourceKey.create(Registries.PAINTING_VARIANT, chosenId)
        );
        if (holder.isEmpty()) {
            return Optional.empty();
        }

        ItemStack forSale = new ItemStack(Items.PAINTING);
        PaintingStackHelper.saveVariant(forSale, trader.registryAccess(), holder.get());
        return Optional.of(new MerchantOffer(
                new ItemCost(Items.PAINTING, 1),
                forSale,
                1,
                1,
                0.0F
        ));
    }
}

