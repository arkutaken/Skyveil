package name.skyveil.client.inventorybuttons;

import name.skyveil.client.gui.SkyveilTheme;
import name.skyveil.client.itemsearch.SkyBlockHeadIcons;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Searchable, cached picker that uses Minecraft's real item renderer. */
public final class InventoryButtonIconPickerScreen extends Screen {
    private static volatile List<IconEntry> cachedVanillaIcons,cachedHeadIcons;
    private final Screen parent;
    private final Consumer<String> selection;
    private EditBox search;
    private Button vanillaButton,headsButton;
    private List<IconEntry> filtered = List.of();
    private int scrollRow;
    private Source source=Source.VANILLA;

    public InventoryButtonIconPickerScreen(Screen parent, Consumer<String> selection) {
        super(Component.literal("Choose Button Icon"));
        this.parent = parent;
        this.selection = selection;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(392, width - 24);
        int left = (width - panelWidth) / 2;
        search = new EditBox(font, left + 12, 34, panelWidth - 24, 20, Component.literal("Search items"));
        search.setHint(Component.literal("Search Minecraft items"));
        search.setResponder(value -> filter());
        addRenderableWidget(search);
        int sourceWidth=(panelWidth-28)/2;vanillaButton=addRenderableWidget(new name.skyveil.client.gui.SkyveilButton(left+12,59,sourceWidth,20,Component.literal("Vanilla Items"),button->setSource(Source.VANILLA)));
        headsButton=addRenderableWidget(new name.skyveil.client.gui.SkyveilButton(left+16+sourceWidth,59,sourceWidth,20,Component.literal("SkyBlock Heads"),button->setSource(Source.SKYBLOCK_HEADS)));
        updateSourceButtons();
        filter();
        setInitialFocus(search);
    }

    private void setSource(Source next){source=next;search.setHint(Component.literal(source==Source.VANILLA?"Search Minecraft items":"Search SkyBlock heads and NPCs"));updateSourceButtons();filter();}
    private void updateSourceButtons(){if(vanillaButton!=null)vanillaButton.active=source!=Source.VANILLA;if(headsButton!=null)headsButton.active=source!=Source.SKYBLOCK_HEADS;}

    private void filter() {
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        filtered = icons().stream().filter(icon -> query.isBlank() || icon.searchText.contains(query)).toList();
        scrollRow = 0;
    }

