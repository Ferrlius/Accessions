package ru.ferrlius.unique_paintings.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.ferrlius.unique_paintings.util.MissingPaintingVariantHolder;
import ru.ferrlius.unique_paintings.util.PaintingStackHelper;

import javax.annotation.Nullable;

@Mixin(Painting.class)
public abstract class PaintingMixin implements MissingPaintingVariantHolder {
    @Unique
    private static final EntityDataAccessor<String> UNIQUE_PAINTINGS$MISSING_VARIANT =
            SynchedEntityData.defineId(Painting.class, EntityDataSerializers.STRING);
    @Unique
    private static final EntityDataAccessor<Integer> UNIQUE_PAINTINGS$MISSING_WIDTH =
            SynchedEntityData.defineId(Painting.class, EntityDataSerializers.INT);
    @Unique
    private static final EntityDataAccessor<Integer> UNIQUE_PAINTINGS$MISSING_HEIGHT =
            SynchedEntityData.defineId(Painting.class, EntityDataSerializers.INT);

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void uniquePaintings$defineMissingVariantData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(UNIQUE_PAINTINGS$MISSING_VARIANT, "");
        builder.define(UNIQUE_PAINTINGS$MISSING_WIDTH, 0);
        builder.define(UNIQUE_PAINTINGS$MISSING_HEIGHT, 0);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void uniquePaintings$saveMissingVariantData(net.minecraft.nbt.CompoundTag tag, CallbackInfo ci) {
        if (!uniquePaintings$hasMissingVariant()) {
            return;
        }

        tag.putString(PaintingStackHelper.ORIGINAL_VARIANT_TAG, uniquePaintings$getMissingVariantId().toString());
        tag.putInt(PaintingStackHelper.WIDTH_TAG, uniquePaintings$getMissingWidth());
        tag.putInt(PaintingStackHelper.HEIGHT_TAG, uniquePaintings$getMissingHeight());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void uniquePaintings$readMissingVariantData(net.minecraft.nbt.CompoundTag tag, CallbackInfo ci) {
        if (!tag.contains(PaintingStackHelper.ORIGINAL_VARIANT_TAG)) {
            return;
        }

        ResourceLocation variantId = ResourceLocation.tryParse(tag.getString(PaintingStackHelper.ORIGINAL_VARIANT_TAG));
        if (variantId == null) {
            return;
        }

        uniquePaintings$setMissingVariant(
                variantId,
                Math.max(1, tag.getInt(PaintingStackHelper.WIDTH_TAG)),
                Math.max(1, tag.getInt(PaintingStackHelper.HEIGHT_TAG))
        );
    }

    @Inject(
            method = "dropItem",
            at = @At("HEAD"),
            cancellable = true
    )
    private void uniquePaintings$preserveVariant(@Nullable Entity breaker, CallbackInfo ci) {
        ci.cancel();

        Painting painting = (Painting) (Object) this;
        if (!painting.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            return;
        }

        if (painting.level().isClientSide()) {
            return;
        }

        painting.playSound(SoundEvents.PAINTING_BREAK, 1.0F, 1.0F);
        if (breaker instanceof Player player && player.hasInfiniteMaterials()) {
            return;
        }

        ItemStack stack = PaintingStackHelper.createStackForPainting(painting);
        painting.spawnAtLocation(stack);
    }

    @Inject(method = "getPickResult", at = @At("HEAD"), cancellable = true)
    private void uniquePaintings$pickExactVariant(CallbackInfoReturnable<ItemStack> cir) {
        Painting painting = (Painting) (Object) this;
        cir.setReturnValue(PaintingStackHelper.createStackForPainting(painting));
    }

    @Override
    public ResourceLocation uniquePaintings$getMissingVariantId() {
        return ResourceLocation.parse(((Painting) (Object) this).getEntityData().get(UNIQUE_PAINTINGS$MISSING_VARIANT));
    }

    @Override
    public int uniquePaintings$getMissingWidth() {
        return ((Painting) (Object) this).getEntityData().get(UNIQUE_PAINTINGS$MISSING_WIDTH);
    }

    @Override
    public int uniquePaintings$getMissingHeight() {
        return ((Painting) (Object) this).getEntityData().get(UNIQUE_PAINTINGS$MISSING_HEIGHT);
    }

    @Override
    public void uniquePaintings$setMissingVariant(ResourceLocation variantId, int width, int height) {
        ((Painting) (Object) this).getEntityData().set(UNIQUE_PAINTINGS$MISSING_VARIANT, variantId.toString());
        ((Painting) (Object) this).getEntityData().set(UNIQUE_PAINTINGS$MISSING_WIDTH, Math.max(1, width));
        ((Painting) (Object) this).getEntityData().set(UNIQUE_PAINTINGS$MISSING_HEIGHT, Math.max(1, height));
    }

    @Override
    public boolean uniquePaintings$hasMissingVariant() {
        return !((Painting) (Object) this).getEntityData().get(UNIQUE_PAINTINGS$MISSING_VARIANT).isBlank();
    }
}
