package ru.ferrlius.unique_paintings.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;


public class PaintingLootModifier extends LootModifier {

    private final ResourceLocation theme;

    public PaintingLootModifier(LootItemCondition[] conditions, ResourceLocation theme) {
        super(conditions);
        this.theme = theme;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(
            ObjectArrayList<ItemStack> generatedLoot,
            LootContext context
    ) {
        generatedLoot.add(
                PaintingLootHandler.createThemedPainting(
                        context,
                        theme
                )
        );

        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    public static final MapCodec<PaintingLootModifier> CODEC =
            RecordCodecBuilder.mapCodec(instance ->
                    LootModifier.codecStart(instance)
                            .and(ResourceLocation.CODEC.fieldOf("theme").forGetter(modifier -> modifier.theme))
                            .apply(instance, PaintingLootModifier::new)
            );
}
