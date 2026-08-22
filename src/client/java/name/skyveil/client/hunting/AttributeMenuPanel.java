package name.skyveil.client.hunting;

import name.skyveil.client.config.ConfigManager;
import name.skyveil.client.gui.SkyveilTheme;
import name.skyveil.client.inventorybuttons.InventoryButtonManager;
import name.skyveil.client.itemrarity.SkyblockRarity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Cached Attribute Menu progress and required-cost panel backed by Bazaar shard prices. */
public final class AttributeMenuPanel {
    private static final Logger LOGGER=LoggerFactory.getLogger("skyveil-attribute-menu");
    private static final int SPACING=5,PREFERRED_WIDTH=244,MIN_WIDTH=150,HEADER=70,ROW_HEIGHT=24,FOOTER=16;
    private static final int PRICE_TEXT=0xFFFFE8B0;
    private static AbstractContainerScreen<?> cachedScreen;
    private static int scroll;
    private static boolean dirty=true;
    private static int pendingFingerprint=Integer.MIN_VALUE,pendingConfirmations;
    private static long cachedRevision=-1;
    private static List<Row> rows=List.of();
    private static List<String> diagnosticEntries=List.of();
    private static Bounds bounds;
    private static boolean complete;
    private static int known,maxed;
    private AttributeMenuPanel(){}

    public static void render(AbstractContainerScreen<?> screen,GuiGraphicsExtractor graphics,int guiLeft,int guiTop,int guiWidth,int guiHeight,int mouseX,int mouseY){
        if(!ConfigManager.get().hunting.attributeProgress||!AttributeMenuDetector.matches(screen)){clearUnless(screen);return;}
        int leftSpace=guiLeft-SPACING-2,rightSpace=graphics.guiWidth()-(guiLeft+guiWidth)-SPACING-2;
        int available=Math.max(leftSpace,rightSpace);if(available<MIN_WIDTH){bounds=null;return;}
        int width=Math.min(PREFERRED_WIDTH,available),x=leftSpace>=rightSpace?guiLeft-SPACING-width:guiLeft+guiWidth+SPACING,y=guiTop,height=guiHeight;
        if(screen!=cachedScreen)beginScreen(screen);
        if(dirty)stabilize(screen);
        if(cachedRevision!=AttributeSessionData.revision())rebuildRows();if(sortMode()==ShardSortMode.PRICE)sortRows();
        int viewportTop=y+HEADER,viewportBottom=y+height-FOOTER,visible=Math.max(1,(viewportBottom-viewportTop)/ROW_HEIGHT),maxScroll=Math.max(0,rows.size()-visible);
        scroll=Math.max(0,Math.min(scroll,maxScroll));bounds=new Bounds(x,y,width,height,viewportTop,viewportBottom,visible,maxScroll);

        int background=switch(ConfigManager.get().darkMode){case "DARK_PURPLE"->0xEE170D24;case "DARK"->0xEE15171C;default->0xE8211A2B;};
        graphics.fill(x,y,x+width,y+height,background);graphics.outline(x,y,width,height,SkyveilTheme.ACCENT);
        graphics.text(Minecraft.getInstance().font,"Attribute Progress",x+6,y+6,SkyveilTheme.TEXT,true);
        String suffix=complete?"":"+";
        graphics.text(Minecraft.getInstance().font,"Attributes: "+known+suffix+"/"+AttributeSessionData.TOTAL_CONSUMABLE_ATTRIBUTES,x+6,y+17,0xFF55FF55,false);
        graphics.text(Minecraft.getInstance().font,"Maxed: "+maxed+suffix+"/"+AttributeSessionData.TOTAL_CONSUMABLE_ATTRIBUTES,x+6,y+28,0xFFFFD45C,false);
        String scan=complete?"Collection scan complete":"Scan pages: "+AttributeSessionData.visitedPages()+"/"+AttributeSessionData.totalPages()+" (Advanced Mode)";
        graphics.text(Minecraft.getInstance().font,scan,x+6,y+39,complete?0xFF55FF55:0xFFFFAA55,false);
        ShardSortControls.render(graphics,x+5,y+51,width-10,sortMode(),ConfigManager.get().hunting.attributeSortDescending,mouseX,mouseY);

        ShardPriceService.Status priceStatus=ShardPriceService.status();graphics.enableScissor(x+1,viewportTop,x+width-1,viewportBottom);
        if(rows.isEmpty())graphics.text(Minecraft.getInstance().font,complete?"No missing attributes":"No missing attributes observed yet",x+6,viewportTop+5,SkyveilTheme.SECONDARY,false);
        for(int index=0;index<visible&&scroll+index<rows.size();index++){
            Row row=rows.get(scroll+index);int rowY=viewportTop+index*ROW_HEIGHT;
            graphics.fill(x+1,rowY,x+width-1,rowY+ROW_HEIGHT,(index&1)==0?0x302E243C:0x30372A49);graphics.item(row.stack(),x+3,rowY+2);
            String quantity=row.quantityKnown()?row.needed()+" shards":"Unknown quantity";int quantityWidth=Minecraft.getInstance().font.width(quantity),quantityX=x+width-5-quantityWidth,nameWidth=Math.max(30,quantityX-(x+22)-4);
            var nameLine=Minecraft.getInstance().font.split(row.name(),nameWidth);
            graphics.text(Minecraft.getInstance().font,quantity,quantityX,rowY+3,0xFFAAAAAA,false);
            if(!nameLine.isEmpty())graphics.text(Minecraft.getInstance().font,nameLine.getFirst(),x+22,rowY+3,row.rarity()==null?0xFFFFFFFF:0xFF000000|row.rarity().rgb(),true);
            if(ConfigManager.get().hunting.attributePricing){Long unit=unitPrice(row),total=unit==null||!row.quantityKnown()?null:safeMultiply(unit,row.needed());String price=unit==null?(priceStatus==ShardPriceService.Status.LOADING?"Price: loading…":"Price: no data"):ShardPriceService.format(unit)+" ea"+(total==null?"":" • "+ShardPriceService.format(total)+" total");graphics.text(Minecraft.getInstance().font,price,x+22,rowY+14,unit==null?0xFFFFAA55:PRICE_TEXT,false);}
            if(mouseX>=x+2&&mouseX<x+width-2&&mouseY>=rowY&&mouseY<rowY+ROW_HEIGHT){graphics.outline(x+1,rowY,width-2,ROW_HEIGHT,SkyveilTheme.ACCENT);graphics.setTooltipForNextFrame(net.minecraft.network.chat.Component.literal("Click to search"),mouseX,mouseY);}
        }
        graphics.disableScissor();
        graphics.text(Minecraft.getInstance().font,ConfigManager.get().hunting.attributePricing?"Hypixel Bazaar • "+priceStatus.name().toLowerCase(java.util.Locale.ROOT):"Local progress data only",x+5,y+height-12,SkyveilTheme.SECONDARY,false);
    }

