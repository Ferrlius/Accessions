package ru.ferrlius.accessions.util;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import org.slf4j.Logger;
import ru.ferrlius.accessions.Accessions;
import ru.ferrlius.accessions.data.PaintingThemeManager;
import ru.ferrlius.accessions.registry.ModPaintingVariants;

import java.util.Optional;

public final class PaintingStackHelper {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final String WIDTH_TAG = "unique_paintings_width";
    public static final String HEIGHT_TAG = "unique_paintings_height";
    public static final String ORIGINAL_VARIANT_TAG = "unique_paintings_original_variant";

    private PaintingStackHelper() {
    }

    public static void saveVariant(
            ItemStack stack,
            HolderLookup.Provider registries,
            Holder<PaintingVariant> variant
    ) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", EntityType.getKey(EntityType.PAINTING).toString());

        Painting.VARIANT_CODEC.encodeStart(
                registries.createSerializationContext(NbtOps.INSTANCE),
                variant
        ).resultOrPartial(error ->
                LOGGER.error("Failed to encode painting variant: {}", error)
        ).ifPresent(encoded ->
                tag.merge((CompoundTag) encoded)
        );

        tag.putInt(WIDTH_TAG, variant.value().width());
        tag.putInt(HEIGHT_TAG, variant.value().height());

