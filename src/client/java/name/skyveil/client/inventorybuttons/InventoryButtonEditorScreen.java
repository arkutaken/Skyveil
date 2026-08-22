package name.skyveil.client.inventorybuttons;

import name.skyveil.client.gui.SkyveilTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Add/edit form for one persistent inventory command button. */
public final class InventoryButtonEditorScreen extends Screen {
    private final Screen parent;
    private final InventoryButtonDefinition existing;
    private final InventoryButtonDefinition draft;
    private final InventoryButtonPosition position;
    private EditBox commandBox;
    private String error = "";

    public InventoryButtonEditorScreen(Screen parent, InventoryButtonDefinition existing, InventoryButtonPosition requested) {
        super(Component.literal(existing == null ? "Add Inventory Button" : "Edit Inventory Button"));
        this.parent = parent;
        this.existing = existing;
        this.draft = existing == null ? new InventoryButtonDefinition() : existing.copy();
        this.position = existing == null ? requested : InventoryButtonPosition.parse(existing.position);
        this.draft.position = position.name();
    }

    @Override
    protected void init() {
        int left = width / 2 - 122;
        int top = height / 2 - 77;
        commandBox = new EditBox(font, left + 12, top + 38, 220, 20, Component.literal("Command"));
        commandBox.setMaxLength(256);
        commandBox.setHint(Component.literal("Required command, with or without /"));
        commandBox.setValue(draft.command == null ? "" : draft.command);
        addRenderableWidget(commandBox);

        addRenderableWidget(Button.builder(Component.literal("Choose Icon"), button -> openIconPicker()).bounds(left + 12, top + 68, 90, 20).build());
        if (existing != null) {
            addRenderableWidget(Button.builder(Component.literal("Save"), button -> save()).bounds(left + 12, top + 108, 68, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose()).bounds(left + 88, top + 108, 68, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Delete"), button -> delete()).bounds(left + 164, top + 108, 68, 20).build());
        } else {
            addRenderableWidget(Button.builder(Component.literal("Save"), button -> save()).bounds(left + 12, top + 108, 104, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose()).bounds(left + 128, top + 108, 104, 20).build());
        }
        setInitialFocus(commandBox);
    }

    private void captureFields() {
        draft.command = commandBox.getValue();
        draft.position = position.name();
    }

    private void openIconPicker() {
        captureFields();
        minecraft.setScreen(new InventoryButtonIconPickerScreen(this, icon -> {
            draft.icon = icon;
            minecraft.setScreen(this);
        }));
    }

    private void save() {
        captureFields();
        draft.command = InventoryButtonManager.normalizeCommand(draft.command);
        if (draft.command.isBlank()) {
            error = "Enter a command before saving.";
            return;
        }
        if (existing != null) draft.id = existing.id;
        InventoryButtonManager.save(draft);
        minecraft.setScreen(parent);
    }

    private void delete() {
        InventoryButtonManager.delete(existing.id);
        minecraft.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int left = width / 2 - 122;
        int top = height / 2 - 77;
        graphics.fill(0, 0, width, height, SkyveilTheme.SCRIM);
        graphics.fill(left, top, left + 244, top + 154, SkyveilTheme.WINDOW);
        graphics.outline(left, top, 244, 154, SkyveilTheme.ACCENT);
        graphics.centeredText(font, title, width / 2, top + 10, SkyveilTheme.TEXT);
        graphics.text(font, "Command", left + 12, top + 27, SkyveilTheme.SECONDARY, false);
        graphics.fill(left + 112, top + 67, left + 134, top + 89, SkyveilTheme.CARD);
        graphics.outline(left + 112, top + 67, 22, 22, SkyveilTheme.OUTLINE);
        graphics.item(InventoryButtonManager.icon(draft.icon), left + 115, top + 70);
        graphics.text(font, InventoryButtonManager.icon(draft.icon).getHoverName(), left + 141, top + 74, SkyveilTheme.TEXT, false);
        if (!error.isBlank()) graphics.centeredText(font, error, width / 2, top + 137, 0xFFFF7777);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
}
