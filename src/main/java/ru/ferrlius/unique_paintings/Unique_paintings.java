package ru.ferrlius.unique_paintings;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.PaintingVariantTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;
import ru.ferrlius.unique_paintings.data.PaintingThemeReloadListener;
import ru.ferrlius.unique_paintings.loot.ModLootModifiers;
import ru.ferrlius.unique_paintings.registry.ModPaintingVariants;
import ru.ferrlius.unique_paintings.trade.PaintingTraderListing;
import ru.ferrlius.unique_paintings.util.PaintingStackHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Mod(Unique_paintings.MODID)
public class Unique_paintings {
    public static final String MODID = "unique_paintings";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation PAINTING_RECIPE_ID =
            ResourceLocation.fromNamespaceAndPath("minecraft", "painting");

    public Unique_paintings(IEventBus modEventBus) {
        modEventBus.addListener(this::onBuildCreativeTabContents);

        NeoForge.EVENT_BUS.addListener(this::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onDatapackSync);
        NeoForge.EVENT_BUS.addListener(this::onEntityJoinLevel);
        NeoForge.EVENT_BUS.addListener(this::onItemTooltip);

        ModLootModifiers.LOOT_MODIFIERS.register(modEventBus);
        ModPaintingVariants.PAINTING_VARIANTS.register(modEventBus);

        LOGGER.info("Paintings are Unique.");
    }

