package ru.ferrlius.accessions.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.ferrlius.accessions.Accessions;

import java.util.Optional;

public final class ModPaintingVariants {
    public static final DeferredRegister<PaintingVariant> PAINTING_VARIANTS =
            DeferredRegister.create(Registries.PAINTING_VARIANT, Accessions.MODID);

    static {
        for (int width = 1; width <= 16; width++) {
            for (int height = 1; height <= 16; height++) {
                int finalWidth = width;
                int finalHeight = height;
                PAINTING_VARIANTS.register(
                        errorVariantPath(finalWidth, finalHeight),
                        () -> new PaintingVariant(
                                finalWidth,
                                finalHeight,
                                ResourceLocation.fromNamespaceAndPath(
                                        Accessions.MODID,
                                        "texture_error/" + finalWidth + "x" + finalHeight
                                )
                        )
                );
            }
        }
    }

    private ModPaintingVariants() {
    }

    public static Optional<Holder.Reference<PaintingVariant>> getErrorVariant(
            HolderLookup.Provider registries,
            int width,
            int height
    ) {
        int clampedWidth = Math.max(1, Math.min(16, width));
        int clampedHeight = Math.max(1, Math.min(16, height));
        return registries.lookupOrThrow(Registries.PAINTING_VARIANT).get(
                ResourceKey.create(
                        Registries.PAINTING_VARIANT,
                        ResourceLocation.fromNamespaceAndPath(
                                Accessions.MODID,
                                errorVariantPath(clampedWidth, clampedHeight)
                        )
                )
        );
    }

    public static boolean isErrorVariant(ResourceLocation variantId) {
        return variantId.getNamespace().equals(Accessions.MODID)
                && variantId.getPath().startsWith("texture_error_");
    }

    private static String errorVariantPath(int width, int height) {
        return "texture_error_" + width + "x" + height;
    }
}