    private List<IconEntry> icons() {return source==Source.VANILLA?vanillaIcons():headIcons();}
    private static List<IconEntry> vanillaIcons() {
        List<IconEntry> result = cachedVanillaIcons;
        if (result != null) return result;
        synchronized (InventoryButtonIconPickerScreen.class) {
            if (cachedVanillaIcons != null) return cachedVanillaIcons;
            ArrayList<IconEntry> built = new ArrayList<>();
            for (var id : BuiltInRegistries.ITEM.keySet()) {
                var item = BuiltInRegistries.ITEM.getValue(id);
                if (item == null || item == Items.AIR) continue;
                ItemStack stack = new ItemStack(item);
                built.add(new IconEntry(id.toString(),()->stack,(id + " " + stack.getHoverName().getString()).toLowerCase(Locale.ROOT),id.toString(),id.toString()));
            }
            built.sort(Comparator.comparing(entry -> entry.id));
            cachedVanillaIcons = List.copyOf(built);
            return cachedVanillaIcons;
        }
    }
    private static List<IconEntry> headIcons(){List<IconEntry> result=cachedHeadIcons;if(result!=null)return result;synchronized(InventoryButtonIconPickerScreen.class){if(cachedHeadIcons==null)cachedHeadIcons=skyBlockIcons(SkyBlockHeadIcons.all());return cachedHeadIcons;}}
    private static List<IconEntry> skyBlockIcons(List<SkyBlockHeadIcons.HeadIcon> source){ArrayList<IconEntry> built=new ArrayList<>();for(var head:source)built.add(new IconEntry(head.key(),head::stack,(head.name()+" "+head.internalName()).toLowerCase(Locale.ROOT),head.internalName(),head.name()));built.sort(Comparator.comparing(IconEntry::sortName,String.CASE_INSENSITIVE_ORDER));return List.copyOf(built);}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int panelWidth = Math.min(392, width - 24), left = (width - panelWidth) / 2;
        int top = 14, bottom = height - 14;
        graphics.fill(0, 0, width, height, SkyveilTheme.SCRIM);
        graphics.fill(left, top, left + panelWidth, bottom, SkyveilTheme.WINDOW);
        graphics.outline(left, top, panelWidth, bottom - top, SkyveilTheme.ACCENT);
        graphics.centeredText(font, title, width / 2, top + 8, SkyveilTheme.ACCENT);
        Grid grid = grid(left, panelWidth);
        int first = scrollRow * grid.columns;
        int visible = grid.columns * grid.rows;
        for (int index = first; index < Math.min(filtered.size(), first + visible); index++) {
            int visibleIndex = index - first;
            int x = grid.x + visibleIndex % grid.columns * 24;
            int y = grid.y + visibleIndex / grid.columns * 24;
            boolean hovered = inside(mouseX, mouseY, x, y, 22, 22);
            graphics.fill(x, y, x + 22, y + 22, hovered ? SkyveilTheme.HOVER : SkyveilTheme.CARD);
            graphics.outline(x, y, 22, 22, hovered ? SkyveilTheme.ACCENT : SkyveilTheme.OUTLINE);
            IconEntry icon = filtered.get(index);
            ItemStack stack=icon.renderStack();graphics.item(stack, x + 3, y + 3);
            if (hovered) graphics.setComponentTooltipForNextFrame(font, List.of(stack.getHoverName(), Component.literal(icon.detail)), mouseX, mouseY);
        }
        if (filtered.isEmpty()) graphics.centeredText(font, "No matching icons", width / 2, grid.y + 20, SkyveilTheme.SECONDARY);
        graphics.centeredText(font, filtered.size() + (source==Source.VANILLA?" vanilla items":" SkyBlock heads") + "  •  Scroll to browse  •  Esc to cancel", width / 2, bottom - 13, SkyveilTheme.SECONDARY);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int panelWidth = Math.min(392, width - 24), left = (width - panelWidth) / 2;
            Grid grid = grid(left, panelWidth);
            if (inside(event.x(), event.y(), grid.x, grid.y, grid.columns * 24, grid.rows * 24)) {
                int column = ((int) event.x() - grid.x) / 24;
                int row = ((int) event.y() - grid.y) / 24;
                if (((int) event.x() - grid.x) % 24 < 22 && ((int) event.y() - grid.y) % 24 < 22) {
                    int index = (scrollRow + row) * grid.columns + column;
                    if (index >= 0 && index < filtered.size()) {
                        selection.accept(filtered.get(index).id);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        int panelWidth = Math.min(392, width - 24), columns = grid((width - panelWidth) / 2, panelWidth).columns;
        int rows = grid((width - panelWidth) / 2, panelWidth).rows;
        int totalRows = (filtered.size() + columns - 1) / columns;
        scrollRow = Math.max(0, Math.min(Math.max(0, totalRows - rows), scrollRow - (int) Math.signum(vertical)));
        return true;
    }

    private Grid grid(int left, int panelWidth) {
        int columns = Math.max(1, (panelWidth - 24) / 24);
        int rows = Math.max(1, (height - 110) / 24);
        int gridWidth = columns * 24;
        return new Grid(left + (panelWidth - gridWidth) / 2, 85, columns, rows);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
    private static boolean inside(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    private enum Source{VANILLA,SKYBLOCK_HEADS}
    private record IconEntry(String id,Supplier<ItemStack> stackSupplier,String searchText,String detail,String sortName){ItemStack renderStack(){return stackSupplier.get();}}
    private record Grid(int x, int y, int columns, int rows) {}
}