        CustomData.set(DataComponents.ENTITY_DATA, stack, tag);
        applyUniqueComponents(stack, variant.unwrapKey().map(key -> key.location()).orElse(null));
    }

    public static void saveMissingVariant(
            ItemStack stack,
            HolderLookup.Provider registries,
            ResourceLocation originalVariantId,
            int width,
            int height
    ) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", EntityType.getKey(EntityType.PAINTING).toString());

        ResourceLocation fallbackVariantId = ModPaintingVariants.getErrorVariant(
                registries,
                Math.max(1, width),
                Math.max(1, height)
        ).flatMap(Holder::unwrapKey)
                .map(key -> key.location())
                .orElseGet(() -> ResourceLocation.fromNamespaceAndPath(
                        Accessions.MODID,
                        "texture_error_1x1"
                ));

        tag.putString("variant", fallbackVariantId.toString());
        tag.putInt(WIDTH_TAG, Math.max(1, width));
        tag.putInt(HEIGHT_TAG, Math.max(1, height));
        tag.putString(ORIGINAL_VARIANT_TAG, originalVariantId.toString());

        CustomData.set(DataComponents.ENTITY_DATA, stack, tag);
        applyUniqueComponents(stack, originalVariantId);
    }

    public static Optional<Holder<PaintingVariant>> getSavedVariant(
            ItemStack stack,
            HolderLookup.Provider registries
    ) {
        CustomData entityData = stack.getOrDefault(
                DataComponents.ENTITY_DATA,
                CustomData.EMPTY
        );

        if (entityData.isEmpty()) {
            return Optional.empty();
        }

        return entityData.read(
                registries.createSerializationContext(NbtOps.INSTANCE),
                Painting.VARIANT_MAP_CODEC
        ).result();
    }

    public static Optional<ResourceLocation> getSavedVariantId(ItemStack stack) {
        if (!stack.is(Items.PAINTING)) {
            return Optional.empty();
        }

        CustomData entityData = stack.getOrDefault(
                DataComponents.ENTITY_DATA,
                CustomData.EMPTY
        );

        if (entityData.isEmpty()) {
            return Optional.empty();
        }

        CompoundTag tag = entityData.copyTag();
        if (!tag.contains("variant")) {
            return Optional.empty();
        }

        return Optional.ofNullable(ResourceLocation.tryParse(tag.getString("variant")));
    }

    public static Optional<ResourceLocation> getOriginalVariantId(ItemStack stack) {
        CustomData entityData = stack.getOrDefault(DataComponents.ENTITY_DATA, CustomData.EMPTY);
        if (entityData.isEmpty()) {
            return Optional.empty();
        }

        CompoundTag tag = entityData.copyTag();
        if (!tag.contains(ORIGINAL_VARIANT_TAG)) {
            return getSavedVariantId(stack);
        }

        return Optional.ofNullable(ResourceLocation.tryParse(tag.getString(ORIGINAL_VARIANT_TAG)));
    }

    public static Optional<Holder<PaintingVariant>> getOriginalVariant(
            ItemStack stack,
            HolderLookup.Provider registries
    ) {
        Optional<ResourceLocation> originalId = getOriginalVariantId(stack);
        if (originalId.isEmpty()) {
            return Optional.empty();
        }

        return registries.lookupOrThrow(Registries.PAINTING_VARIANT)
                .get(ResourceKey.create(Registries.PAINTING_VARIANT, originalId.get()))
                .map(holder -> holder);
    }

    public static int getStoredWidth(ItemStack stack) {
        return getStoredDimension(stack, WIDTH_TAG);
    }

    public static int getStoredHeight(ItemStack stack) {
        return getStoredDimension(stack, HEIGHT_TAG);
    }

    public static boolean isMissingVariant(ItemStack stack, HolderLookup.Provider registries) {
        return getOriginalVariantId(stack)
                .filter(originalId -> !originalId.equals(getSavedVariantId(stack).orElse(null)))
                .isPresent()
                && getOriginalVariant(stack, registries).isEmpty();
    }

    public static Optional<Holder<PaintingVariant>> getPlacementVariant(ItemStack stack, HolderLookup.Provider registries) {
        Optional<Holder<PaintingVariant>> original = getOriginalVariant(stack, registries);
        if (original.isPresent()) {
            return original;
        }

        return getSavedVariant(stack, registries).map(holder -> holder);
    }

    public static void markMissingPaintingEntity(Painting painting, ItemStack sourceStack) {
        ResourceLocation originalId = getOriginalVariantId(sourceStack).orElse(null);
        if (originalId == null) {
            return;
        }

        if (painting instanceof MissingPaintingVariantHolder holder) {
            holder.uniquePaintings$setMissingVariant(
                    originalId,
                    getStoredWidthOrDefault(sourceStack),
                    getStoredHeightOrDefault(sourceStack)
            );
        }
    }

    public static boolean isMissingPaintingEntity(Painting painting) {
        return painting instanceof MissingPaintingVariantHolder holder
                && holder.uniquePaintings$hasMissingVariant();
    }

    public static ItemStack createStackForPainting(Painting painting) {
        ItemStack stack = new ItemStack(Items.PAINTING);
        if (painting instanceof MissingPaintingVariantHolder holder
                && holder.uniquePaintings$hasMissingVariant()) {
            saveMissingVariant(
                    stack,
                    painting.registryAccess(),
                    holder.uniquePaintings$getMissingVariantId(),
                    holder.uniquePaintings$getMissingWidth(),
                    holder.uniquePaintings$getMissingHeight()
            );
            return stack;
        }

        saveVariant(stack, painting.registryAccess(), painting.getVariant());
        return stack;
    }

    public static Rarity getDisplayRarity(ItemStack stack) {
        return getOriginalVariantId(stack)
                .map(PaintingThemeManager::getRarity)
                .orElse(Rarity.COMMON);
    }

    private static void applyUniqueComponents(ItemStack stack, ResourceLocation variantId) {
        stack.set(DataComponents.MAX_STACK_SIZE, 1);
        stack.set(
                DataComponents.RARITY,
                variantId != null ? PaintingThemeManager.getRarity(variantId) : Rarity.COMMON
        );
    }

    private static int getStoredDimension(ItemStack stack, String key) {
        CustomData entityData = stack.getOrDefault(DataComponents.ENTITY_DATA, CustomData.EMPTY);
        if (entityData.isEmpty()) {
            return 0;
        }

        CompoundTag tag = entityData.copyTag();
        return tag.contains(key) ? tag.getInt(key) : 0;
    }

    private static int getStoredWidthOrDefault(ItemStack stack) {
        return Math.max(1, getStoredWidth(stack));
    }

    private static int getStoredHeightOrDefault(ItemStack stack) {
        return Math.max(1, getStoredHeight(stack));
    }
}