    public static boolean mouseScrolled(AbstractContainerScreen<?> screen,double mouseX,double mouseY,double vertical){
        Bounds area=bounds;if(screen!=cachedScreen||area==null||area.maxScroll()==0||!area.inViewport(mouseX,mouseY))return false;
        if(vertical>0)scroll=Math.max(0,scroll-1);else if(vertical<0)scroll=Math.min(area.maxScroll(),scroll+1);return vertical!=0;
    }
    public static boolean mouseClicked(AbstractContainerScreen<?> screen,double mouseX,double mouseY,int button){
        Bounds area=bounds;if(screen!=cachedScreen||area==null||!area.contains(mouseX,mouseY))return false;
        if(button==0){var selected=ShardSortControls.click(mouseX,mouseY,area.x()+5,area.y()+51,area.width()-10,sortMode(),ConfigManager.get().hunting.attributeSortDescending);
            if(selected!=null){ConfigManager.get().hunting.attributeSort=selected.mode().name();ConfigManager.get().hunting.attributeSortDescending=selected.descending();sortRows();scroll=0;ConfigManager.save();}}
        if(button==0&&area.inViewport(mouseX,mouseY)){int visibleIndex=(int)((mouseY-area.viewportTop())/ROW_HEIGHT),rowIndex=scroll+visibleIndex;if(visibleIndex>=0&&visibleIndex<area.visibleRows()&&rowIndex>=0&&rowIndex<rows.size()){InventoryButtonManager.executeCommand("bz "+bazaarQuery(rows.get(rowIndex).name().getString()));return true;}}
        // Consume all clicks over the external panel so vanilla cannot interpret them as outside-container drops.
        return true;
    }
    public static void onContainerUpdate(int containerId){if(cachedScreen!=null&&cachedScreen.getMenu().containerId==containerId)dirty=true;}
    public static void reset(){cachedScreen=null;rows=List.of();diagnosticEntries=List.of();bounds=null;scroll=0;dirty=true;pendingFingerprint=Integer.MIN_VALUE;pendingConfirmations=0;cachedRevision=-1;}
    private static void clearUnless(AbstractContainerScreen<?> screen){if(screen==cachedScreen)reset();}

