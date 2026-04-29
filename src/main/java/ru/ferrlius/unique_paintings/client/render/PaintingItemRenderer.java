package ru.ferrlius.unique_paintings.client.render;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.joml.Matrix4f;

public class PaintingItemRenderer extends BlockEntityWithoutLevelRenderer {

    public PaintingItemRenderer() {
        super(null, null);
    }

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext context,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            int overlay
    ) {
        ResourceLocation texture = resolveTexture(stack);

        VertexConsumer consumer = bufferSource.getBuffer(
                RenderType.entityCutoutNoCull(texture)
        );

        poseStack.pushPose();

        renderPaintedQuad(
                poseStack,
                consumer,
                light,
                OverlayTexture.NO_OVERLAY
        );

        poseStack.popPose();
    }

    private ResourceLocation resolveTexture(ItemStack stack) {
        CustomData data = stack.getOrDefault(
                DataComponents.ENTITY_DATA,
                CustomData.EMPTY
        );

        if (data.isEmpty()) {
            return ResourceLocation.withDefaultNamespace(
                    "textures/item/painting.png"
            );
        }

        CompoundTag tag = data.copyTag();

        if (!tag.contains("variant")) {
            return ResourceLocation.withDefaultNamespace(
                    "textures/item/painting.png"
            );
        }

        ResourceLocation variant =
                ResourceLocation.tryParse(tag.getString("variant"));

        if (variant == null) {
            return ResourceLocation.withDefaultNamespace(
                    "textures/item/painting.png"
            );
        }

        return ResourceLocation.fromNamespaceAndPath(
                variant.getNamespace(),
                "textures/item/painting/" + variant.getPath() + ".png"
        );
    }

    private static void renderPaintedQuad(PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();

        // attempt to generate an item model
        final float zFront = 8.5f; // zFront and zBack are positions of texture faces.
        final float zBack  = 7.5f;
        final float size   = 16f; // texture size
        final float p      = 1f / 16f; // the pixel size for the UV

        // Front (+Z)
        addVertex(consumer, pose, matrix, 0f,   0f,   zFront, 0f, 1f, light, overlay, 0f, 0f,  1f);
        addVertex(consumer, pose, matrix, size, 0f,   zFront, 1f, 1f, light, overlay, 0f, 0f,  1f);
        addVertex(consumer, pose, matrix, size, size, zFront, 1f, 0f, light, overlay, 0f, 0f,  1f);
        addVertex(consumer, pose, matrix, 0f,   size, zFront, 0f, 0f, light, overlay, 0f, 0f,  1f);

        // Back (-Z)
        addVertex(consumer, pose, matrix, 0f,   size, zBack, 0f, 0f, light, overlay, 0f, 0f, -1f);
        addVertex(consumer, pose, matrix, size, size, zBack, 1f, 0f, light, overlay, 0f, 0f, -1f);
        addVertex(consumer, pose, matrix, size, 0f,   zBack, 1f, 1f, light, overlay, 0f, 0f, -1f);
        addVertex(consumer, pose, matrix, 0f,   0f,   zBack, 0f, 1f, light, overlay, 0f, 0f, -1f);

        // Left (-X) — 1-пиксельная полоска u=0..1/16 (левая колонка текстуры)
        addVertex(consumer, pose, matrix, 0f, 0f,   zFront, 0f, 1f, light, overlay, -1f, 0f, 0f);
        addVertex(consumer, pose, matrix, 0f, 0f,   zBack,  p,  1f, light, overlay, -1f, 0f, 0f);
        addVertex(consumer, pose, matrix, 0f, size, zBack,  p,  0f, light, overlay, -1f, 0f, 0f);
        addVertex(consumer, pose, matrix, 0f, size, zFront, 0f, 0f, light, overlay, -1f, 0f, 0f);

        // Right (+X) — u=15/16..1 (правая колонка)
        addVertex(consumer, pose, matrix, size, 0f,   zBack,  1f - p, 1f, light, overlay, 1f, 0f, 0f);
        addVertex(consumer, pose, matrix, size, 0f,   zFront, 1f,     1f, light, overlay, 1f, 0f, 0f);
        addVertex(consumer, pose, matrix, size, size, zFront, 1f,     0f, light, overlay, 1f, 0f, 0f);
        addVertex(consumer, pose, matrix, size, size, zBack,  1f - p, 0f, light, overlay, 1f, 0f, 0f);

        // Top (+Y) — v=0..1/16 (верхний ряд)
        addVertex(consumer, pose, matrix, 0f,   size, zFront, 0f, 0f, light, overlay, 0f, 1f, 0f);
        addVertex(consumer, pose, matrix, 0f,   size, zBack,  0f, p,  light, overlay, 0f, 1f, 0f);
        addVertex(consumer, pose, matrix, size, size, zBack,  1f, p,  light, overlay, 0f, 1f, 0f);
        addVertex(consumer, pose, matrix, size, size, zFront, 1f, 0f, light, overlay, 0f, 1f, 0f);

        // Bottom (-Y) — v=15/16..1 (нижний ряд)
        addVertex(consumer, pose, matrix, 0f,   0f, zBack,  0f, 1f - p, light, overlay, 0f, -1f, 0f);
        addVertex(consumer, pose, matrix, 0f,   0f, zFront, 0f, 1f,     light, overlay, 0f, -1f, 0f);
        addVertex(consumer, pose, matrix, size, 0f, zFront, 1f, 1f,     light, overlay, 0f, -1f, 0f);
        addVertex(consumer, pose, matrix, size, 0f, zBack,  1f, 1f - p, light, overlay, 0f, -1f, 0f);
    }

    private static void addVertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Matrix4f matrix,
            float x,
            float y,
            float z,
            float u,
            float v,
            int light,
            int overlay,
            float normalX,
            float normalY,
            float normalZ
    ) {
        consumer.addVertex(
                        pose,
                        x / 16f,
                        y / 16f,
                        z / 16f
                )
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(
                        pose,
                        normalX,
                        normalY,
                        normalZ
                );
    }
}