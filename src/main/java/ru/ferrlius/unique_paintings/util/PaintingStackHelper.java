package ru.ferrlius.unique_paintings.util;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.slf4j.Logger;
import ru.ferrlius.unique_paintings.data.PaintingThemeManager;

import java.util.Optional;

public class PaintingStackHelper {
    private static final Logger LOGGER = LogUtils.getLogger();

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

        CustomData.set(DataComponents.ENTITY_DATA, stack, tag);
        stack.set(DataComponents.MAX_STACK_SIZE, 1);
        variant.unwrapKey().ifPresent(key ->
                stack.set(DataComponents.RARITY, PaintingThemeManager.getRarity(key.location()))
        );
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

        ResourceLocation variantId = ResourceLocation.tryParse(tag.getString("variant"));
        return Optional.ofNullable(variantId);
    }

    public static boolean sanitizeInvalidVariant(
            ItemStack stack,
            HolderLookup.Provider registries
    ) {
        if (!stack.is(Items.PAINTING)) {
            return false;
        }

        CustomData entityData = stack.getOrDefault(
                DataComponents.ENTITY_DATA,
                CustomData.EMPTY
        );

        if (entityData.isEmpty()) {
            return false;
        }

        if (getSavedVariant(stack, registries).isPresent()) {
            return false;
        }

        stack.remove(DataComponents.ENTITY_DATA);
        stack.remove(DataComponents.MAX_STACK_SIZE);
        stack.remove(DataComponents.RARITY);
        return true;
    }
}
