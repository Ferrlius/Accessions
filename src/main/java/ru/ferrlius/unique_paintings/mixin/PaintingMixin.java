package ru.ferrlius.unique_paintings.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.ferrlius.unique_paintings.util.PaintingStackHelper;

import javax.annotation.Nullable;

@Mixin(Painting.class)
public abstract class PaintingMixin {

    @Inject(
            method = "dropItem",
            at = @At("HEAD"),
            cancellable = true
    )
    private void uniquePaintings$preserveVariant(@Nullable Entity breaker, CallbackInfo ci) {
        ci.cancel();
        // had to rewrite some logic cuz I apparently overwrite an entire

        Painting painting = (Painting)(Object) this;

        if (!painting.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            return;
        }

        if (painting.level().isClientSide()) {
            return;
        }

        painting.playSound(
                net.minecraft.sounds.SoundEvents.PAINTING_BREAK,
                1.0F,
                1.0F
        );

        if (breaker instanceof Player player && player.hasInfiniteMaterials()) {
            return;
        }

        Holder<PaintingVariant> variant = painting.getVariant();

        ItemStack stack = new ItemStack(Items.PAINTING);

        PaintingStackHelper.saveVariant(
                stack,
                painting.registryAccess(),
                variant
        );

        painting.spawnAtLocation(stack);
    }
}
