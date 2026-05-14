package ru.ferrlius.accessions.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.VariantHolder;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.ferrlius.accessions.util.MissingPaintingVariantHolder;
import ru.ferrlius.accessions.util.PaintingStackHelper;

@Pseudo
@Mixin(targets = "net.mehvahdjukaar.fastpaintings.PaintingBlock")
public abstract class FastPaintingsBlockMixin {
    @Inject(method = "getCloneItemStack", at = @At("HEAD"), cancellable = true)
    private void accessions$cloneExactPainting(
            LevelReader level,
            BlockPos pos,
            BlockState state,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (!(level instanceof Level actualLevel)) {
            return;
        }

        BlockEntity blockEntity = actualLevel.getBlockEntity(findMasterPos(state, pos));
        if (!(blockEntity instanceof VariantHolder<?> variantHolder)) {
            return;
        }

        Object rawVariant = variantHolder.getVariant();
        if (!(rawVariant instanceof Holder<?> genericHolder) || !(genericHolder.value() instanceof PaintingVariant)) {
            return;
        }

        @SuppressWarnings("unchecked")
        Holder<PaintingVariant> variant = (Holder<PaintingVariant>) genericHolder;
        ItemStack stack = new ItemStack(Items.PAINTING);

        if (blockEntity instanceof MissingPaintingVariantHolder missingHolder
                && missingHolder.uniquePaintings$hasMissingVariant()) {
            PaintingStackHelper.saveMissingVariant(
                    stack,
                    actualLevel.registryAccess(),
                    missingHolder.uniquePaintings$getMissingVariantId(),
                    missingHolder.uniquePaintings$getMissingWidth(),
                    missingHolder.uniquePaintings$getMissingHeight()
            );
        } else {
            PaintingStackHelper.saveVariant(stack, actualLevel.registryAccess(), variant);
        }

        cir.setReturnValue(stack);
    }

    private static BlockPos findMasterPos(BlockState state, BlockPos pos) {
        int yOffset = getIntProperty(state, "y_offset");
        int xOffset = getIntProperty(state, "x_offset");
        Direction facing = getDirectionProperty(state, "facing");
        return pos.above(yOffset).relative(facing.getClockWise(), xOffset);
    }

    private static int getIntProperty(BlockState state, String propertyName) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty integerProperty && integerProperty.getName().equals(propertyName)) {
                return state.getValue(integerProperty);
            }
        }
        return 0;
    }

    private static Direction getDirectionProperty(BlockState state, String propertyName) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof DirectionProperty directionProperty && directionProperty.getName().equals(propertyName)) {
                return state.getValue(directionProperty);
            }
        }
        return Direction.NORTH;
    }
}
