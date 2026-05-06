package ru.ferrlius.unique_paintings.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HangingEntityItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.ferrlius.unique_paintings.util.PaintingStackHelper;

@Mixin(HangingEntityItem.class)
public abstract class HangingEntityItemMixin {

    @Shadow
    @Final
    private EntityType<? extends HangingEntity> type;

    @Shadow
    protected abstract boolean mayPlace(Player player, Direction direction, ItemStack hangingEntityStack, BlockPos pos);

    @Inject(
            method = "useOn",
            at = @At("HEAD"),
            cancellable = true
    )
    private void uniquePaintings$placeExactPainting(
            UseOnContext context,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (this.type != EntityType.PAINTING) {
            return;
        }

        ItemStack stack = context.getItemInHand();
        if (!stack.has(DataComponents.ENTITY_DATA)) {
            return;
        }

        Holder<PaintingVariant> variant = PaintingStackHelper.getPlacementVariant(
                stack,
                context.getLevel().registryAccess()
        ).orElse(null);

        if (variant == null) {
            return;
        }

        BlockPos clickedPos = context.getClickedPos();
        Direction direction = context.getClickedFace();
        BlockPos placePos = clickedPos.relative(direction);
        Player player = context.getPlayer();
        ItemStack itemStack = context.getItemInHand();

        if (player != null && !this.mayPlace(player, direction, itemStack, placePos)) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }

        Level level = context.getLevel();
        Painting painting = new Painting(level, placePos, direction, variant);
        if (PaintingStackHelper.isMissingVariant(stack, context.getLevel().registryAccess())) {
            PaintingStackHelper.markMissingPaintingEntity(painting, stack);
        }

        if (!painting.survives()) {
            cir.setReturnValue(InteractionResult.CONSUME);
            return;
        }

        if (!level.isClientSide()) {
            painting.playPlacementSound();
            level.gameEvent(player, GameEvent.ENTITY_PLACE, painting.position());
            level.addFreshEntity(painting);
        }

        itemStack.shrink(1);
        cir.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide()));
    }
}
