package ru.ferrlius.accessions.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import ru.ferrlius.accessions.data.PaintingThemeManager;
import ru.ferrlius.accessions.data.PaintingThemePlacement;

import java.util.List;

public class PaintingLootModifier extends LootModifier {
    public PaintingLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(
            ObjectArrayList<ItemStack> generatedLoot,
            LootContext context
    ) {
        ResourceLocation lootTableId = context.getQueriedLootTableId();
        List<PaintingThemePlacement> placements = PaintingThemeManager.getPlacements(lootTableId);
        for (PaintingThemePlacement placement : placements) {
            if (context.getRandom().nextFloat() <= placement.chance()) {
                generatedLoot.add(PaintingLootHandler.createThemedPainting(context, placement.theme()));
            }
        }

        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    public static final MapCodec<PaintingLootModifier> CODEC =
            RecordCodecBuilder.mapCodec(instance ->
                    LootModifier.codecStart(instance)
                            .apply(instance, PaintingLootModifier::new)
            );
}

