package ru.ferrlius.unique_paintings;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;
import ru.ferrlius.unique_paintings.data.PaintingThemeReloadListener;
import ru.ferrlius.unique_paintings.loot.ModLootModifiers;
import ru.ferrlius.unique_paintings.trade.PaintingTraderListing;
import ru.ferrlius.unique_paintings.util.PaintingStackHelper;

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
        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.addListener(this::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onDatapackSync);
        NeoForge.EVENT_BUS.addListener(this::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(this::onEntityTick);
        NeoForge.EVENT_BUS.addListener(this::onEntityJoinLevel);
        ModLootModifiers.LOOT_MODIFIERS.register(modEventBus);
    }

    private void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new PaintingThemeReloadListener());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Paintings are Unique.");


    }

    private void onServerStarted(ServerStartedEvent event) {
        removePaintingRecipe(event.getServer().getRecipeManager());
    }

    private void onDatapackSync(OnDatapackSyncEvent event) {
        removePaintingRecipe(event.getPlayerList().getServer().getRecipeManager());
    }

    // hardcode removal of Painting recipe cuz datapack stuff didn't work...
    private void removePaintingRecipe(RecipeManager recipeManager) {
        List<RecipeHolder<?>> recipes = List.copyOf(recipeManager.getRecipes());
        List<RecipeHolder<?>> filteredRecipes = recipes.stream()
                .filter(recipe -> !recipe.id().equals(PAINTING_RECIPE_ID))
                .toList();

        recipeManager.replaceRecipes(filteredRecipes);
    }

    private void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide() || event.getEntity().tickCount % 40 != 0) {
            return;
        }

        boolean changed = false;
        for (int slot = 0; slot < event.getEntity().getInventory().getContainerSize(); slot++) {
            ItemStack stack = event.getEntity().getInventory().getItem(slot);
            changed |= PaintingStackHelper.sanitizeInvalidVariant(
                    stack,
                    event.getEntity().registryAccess()
            );
        }

        ItemStack carried = event.getEntity().containerMenu.getCarried();
        changed |= PaintingStackHelper.sanitizeInvalidVariant(
                carried,
                event.getEntity().registryAccess()
        );

        if (changed) {
            event.getEntity().getInventory().setChanged();
            event.getEntity().containerMenu.broadcastChanges();
        }
    }

    private void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()
                || !(event.getEntity() instanceof ItemEntity itemEntity)
                || itemEntity.tickCount % 40 != 0) {
            return;
        }

        ItemStack stack = itemEntity.getItem();
        if (PaintingStackHelper.sanitizeInvalidVariant(stack, itemEntity.registryAccess())) {
            itemEntity.setItem(stack);
        }
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
}
