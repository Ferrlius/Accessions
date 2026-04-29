package ru.ferrlius.unique_paintings.loot;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModLootModifiers {

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>>
            LOOT_MODIFIERS =
            DeferredRegister.create(
                    NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                    "unique_paintings"
            );

    public static final Supplier<MapCodec<? extends IGlobalLootModifier>>
            PAINTING_LOOT =
            LOOT_MODIFIERS.register(
                    "painting_loot",
                    () -> PaintingLootModifier.CODEC
            );
}