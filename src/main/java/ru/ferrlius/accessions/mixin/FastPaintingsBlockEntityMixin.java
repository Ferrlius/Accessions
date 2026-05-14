package ru.ferrlius.accessions.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.decoration.PaintingVariant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.ferrlius.accessions.registry.ModPaintingVariants;
import ru.ferrlius.accessions.util.MissingPaintingVariantHolder;
import ru.ferrlius.accessions.util.PaintingStackHelper;

import java.util.Optional;

@Pseudo
@Mixin(targets = "net.mehvahdjukaar.fastpaintings.PaintingBlockEntity")
public abstract class FastPaintingsBlockEntityMixin implements MissingPaintingVariantHolder {
    @Unique
    private ResourceLocation accessions$missingVariantId;
    @Unique
    private int accessions$missingWidth;
    @Unique
    private int accessions$missingHeight;

    @Shadow
    public abstract Holder<PaintingVariant> getVariant();

    @Shadow
    public abstract void setVariant(Holder<PaintingVariant> variant);

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void accessions$saveMissingVariant(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        if (!uniquePaintings$hasMissingVariant()) {
            return;
        }

        tag.putString(PaintingStackHelper.ORIGINAL_VARIANT_TAG, accessions$missingVariantId.toString());
        tag.putInt(PaintingStackHelper.WIDTH_TAG, accessions$missingWidth);
        tag.putInt(PaintingStackHelper.HEIGHT_TAG, accessions$missingHeight);
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void accessions$loadMissingVariant(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        accessions$missingVariantId = null;
        accessions$missingWidth = 0;
        accessions$missingHeight = 0;

        if (!tag.contains(PaintingStackHelper.ORIGINAL_VARIANT_TAG)) {
            return;
        }

        ResourceLocation originalId = ResourceLocation.tryParse(tag.getString(PaintingStackHelper.ORIGINAL_VARIANT_TAG));
        if (originalId == null) {
            return;
        }

        int width = Math.max(1, tag.getInt(PaintingStackHelper.WIDTH_TAG));
        int height = Math.max(1, tag.getInt(PaintingStackHelper.HEIGHT_TAG));

        Optional<Holder.Reference<PaintingVariant>> original = registries.lookupOrThrow(Registries.PAINTING_VARIANT)
                .get(ResourceKey.create(Registries.PAINTING_VARIANT, originalId));

        if (original.isPresent()) {
            setVariant(original.get());
            return;
        }

        uniquePaintings$setMissingVariant(originalId, width, height);
        ModPaintingVariants.getErrorVariant(registries, width, height).ifPresent(this::setVariant);
    }

    @Override
    public ResourceLocation uniquePaintings$getMissingVariantId() {
        return accessions$missingVariantId;
    }

    @Override
    public int uniquePaintings$getMissingWidth() {
        return accessions$missingWidth;
    }

    @Override
    public int uniquePaintings$getMissingHeight() {
        return accessions$missingHeight;
    }

    @Override
    public void uniquePaintings$setMissingVariant(ResourceLocation variantId, int width, int height) {
        accessions$missingVariantId = variantId;
        accessions$missingWidth = Math.max(1, width);
        accessions$missingHeight = Math.max(1, height);
    }

    @Override
    public boolean uniquePaintings$hasMissingVariant() {
        return accessions$missingVariantId != null;
    }
}
