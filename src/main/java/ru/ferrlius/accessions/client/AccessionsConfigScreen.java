package ru.ferrlius.accessions.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import ru.ferrlius.accessions.config.AccessionsConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AccessionsConfigScreen extends Screen {
    private static final Component TITLE = Component.literal("Accessions Config");
    private static final Component TRADER_LABEL = Component.literal("Trader blacklist");
    private static final Component LOOT_LABEL = Component.literal("Chest loot blacklist");
    private static final Component HINT = Component.literal("Example: minecraft:void");
    private static final Component EMPTY = Component.literal("No blacklisted variants");
    private static final int MAX_SUGGESTIONS = 6;

    private final Screen parent;
    private final List<String> traderBlacklist = new ArrayList<>();
    private final List<String> lootBlacklist = new ArrayList<>();

    private EditBox traderInput;
    private EditBox lootInput;
    private Component statusMessage;
    private int statusColor = 0xFF8080;
    private List<String> suggestions = List.of();
    private EditBox activeSuggestionInput;
    private int selectedSuggestion = -1;

    public AccessionsConfigScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
        this.traderBlacklist.addAll(AccessionsConfig.getTraderBlacklist());
        this.lootBlacklist.addAll(AccessionsConfig.getLootBlacklist());
    }

    @Override
    protected void init() {
        clearWidgets();

        int panelWidth = Math.min(380, this.width - 32);
        int x = (this.width - panelWidth) / 2;

        traderInput = new EditBox(this.font, x, 54, panelWidth - 56, 20, TRADER_LABEL);
        traderInput.setHint(Component.literal(""));
        traderInput.setResponder(value -> onInputChanged(traderInput));
        traderInput.setMaxLength(200);
        addRenderableWidget(traderInput);
        addRenderableWidget(Button.builder(Component.literal("Add"), button -> addEntry(traderInput, traderBlacklist, "trader"))
                .bounds(x + panelWidth - 50, 54, 50, 20)
                .build());

        lootInput = new EditBox(this.font, x, 172, panelWidth - 56, 20, LOOT_LABEL);
        lootInput.setHint(Component.literal(""));
        lootInput.setResponder(value -> onInputChanged(lootInput));
        lootInput.setMaxLength(200);
        addRenderableWidget(lootInput);
        addRenderableWidget(Button.builder(Component.literal("Add"), button -> addEntry(lootInput, lootBlacklist, "loot"))
                .bounds(x + panelWidth - 50, 172, 50, 20)
                .build());

        addEntryButtons(x, 94, panelWidth, traderBlacklist, true);
        addEntryButtons(x, 212, panelWidth, lootBlacklist, false);

        addRenderableWidget(Button.builder(Component.literal("Defaults"), button -> restoreDefaults())
                .bounds(this.width / 2 - 154, this.height - 28, 100, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Save"), button -> saveAndClose())
                .bounds(this.width / 2 - 50, this.height - 28, 100, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                .bounds(this.width / 2 + 54, this.height - 28, 100, 20)
                .build());

        setInitialFocus(traderInput);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int panelWidth = Math.min(380, this.width - 32);
        int x = (this.width - panelWidth) / 2;

        renderSectionBackground(guiGraphics, x, 40, panelWidth);
        renderSectionBackground(guiGraphics, x, 158, panelWidth);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 18, 0xFFFFFF);

        renderSectionContent(guiGraphics, x, 40, panelWidth, TRADER_LABEL, HINT, traderBlacklist);
        renderSectionContent(guiGraphics, x, 158, panelWidth, LOOT_LABEL, HINT, lootBlacklist);
        renderSuggestions(guiGraphics);

        if (statusMessage != null) {
            guiGraphics.drawCenteredString(this.font, statusMessage, this.width / 2, this.height - 42, statusColor);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (activeSuggestionInput != null && !suggestions.isEmpty()) {
            if (keyCode == 258 || keyCode == 257 || keyCode == 335) {
                if (selectedSuggestion >= 0 && selectedSuggestion < suggestions.size()) {
                    applySuggestion(suggestions.get(selectedSuggestion));
                    return true;
                }
            } else if (keyCode == 264) {
                selectedSuggestion = Math.min(selectedSuggestion + 1, suggestions.size() - 1);
                return true;
            } else if (keyCode == 265) {
                selectedSuggestion = Math.max(selectedSuggestion - 1, 0);
                return true;
            } else if (keyCode == 256) {
                clearSuggestions();
                return true;
            }
        }

        if (traderInput.isFocused() && isAddKey(keyCode)) {
            addEntry(traderInput, traderBlacklist, "trader");
            return true;
        }
        if (lootInput.isFocused() && isAddKey(keyCode)) {
            addEntry(lootInput, lootBlacklist, "loot");
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (clickSuggestion(mouseX, mouseY)) {
            return true;
        }
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        updateFocusedInput();
        return handled;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    private void renderSectionBackground(GuiGraphics guiGraphics, int x, int y, int width) {
        guiGraphics.fill(x - 6, y - 4, x + width + 6, y + 96, 0x33000000);
    }

    private void renderSectionContent(GuiGraphics guiGraphics, int x, int y, int width, Component title, Component hint, List<String> entries) {
        guiGraphics.drawString(this.font, title, x, y, 0xFFFFFF);
        guiGraphics.drawString(this.font, hint, x, y + 40, 0x808080);

        if (entries.isEmpty()) {
            guiGraphics.drawString(this.font, EMPTY, x, y + 58, 0x808080);
            return;
        }

        int rowY = y + 58;
        for (String value : entries) {
            String display = this.font.plainSubstrByWidth(value, width - 30);
            guiGraphics.drawString(this.font, display, x, rowY, 0xE0E0E0);
            rowY += 22;
        }
    }

    private void addEntry(EditBox source, List<String> target, String label) {
        String raw = source.getValue().trim();
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (raw.isEmpty()) {
            setStatus(Component.literal("Enter a variant id."), 0xFF8080);
            return;
        }
        if (id == null) {
            setStatus(Component.literal("Invalid variant id: " + raw).withStyle(ChatFormatting.RED), 0xFF8080);
            return;
        }
        if (target.contains(id.toString())) {
            setStatus(Component.literal("Already listed in " + label + " blacklist: " + raw).withStyle(ChatFormatting.YELLOW), 0xFFD070);
            return;
        }

        target.add(id.toString());
        source.setValue("");
        setStatus(Component.literal("Added " + id + ".").withStyle(ChatFormatting.GREEN), 0x80FF80);
        clearSuggestions();
        init();
    }

    private void removeEntry(List<String> target, String value) {
        target.remove(value);
        setStatus(Component.literal("Removed " + value + ".").withStyle(ChatFormatting.YELLOW), 0xFFD070);
        init();
    }

    private void restoreDefaults() {
        traderBlacklist.clear();
        traderBlacklist.addAll(AccessionsConfig.getDefaultBlacklist());
        lootBlacklist.clear();
        lootBlacklist.addAll(AccessionsConfig.getDefaultBlacklist());
        traderInput.setValue("");
        lootInput.setValue("");
        setStatus(Component.literal("Restored default blacklist.").withStyle(ChatFormatting.GREEN), 0x80FF80);
        clearSuggestions();
        init();
    }

    private void saveAndClose() {
        AccessionsConfig.setTraderBlacklist(traderBlacklist);
        AccessionsConfig.setLootBlacklist(lootBlacklist);
        AccessionsConfig.save();
        onClose();
    }

    private void addEntryButtons(int x, int startY, int panelWidth, List<String> entries, boolean traderSection) {
        int y = startY;
        for (String value : entries) {
            addRenderableWidget(Button.builder(Component.literal("x"), button -> removeEntry(entries, value))
                    .bounds(x + panelWidth - 20, y - 2, 20, 20)
                    .build());
            y += 22;
        }
    }

    private void clearStatus() {
        statusMessage = null;
    }

    private void onInputChanged(EditBox source) {
        clearStatus();
        activeSuggestionInput = source;
        updateSuggestions(source);
    }

    private void setStatus(Component message, int color) {
        statusMessage = message;
        statusColor = color;
    }

    private void updateFocusedInput() {
        if (traderInput != null && traderInput.isFocused()) {
            activeSuggestionInput = traderInput;
            updateSuggestions(traderInput);
            return;
        }
        if (lootInput != null && lootInput.isFocused()) {
            activeSuggestionInput = lootInput;
            updateSuggestions(lootInput);
            return;
        }
        clearSuggestions();
    }

    private void updateSuggestions(EditBox source) {
        String raw = source.getValue().trim().toLowerCase();
        if (raw.isEmpty()) {
            clearSuggestions();
            return;
        }

        suggestions = collectRegistryVariantIds().stream()
                .filter(id -> id.toLowerCase().contains(raw))
                .sorted(Comparator
                        .comparing((String id) -> !id.startsWith(raw))
                        .thenComparingInt(String::length)
                        .thenComparing(String::compareTo))
                .limit(MAX_SUGGESTIONS)
                .toList();

        selectedSuggestion = suggestions.isEmpty() ? -1 : 0;
    }

    private List<String> collectRegistryVariantIds() {
        Minecraft minecraft = this.minecraft;
        if (minecraft == null) {
            return List.of();
        }

        HolderLookup.Provider provider = null;
        if (minecraft.level != null) {
            provider = minecraft.level.registryAccess();
        } else if (minecraft.getSingleplayerServer() != null) {
            provider = minecraft.getSingleplayerServer().registryAccess();
        } else if (minecraft.getConnection() != null) {
            provider = minecraft.getConnection().registryAccess();
        }

        if (provider == null) {
            return List.of();
        }

        return provider.lookupOrThrow(Registries.PAINTING_VARIANT).listElementIds()
                .map(key -> key.location().toString())
                .toList();
    }

    private void renderSuggestions(GuiGraphics guiGraphics) {
        if (activeSuggestionInput == null || suggestions.isEmpty()) {
            return;
        }

        SuggestionPopup popup = getSuggestionPopup();
        int x = popup.x();
        int y = popup.y();
        int width = popup.width();
        int rowHeight = 12;
        int boxHeight = suggestions.size() * rowHeight + 4;

        guiGraphics.fill(x, y, x + width, y + boxHeight, 0xE0101010);
        guiGraphics.fill(x - 1, y - 1, x + width + 1, y, 0xFF707070);
        guiGraphics.fill(x - 1, y + boxHeight, x + width + 1, y + boxHeight + 1, 0xFF707070);
        guiGraphics.fill(x - 1, y - 1, x, y + boxHeight + 1, 0xFF707070);
        guiGraphics.fill(x + width, y - 1, x + width + 1, y + boxHeight + 1, 0xFF707070);

        int rowY = y + 2;
        for (int i = 0; i < suggestions.size(); i++) {
            if (i == selectedSuggestion) {
                guiGraphics.fill(x + 1, rowY - 1, x + width - 1, rowY + rowHeight - 1, 0xFF2F4F4F);
            }
            guiGraphics.drawString(this.font, this.font.plainSubstrByWidth(suggestions.get(i), width - 6), x + 3, rowY, 0xE0E0E0);
            rowY += rowHeight;
        }
    }

    private boolean clickSuggestion(double mouseX, double mouseY) {
        if (activeSuggestionInput == null || suggestions.isEmpty()) {
            return false;
        }

        SuggestionPopup popup = getSuggestionPopup();
        int x = popup.x();
        int y = popup.y();
        int width = popup.width();
        int rowHeight = 12;

        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + suggestions.size() * rowHeight + 4) {
            return false;
        }

        int index = (int) ((mouseY - y - 2) / rowHeight);
        if (index >= 0 && index < suggestions.size()) {
            applySuggestion(suggestions.get(index));
            return true;
        }

        return false;
    }

    private void applySuggestion(String suggestion) {
        if (activeSuggestionInput == null) {
            return;
        }
        activeSuggestionInput.setValue(suggestion);
        activeSuggestionInput.moveCursorToEnd(false);
        clearSuggestions();
    }

    private void clearSuggestions() {
        suggestions = List.of();
        selectedSuggestion = -1;
        activeSuggestionInput = null;
    }

    private SuggestionPopup getSuggestionPopup() {
        int width = Math.min(220, Math.max(140, longestSuggestionWidth() + 8));
        int x = activeSuggestionInput.getX() + activeSuggestionInput.getWidth() + 4;
        if (x + width > this.width - 8) {
            x = activeSuggestionInput.getX() - width - 4;
        }
        if (x < 8) {
            x = Math.max(8, this.width - width - 8);
        }

        int y = activeSuggestionInput.getY();
        return new SuggestionPopup(x, y, width);
    }

    private int longestSuggestionWidth() {
        int max = 0;
        for (String suggestion : suggestions) {
            max = Math.max(max, this.font.width(suggestion));
        }
        return max;
    }

    private static boolean isAddKey(int keyCode) {
        return keyCode == 257 || keyCode == 335;
    }

    private record SuggestionPopup(int x, int y, int width) {
    }
}