    private static void beginScreen(AbstractContainerScreen<?> screen){cachedScreen=screen;cachedRevision=-1;pendingFingerprint=Integer.MIN_VALUE;pendingConfirmations=0;dirty=true;scroll=0;}
    private static void stabilize(AbstractContainerScreen<?> screen){
        int fingerprint=fingerprint(screen);if(fingerprint!=pendingFingerprint){pendingFingerprint=fingerprint;pendingConfirmations=1;return;}
        if(++pendingConfirmations<2)return;
        Scan scan=scan(screen);boolean accepted=AttributeSessionData.observe(scan.page(),scan.rows());dirty=false;diagnosticEntries=scan.details();
        if(ConfigManager.get().hunting.attributePricing)ShardPriceService.ensureFresh();
        if(ConfigManager.get().map.debug)LOGGER.info("Attribute Menu snapshot: title='{}' styled={} container={} handler={} slots={} page={}/{} candidates={} identified={} accepted={} states={} navigation={}",screen.getTitle().getString(),screen.getTitle(),screen.getMenu().containerId,screen.getMenu().getClass().getName(),screen.getMenu().slots.size(),scan.page().current(),scan.page().total(),scan.candidates(),scan.rows().size(),accepted,scan.states(),scan.navigation());
    }
    private static int fingerprint(AbstractContainerScreen<?> screen){
        Minecraft client=Minecraft.getInstance();AttributeMenuDetector.Page page=AttributeMenuDetector.page(screen);int result=31*page.current()+page.total();
        for(var slot:screen.getMenu().slots){if(client.player!=null&&slot.container==client.player.getInventory())continue;result=31*result+slot.index;result=31*result+ItemStack.hashItemAndComponents(slot.getItem());}return result;
    }
    private static Scan scan(AbstractContainerScreen<?> screen){
        Minecraft client=Minecraft.getInstance();ArrayList<AttributeMenuParser.Parsed> pageRows=new ArrayList<>();ArrayList<String> details=new ArrayList<>(),navigation=new ArrayList<>();int candidates=0;
        for(var slot:screen.getMenu().slots){if(client.player!=null&&slot.container==client.player.getInventory())continue;ItemStack stack=slot.getItem();if(stack.isEmpty())continue;if(!validAttributeSlot(slot.index)){if(slot.index>=45&&navigation.size()<9)navigation.add(slot.index+":"+stack.getHoverName().getString());continue;}candidates++;
            AttributeMenuParser.Parsed initial=AttributeMenuParser.parse(stack,0);if(initial==null){if(details.size()<12){var lore=stack.get(DataComponents.LORE);ArrayList<String> relevant=new ArrayList<>();if(lore!=null)for(var line:lore.lines())if(line.getString().toLowerCase(java.util.Locale.ROOT).contains("syphon"))relevant.add(line.getString());details.add("REJECTED slot="+slot.index+" item="+BuiltInRegistries.ITEM.getKey(stack.getItem())+" name='"+stack.getHoverName().getString()+"' internal="+AttributeShardResolver.resolveInternalId(stack)+" custom="+stack.has(DataComponents.CUSTOM_DATA)+" lore="+relevant);}continue;}
            AttributeMenuParser.Parsed parsed=AttributeMenuParser.parse(stack,AttributeSessionData.owned(initial.key()));if(parsed==null)continue;pageRows.add(parsed);
            if(details.size()<12){var lore=stack.get(DataComponents.LORE);ArrayList<String> relevant=new ArrayList<>();if(lore!=null)for(var line:lore.lines())if(line.getString().toLowerCase(java.util.Locale.ROOT).contains("syphon"))relevant.add(line.getString());details.add("slot="+slot.index+" item="+BuiltInRegistries.ITEM.getKey(stack.getItem())+" name='"+stack.getHoverName().getString()+"' internal="+AttributeShardResolver.resolveInternalId(stack)+" custom="+stack.has(DataComponents.CUSTOM_DATA)+" lore="+relevant+" rarity="+parsed.rarity()+" tier="+parsed.tier()+" ownership="+parsed.ownership()+" key="+parsed.key());}
        }
        java.util.EnumMap<AttributeMenuParser.Ownership,Integer> states=new java.util.EnumMap<>(AttributeMenuParser.Ownership.class);for(var state:AttributeMenuParser.Ownership.values())states.put(state,0);for(var row:pageRows)states.put(row.ownership(),states.get(row.ownership())+1);
        return new Scan(AttributeMenuDetector.page(screen),List.copyOf(pageRows),candidates,Map.copyOf(states),List.copyOf(navigation),List.copyOf(details));
    }
    private static void rebuildRows(){var progress=AttributeSessionData.progress();
        known=0;maxed=0;int unowned=0,unknownOwnership=0,unknownQuantity=0;ArrayList<Row> next=new ArrayList<>();
        for(var parsed:progress.values()){
            if(parsed.ownership()==AttributeMenuParser.Ownership.OWNED||parsed.ownership()==AttributeMenuParser.Ownership.MAXED)known++;
            if(parsed.ownership()==AttributeMenuParser.Ownership.MAXED)maxed++;
            if(parsed.ownership()==AttributeMenuParser.Ownership.UNKNOWN){unknownOwnership++;continue;}
            if(!AttributeMenuParser.isMissing(parsed.ownership()))continue;unowned++;
            boolean quantityKnown=parsed.progress().known()&&parsed.progress().purchaseRemaining()>0;long needed=quantityKnown?parsed.progress().purchaseRemaining():0;if(!quantityKnown)unknownQuantity++;
            String subtype=parsed.key().startsWith("ATTRIBUTE:")?parsed.key().substring(10):"";var identity=HuntingShardPriceIdentity.resolveAttribute(subtype);ItemStack head=AttributeShardHeadCatalog.head(subtype);if(head.isEmpty())head=parsed.stack();String source=parsed.sourceName().isBlank()?(identity==null?parsed.name().getString():identity.displayName()+" Shard"):parsed.sourceName();source=AttributeMenuParser.sanitizeSourceName(source);var sourceName=net.minecraft.network.chat.Component.literal(source);next.add(new Row(head,sourceName,parsed.rarity(),subtype,needed,quantityKnown));
        }
        complete=AttributeSessionData.complete();cachedRevision=AttributeSessionData.revision();rows=List.copyOf(next);sortRows();scroll=0;
        if(ConfigManager.get().map.debug)LOGGER.info("Attribute row model: cached={} unowned={} unknownOwnership={} unknownQuantity={} rows={}",progress.size(),unowned,unknownOwnership,unknownQuantity,rows.size());
    }
    public static DebugSnapshot debugSnapshot(){String title=cachedScreen==null?"none":cachedScreen.getTitle().getString();return new DebugSnapshot(title,AttributeSessionData.visitedPages(),AttributeSessionData.totalPages(),AttributeSessionData.progress().size(),rows.size(),dirty,List.copyOf(diagnosticEntries));}
    private static ShardSortMode sortMode(){return ShardSortMode.parse(ConfigManager.get().hunting.attributeSort);}
    private static void sortRows(){
        boolean descending=ConfigManager.get().hunting.attributeSortDescending;Comparator<Row> value=switch(sortMode()){
            case RARITY->Comparator.comparingInt(row->row.rarity()==null?Integer.MAX_VALUE:row.rarity().ordinal());case QUANTITY->Comparator.comparingLong(Row::needed);case PRICE->Comparator.comparingLong(row->{Long price=totalPrice(row);return price==null?0:price;});};
        if(descending)value=value.reversed();Comparator<Row> reliable=switch(sortMode()){case RARITY->Comparator.comparing((Row row)->row.rarity()==null);case QUANTITY->Comparator.comparing((Row row)->!row.quantityKnown());case PRICE->Comparator.comparing((Row row)->totalPrice(row)==null);};
        ArrayList<Row> sorted=new ArrayList<>(rows);sorted.sort(reliable.thenComparing(value).thenComparing(row->row.name().getString(),String.CASE_INSENSITIVE_ORDER));rows=List.copyOf(sorted);
    }
    private static Long unitPrice(Row row){var identity=HuntingShardPriceIdentity.resolveAttribute(row.subtype());var quote=identity==null?null:ShardPriceService.quote(identity.bazaarName());return quote==null?null:quote.instantBuy();}
    private static Long totalPrice(Row row){Long unit=unitPrice(row);return unit==null||!row.quantityKnown()?null:safeMultiply(unit,row.needed());}
    static String bazaarQuery(String shardName){String value=shardName==null?"":shardName.trim();return value.replaceFirst("(?i)\\s+shard$","").trim();}
    private static long safeMultiply(long left,long right){try{return Math.multiplyExact(left,right);}catch(ArithmeticException ignored){return Long.MAX_VALUE;}}
    private static boolean validAttributeSlot(int slot){return slot>=9&&slot<=44&&slot%9!=0&&slot%9!=8;}
    private record Row(ItemStack stack,net.minecraft.network.chat.Component name,SkyblockRarity rarity,String subtype,long needed,boolean quantityKnown){}
    private record Scan(AttributeMenuDetector.Page page,List<AttributeMenuParser.Parsed> rows,int candidates,Map<AttributeMenuParser.Ownership,Integer> states,List<String> navigation,List<String> details){}
    public record DebugSnapshot(String title,int visitedPages,int totalPages,int discovered,int rows,boolean awaitingStableContent,List<String> entries){}
    private record Bounds(int x,int y,int width,int height,int viewportTop,int viewportBottom,int visibleRows,int maxScroll){boolean contains(double px,double py){return px>=x&&px<x+width&&py>=y&&py<y+height;}boolean inViewport(double px,double py){return px>=x&&px<x+width&&py>=viewportTop&&py<viewportBottom;}}
}