    private void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new PaintingThemeReloadListener());
    }

    private void onServerStarted(ServerStartedEvent event) {
        removePaintingRecipe(event.getServer().getRecipeManager());
    }

    private void onDatapackSync(OnDatapackSyncEvent event) {
        removePaintingRecipe(event.getPlayerList().getServer().getRecipeManager());
    }

    private void removePaintingRecipe(RecipeManager recipeManager) {
        List<RecipeHolder<?>> recipes = List.copyOf(recipeManager.getRecipes());
        recipeManager.replaceRecipes(recipes.stream()
                .filter(recipe -> !recipe.id().equals(PAINTING_RECIPE_ID))
                .toList());
    }

    private void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof WanderingTrader trader)) {
            return;
        }

        ensurePaintingOffers(trader);
    }

    private void ensurePaintingOffers(WanderingTrader trader) {
        MerchantOffers offers = trader.getOffers();
        Set<ResourceLocation> offeredVariants = new HashSet<>();

        for (MerchantOffer offer : offers) {
            PaintingStackHelper.getSavedVariantId(offer.getResult()).ifPresent(offeredVariants::add);
        }

        int desiredPaintingSlots = 1 + trader.getRandom().nextInt(3);
        int missingOffers = desiredPaintingSlots - offeredVariants.size();
        if (missingOffers <= 0) {
            return;
        }

        RandomSource random = trader.getRandom();
        for (int i = 0; i < missingOffers; i++) {
            Optional<MerchantOffer> offer = PaintingTraderListing.createOffer(trader, random, offeredVariants);
            if (offer.isEmpty()) {
                return;
            }

            offers.add(offer.get());
            PaintingStackHelper.getSavedVariantId(offer.get().getResult()).ifPresent(offeredVariants::add);
        }
    }

    private void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.PAINTING)) {
            return;
        }

        if (event.getContext().registries() == null
                || !PaintingStackHelper.isMissingVariant(stack, event.getContext().registries())) {
            return;
        }

        List<Component> tooltip = event.getToolTip();
        String randomText = Component.translatable("painting.random").getString();
        tooltip.removeIf(component -> component.getString().equals(randomText));

        PaintingStackHelper.getOriginalVariantId(stack).ifPresent(variantId ->
                tooltip.add(Component.literal(variantId.toString()).withStyle(ChatFormatting.DARK_GRAY))
        );

        int width = PaintingStackHelper.getStoredWidth(stack);
        int height = PaintingStackHelper.getStoredHeight(stack);
        if (width > 0 && height > 0) {
            tooltip.add(Component.translatable("painting.dimensions", width, height));
        }
    }

    private void onBuildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            rebuildPaintingTab(event, true);
            return;
        }

        if (event.getTabKey() == CreativeModeTabs.OP_BLOCKS) {
            rebuildPaintingTab(event, false);
        }
    }

    private void rebuildPaintingTab(BuildCreativeModeTabContentsEvent event, boolean placeableOnly) {
        if (!containsPainting(event.getParentEntries())) {
            return;
        }

        ItemStack insertionAnchor = findInsertionAnchor(event.getParentEntries());
        removeExistingPaintings(event);

        List<ItemStack> variantStacks = new ArrayList<>();
        if (placeableOnly) {
            variantStacks.add(new ItemStack(Items.PAINTING));
        }

        for (Holder.Reference<net.minecraft.world.entity.decoration.PaintingVariant> holder :
                event.getParameters().holders().lookupOrThrow(Registries.PAINTING_VARIANT).listElements().toList()) {
            ResourceLocation id = holder.key().location();
            if (ModPaintingVariants.isErrorVariant(id)) {
                continue;
            }
            if (holder.is(PaintingVariantTags.PLACEABLE) != placeableOnly) {
                continue;
            }

            ItemStack stack = new ItemStack(Items.PAINTING);
            PaintingStackHelper.saveVariant(stack, event.getParameters().holders(), holder);
            variantStacks.add(stack);
        }

        variantStacks.sort((left, right) -> {
            boolean leftRandom = PaintingStackHelper.getSavedVariantId(left).isEmpty();
            boolean rightRandom = PaintingStackHelper.getSavedVariantId(right).isEmpty();
            if (leftRandom != rightRandom) {
                return leftRandom ? -1 : 1;
            }

            int widthCompare = Integer.compare(
                    PaintingStackHelper.getStoredWidth(left),
                    PaintingStackHelper.getStoredWidth(right)
            );
            if (widthCompare != 0) {
                return widthCompare;
            }

            int heightCompare = Integer.compare(
                    PaintingStackHelper.getStoredHeight(left),
                    PaintingStackHelper.getStoredHeight(right)
            );
            if (heightCompare != 0) {
                return heightCompare;
            }

            ResourceLocation leftId = PaintingStackHelper.getSavedVariantId(left)
                    .orElse(ResourceLocation.withDefaultNamespace("painting"));
            ResourceLocation rightId = PaintingStackHelper.getSavedVariantId(right)
                    .orElse(ResourceLocation.withDefaultNamespace("painting"));
            return leftId.toString().compareTo(rightId.toString());
        });

        if (variantStacks.isEmpty()) {
            return;
        }

        ItemStack previous = null;
        for (ItemStack stack : variantStacks) {
            if (previous == null) {
                if (insertionAnchor != null) {
                    event.insertBefore(insertionAnchor, stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                } else {
                    event.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                }
            } else {
                event.insertAfter(previous, stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
            previous = stack;
        }
    }

    private void removeExistingPaintings(BuildCreativeModeTabContentsEvent event) {
        List<ItemStack> existingPaintings = new ArrayList<>();
        for (ItemStack entry : event.getParentEntries()) {
            if (entry.is(Items.PAINTING)) {
                existingPaintings.add(entry.copy());
            }
        }

        for (ItemStack entry : existingPaintings) {
            event.remove(entry, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }

    private boolean containsPainting(Iterable<ItemStack> entries) {
        for (ItemStack entry : entries) {
            if (entry.is(Items.PAINTING)) {
                return true;
            }
        }

        return false;
    }

    private ItemStack findInsertionAnchor(Iterable<ItemStack> entries) {
        boolean seenPainting = false;
        for (ItemStack entry : entries) {
            if (entry.is(Items.PAINTING)) {
                seenPainting = true;
                continue;
            }

            if (seenPainting) {
                return entry.copy();
            }
        }

        return null;
    }
}
