package name.skyveil.client.itemsearch;

import name.skyveil.client.gui.SkyveilTheme;
import name.skyveil.client.inventorybuttons.InventoryButtonManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Inventory-attached search field and right-hand SkyBlock item result grid. */
public final class ItemSearchOverlay {
    private static final int CELL_WIDTH=152,CELL_HEIGHT=28,MAX_RESULTS=800;
    private static final long AUCTION_QUERY_TTL_NANOS=15_000_000_000L;
    private static AbstractContainerScreen<?> screen;private static EditBox field;private static List<ItemSearchCatalog.Entry> results=List.of();private static String lastQuery="";private static int scrollRow;private static Bounds bounds;
    private static final Map<ItemStack,Boolean> LORE_MATCHES=new WeakHashMap<>();
    private static String rememberedAuctionLore="";private static long rememberedAuctionAt;
    private ItemSearchOverlay(){}

    public static EditBox create(AbstractContainerScreen<?> owner,int guiTop,int guiHeight){
        reset();boolean auction=isAuctionTitle(owner.getTitle().getString()),restore=auction&&!rememberedAuctionLore.isBlank()&&System.nanoTime()-rememberedAuctionAt<=AUCTION_QUERY_TTL_NANOS;if(!auction){rememberedAuctionLore="";rememberedAuctionAt=0;}screen=owner;int width=Math.min(240,Math.max(140,owner.width-16)),x=(owner.width-width)/2,y=Math.min(owner.height-22,guiTop+guiHeight+108);
        field=new EditBox(Minecraft.getInstance().font,x,y,width,18,net.minecraft.network.chat.Component.literal("Item Search"));field.setMaxLength(80);field.setHint(net.minecraft.network.chat.Component.literal("Search items or lore:text"));field.setResponder(ItemSearchOverlay::filter);if(restore)field.setValue(rememberedAuctionLore);field.setFocused(false);return field;
    }
    private static void filter(String value){String query=value==null?"":value.trim();lastQuery=query;results=query.isBlank()||ItemSearchCatalog.isLoreSearch(query)?List.of():ItemSearchCatalog.search(query);LORE_MATCHES.clear();scrollRow=0;rememberAuctionQuery(query);}
    public static void render(AbstractContainerScreen<?> owner,GuiGraphicsExtractor graphics,int mouseX,int mouseY){
        if(owner!=screen||field==null){bounds=null;return;}name.skyveil.client.gui.HudVisibility.cover(graphics,field.getX()-1,field.getY()-1,field.getWidth()+2,field.getHeight()+2);String query=field.getValue().trim();if(!query.equals(lastQuery))filter(query);
        if(query.isBlank()||ItemSearchCatalog.isLoreSearch(query)){bounds=null;return;}
        int panelWidth=Math.min(640,Math.max(260,graphics.guiWidth()/3)),x=graphics.guiWidth()-panelWidth-8,y=8,height=graphics.guiHeight()-16;
        int columns=Math.max(1,(panelWidth-12)/CELL_WIDTH),rows=Math.max(1,(height-38)/CELL_HEIGHT),totalRows=(Math.min(results.size(),MAX_RESULTS)+columns-1)/columns,maxScroll=Math.max(0,totalRows-rows);scrollRow=Math.max(0,Math.min(scrollRow,maxScroll));
        int visibleRows=Math.max(1,Math.min(rows,Math.max(1,totalRows))),contentHeight=25+visibleRows*CELL_HEIGHT+(maxScroll>0?12:0);bounds=new Bounds(x,y,panelWidth,contentHeight,columns,rows,maxScroll);
        name.skyveil.client.gui.HudVisibility.cover(graphics,x,y,panelWidth,contentHeight);
        String count=results.size()>MAX_RESULTS?MAX_RESULTS+"+ of "+results.size():results.size()+"";graphics.text(Minecraft.getInstance().font,"Item Search • "+count+" results",x+7,y+7,SkyveilTheme.TEXT,true);
        if(results.isEmpty())graphics.text(Minecraft.getInstance().font,"No matching SkyBlock items",x+8,y+28,SkyveilTheme.SECONDARY,false);
        int first=scrollRow*columns,limit=Math.min(Math.min(results.size(),MAX_RESULTS),first+rows*columns);
        for(int index=first;index<limit;index++){int visible=index-first,column=visible%columns,row=visible/columns,cx=x+6+column*CELL_WIDTH,cy=y+24+row*CELL_HEIGHT;boolean hover=inside(mouseX,mouseY,cx,cy,CELL_WIDTH-3,CELL_HEIGHT-2);graphics.fill(cx,cy,cx+CELL_WIDTH-3,cy+CELL_HEIGHT-2,hover?SkyveilTheme.HOVER:((row&1)==0?0x50372A49:0x502E243C));var entry=results.get(index);var stack=entry.stack();graphics.item(stack,cx+4,cy+5);var line=Minecraft.getInstance().font.split(stack.getHoverName(),CELL_WIDTH-28);if(!line.isEmpty())graphics.text(Minecraft.getInstance().font,line.getFirst(),cx+24,cy+9,0xFFFFFFFF,true);if(hover)graphics.setTooltipForNextFrame(Minecraft.getInstance().font,stack,mouseX,mouseY);}
        if(maxScroll>0)graphics.text(Minecraft.getInstance().font,"Scroll for more",x+panelWidth-78,y+contentHeight-11,SkyveilTheme.SECONDARY,false);
    }
    public static boolean keyPressed(AbstractContainerScreen<?> owner,KeyEvent event){if(owner!=screen||field==null||!field.isFocused()||event.key()==GLFW.GLFW_KEY_ESCAPE)return false;field.keyPressed(event);return true;}
    public static boolean blurOnOutsideClick(AbstractContainerScreen<?> owner,double mouseX,double mouseY){if(owner!=screen||field==null||!field.isFocused()||inside(mouseX,mouseY,field.getX(),field.getY(),field.getWidth(),field.getHeight()))return false;field.setFocused(false);return true;}
    public static boolean mouseClicked(AbstractContainerScreen<?> owner,double mouseX,double mouseY,int button){if(owner!=screen||bounds==null||!bounds.contains(mouseX,mouseY))return false;ItemSearchCatalog.Entry entry=resultAt(mouseX,mouseY);if(button==0&&entry!=null&&entry.craftable())InventoryButtonManager.executeCommand("recipe "+entry.recipeQuery());return true;}
    public static boolean mouseScrolled(AbstractContainerScreen<?> owner,double mouseX,double mouseY,double vertical){if(owner!=screen||bounds==null||!bounds.contains(mouseX,mouseY)||bounds.maxScroll==0)return false;if(vertical>0)scrollRow=Math.max(0,scrollRow-1);else if(vertical<0)scrollRow=Math.min(bounds.maxScroll,scrollRow+1);return vertical!=0;}
    public static void drawLoreMatch(GuiGraphicsExtractor graphics,ItemStack stack,int x,int y){if(field==null||screen==null||stack==null||stack.isEmpty()||!ItemSearchCatalog.isLoreSearch(lastQuery)||!LORE_MATCHES.computeIfAbsent(stack,item->ItemSearchCatalog.matchesLore(item,lastQuery)))return;graphics.fill(x,y,x+16,y+16,0xA838A848);graphics.outline(x,y,16,16,0xFF63E875);}
    public static void reset(){if(field!=null)rememberAuctionQuery(field.getValue().trim());screen=null;field=null;results=List.of();LORE_MATCHES.clear();lastQuery="";scrollRow=0;bounds=null;}
    private static void rememberAuctionQuery(String query){if(screen==null||!isAuctionTitle(screen.getTitle().getString()))return;if(ItemSearchCatalog.isLoreSearch(query)&&query.length()>5){rememberedAuctionLore=query;rememberedAuctionAt=System.nanoTime();}else{rememberedAuctionLore="";rememberedAuctionAt=0;}}
    static boolean isAuctionTitle(String title){return title!=null&&title.toLowerCase(java.util.Locale.ROOT).contains("auction");}
    static int catalogSize(){return ItemSearchCatalog.size();}
    private static ItemSearchCatalog.Entry resultAt(double mouseX,double mouseY){Bounds area=bounds;if(area==null)return null;int localX=(int)Math.floor(mouseX)-(area.x+6),localY=(int)Math.floor(mouseY)-(area.y+24);if(localX<0||localY<0)return null;int column=localX/CELL_WIDTH,row=localY/CELL_HEIGHT;if(column<0||column>=area.columns||row<0||row>=area.rows||localX%CELL_WIDTH>=CELL_WIDTH-3||localY%CELL_HEIGHT>=CELL_HEIGHT-2)return null;int index=(scrollRow+row)*area.columns+column;return index>=0&&index<Math.min(results.size(),MAX_RESULTS)?results.get(index):null;}
    private static boolean inside(double mx,double my,int x,int y,int width,int height){return mx>=x&&mx<x+width&&my>=y&&my<y+height;}
    private record Bounds(int x,int y,int width,int height,int columns,int rows,int maxScroll){boolean contains(double px,double py){return inside(px,py,x,y,width,height);}}
}
