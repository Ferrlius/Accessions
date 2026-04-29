package ru.ferrlius.unique_paintings.client;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import ru.ferrlius.unique_paintings.client.render.PaintingItemRenderer;

public class ClientExtensions implements IClientItemExtensions {

    private static final PaintingItemRenderer RENDERER =
            new PaintingItemRenderer();

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return RENDERER;
    }
}