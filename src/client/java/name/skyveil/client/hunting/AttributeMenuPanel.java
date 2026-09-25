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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Cached Attribute Menu progress and required-cost panel backed by Bazaar shard prices. */
public final class AttributeMenuPanel {
    private static final int SPACING=5,PREFERRED_WIDTH=244,MIN_WIDTH=150,HEADER=70,ROW_HEIGHT=24,FOOTER=16;
    private static final int PRICE_TEXT=0xFFFFE8B0;
    private static AbstractContainerScreen<?> cachedScreen;
    private static int scroll;
    private static boolean dirty=true;
    private static int pendingFingerprint=Integer.MIN_VALUE,pendingConfirmations;
    private static long cachedRevision=-1,cachedPriceRevision=-1;
    private static List<Row> rows=List.of();
    private static List<String> diagnosticEntries=List.of();
    // Accumulate visited pages for the current filter; unseen pages are not empty pages.
    private static final Map<Integer,List<AttributeMenuParser.Parsed>> FILTER_PAGES=new HashMap<>();
    private static String filterKey="",filterLabel="";
    private static int filterTotalPages=1;
    private static Bounds bounds;
    private static boolean complete;
    private static int known,maxed;
    private AttributeMenuPanel(){}
    public static void tick(Minecraft client){if(client==null||client.player==null)return;
        boolean attributeMenuOpen=client.screen instanceof AbstractContainerScreen<?> current&&AttributeMenuDetector.matches(current);
        if(AttributeMenuLifecycle.shouldReset(attributeMenuOpen)){if(cachedScreen!=null||!filterKey.isBlank()||!FILTER_PAGES.isEmpty())reset();return;}
        AttributeSessionData.ensureWorld();if(cachedRevision!=AttributeSessionData.revision())rebuildRows();long priceRevision=ShardPriceService.revision();if(priceRevision!=cachedPriceRevision)refreshPrices(priceRevision);
    }

    public static void render(AbstractContainerScreen<?> screen,GuiGraphicsExtractor graphics,int guiLeft,int guiTop,int guiWidth,int guiHeight,int mouseX,int mouseY){
        if(!ConfigManager.get().hunting.attributeProgress||!AttributeMenuDetector.matches(screen)){clearUnless(screen);return;}
        int leftSpace=guiLeft-SPACING-2,rightSpace=graphics.guiWidth()-(guiLeft+guiWidth)-SPACING-2;
        int available=Math.max(leftSpace,rightSpace);if(available<MIN_WIDTH){bounds=null;return;}
        int width=Math.min(PREFERRED_WIDTH,available),x=leftSpace>=rightSpace?guiLeft-SPACING-width:guiLeft+guiWidth+SPACING,y=guiTop,height=guiHeight;
        if(screen!=cachedScreen)beginScreen(screen);
        // Packet updates may expose an intermediate menu. Wait for stable contents
        // before rebuilding rows from what could otherwise be a partial page.
        if(dirty)stabilize(screen);
        int viewportTop=y+HEADER,viewportBottom=y+height-FOOTER,visible=Math.max(1,(viewportBottom-viewportTop)/ROW_HEIGHT),maxScroll=Math.max(0,rows.size()-visible);
        scroll=Math.max(0,Math.min(scroll,maxScroll));bounds=new Bounds(x,y,width,height,viewportTop,viewportBottom,visible,maxScroll);

        int background=SkyveilTheme.HUD_BACKGROUND;
        graphics.fill(x,y,x+width,y+height,background);
        graphics.text(Minecraft.getInstance().font,"Attribute Progress",x+6,y+6,SkyveilTheme.ACCENT,true);
        String suffix=complete?"":"+";
        graphics.text(Minecraft.getInstance().font,"Attributes: "+known+suffix+"/"+AttributeSessionData.TOTAL_CONSUMABLE_ATTRIBUTES,x+6,y+17,0xFF55FF55,false);
        graphics.text(Minecraft.getInstance().font,"Maxed: "+maxed+suffix+"/"+AttributeSessionData.TOTAL_CONSUMABLE_ATTRIBUTES,x+6,y+28,0xFFFFD45C,false);
        String scan=!filterKey.isBlank()?"Filter: "+filterLabel+" - pages "+FILTER_PAGES.size()+"/"+filterTotalPages:complete?"Collection scan complete":"Scan pages: "+AttributeSessionData.visitedPages()+"/"+AttributeSessionData.totalPages()+" (Advanced Mode)";
        graphics.text(Minecraft.getInstance().font,scan,x+6,y+39,complete?0xFF55FF55:0xFFFFAA55,false);
        ShardSortControls.render(graphics,x+5,y+51,width-10,sortMode(),ConfigManager.get().hunting.attributeSortDescending,mouseX,mouseY);

        ShardPriceService.Status priceStatus=ShardPriceService.status();graphics.enableScissor(x+1,viewportTop,x+width-1,viewportBottom);try{
        if(rows.isEmpty())graphics.text(Minecraft.getInstance().font,complete?"No missing attributes":"No missing attributes observed yet",x+6,viewportTop+5,SkyveilTheme.SECONDARY,false);
        for(int index=0;index<visible&&scroll+index<rows.size();index++){
            Row row=rows.get(scroll+index);int rowY=viewportTop+index*ROW_HEIGHT;
            graphics.fill(x+1,rowY,x+width-1,rowY+ROW_HEIGHT,(index&1)==0?0x30282828:0x30383838);graphics.item(row.stack(),x+3,rowY+2);
            String quantity=row.quantityKnown()?row.needed()+" shards":"Unknown quantity";int quantityWidth=Minecraft.getInstance().font.width(quantity),quantityX=x+width-5-quantityWidth,nameWidth=Math.max(30,quantityX-(x+22)-4);
            var nameLine=Minecraft.getInstance().font.split(row.name(),nameWidth);
            graphics.text(Minecraft.getInstance().font,quantity,quantityX,rowY+3,0xFFAAAAAA,false);
            if(!nameLine.isEmpty())graphics.text(Minecraft.getInstance().font,nameLine.getFirst(),x+22,rowY+3,row.rarity()==null?0xFFFFFFFF:0xFF000000|row.rarity().rgb(),true);
            if(ConfigManager.get().hunting.attributePricing){Long unit=row.unitPrice(),total=totalPrice(row);String price=unit==null?(priceStatus==ShardPriceService.Status.LOADING?"Price: loading…":"Price: no data"):ShardPriceService.format(unit)+" ea"+(total==null?"":" • "+ShardPriceService.format(total)+" total");graphics.text(Minecraft.getInstance().font,price,x+22,rowY+14,unit==null?0xFFFFAA55:PRICE_TEXT,false);}
            if(mouseX>=x+2&&mouseX<x+width-2&&mouseY>=rowY&&mouseY<rowY+ROW_HEIGHT){graphics.outline(x+1,rowY,width-2,ROW_HEIGHT,SkyveilTheme.ACCENT);graphics.setTooltipForNextFrame(net.minecraft.network.chat.Component.literal("Click to search"),mouseX,mouseY);}
        }
        }finally{graphics.disableScissor();}
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
    /** Preserve filtered pages while Hypixel replaces one Attribute Menu page screen with another. */
    public static void screenClosed(AbstractContainerScreen<?> screen){if(screen==cachedScreen&&AttributeMenuLifecycle.preserveOnScreenReplacement()){cachedScreen=null;bounds=null;dirty=true;pendingFingerprint=Integer.MIN_VALUE;pendingConfirmations=0;}}
    public static void reset(){cachedScreen=null;rows=List.of();diagnosticEntries=List.of();FILTER_PAGES.clear();filterKey="";filterLabel="";filterTotalPages=1;bounds=null;scroll=0;dirty=true;pendingFingerprint=Integer.MIN_VALUE;pendingConfirmations=0;cachedRevision=-1;cachedPriceRevision=-1;}
    private static void clearUnless(AbstractContainerScreen<?> screen){if(cachedScreen!=null)reset();}

    private static void beginScreen(AbstractContainerScreen<?> screen){cachedScreen=screen;cachedRevision=-1;pendingFingerprint=Integer.MIN_VALUE;pendingConfirmations=0;dirty=true;scroll=0;}
    private static void stabilize(AbstractContainerScreen<?> screen){
        int fingerprint=fingerprint(screen);if(fingerprint!=pendingFingerprint){pendingFingerprint=fingerprint;pendingConfirmations=1;return;}
        if(++pendingConfirmations<2)return;
        Scan scan=scan(screen);observeFilterView(scan);AttributeSessionData.observe(scan.page(),scan.rows());rebuildRows();dirty=false;diagnosticEntries=scan.details();
        if(ConfigManager.get().hunting.attributePricing)ShardPriceService.ensureFresh();
    }
    private static int fingerprint(AbstractContainerScreen<?> screen){
        Minecraft client=Minecraft.getInstance();AttributeMenuDetector.Page page=AttributeMenuDetector.page(screen);int result=31*page.current()+page.total();
        for(var slot:screen.getMenu().slots){if(client.player!=null&&slot.container==client.player.getInventory())continue;result=31*result+slot.index;result=31*result+ItemStack.hashItemAndComponents(slot.getItem());}return result;
    }
    private static Scan scan(AbstractContainerScreen<?> screen){
        Minecraft client=Minecraft.getInstance();boolean debugging=false;ArrayList<AttributeMenuParser.Parsed> pageRows=new ArrayList<>();ArrayList<String> details=new ArrayList<>(),navigation=new ArrayList<>();int candidates=0;
        for(var slot:screen.getMenu().slots){if(client.player!=null&&slot.container==client.player.getInventory())continue;ItemStack stack=slot.getItem();if(stack.isEmpty())continue;if(!validAttributeSlot(slot.index)){if(debugging&&slot.index>=45&&navigation.size()<9)navigation.add(slot.index+":"+stack.getHoverName().getString());continue;}candidates++;
            AttributeMenuParser.Parsed initial=AttributeMenuParser.parse(stack,0);if(initial==null){if(debugging&&details.size()<12){var lore=stack.get(DataComponents.LORE);ArrayList<String> relevant=new ArrayList<>();if(lore!=null)for(var line:lore.lines())if(line.getString().toLowerCase(java.util.Locale.ROOT).contains("syphon"))relevant.add(line.getString());details.add("REJECTED slot="+slot.index+" item="+BuiltInRegistries.ITEM.getKey(stack.getItem())+" name='"+stack.getHoverName().getString()+"' internal="+AttributeShardResolver.resolveInternalId(stack)+" custom="+stack.has(DataComponents.CUSTOM_DATA)+" lore="+relevant);}continue;}
            AttributeMenuParser.Parsed parsed=AttributeMenuParser.parse(stack,AttributeSessionData.owned(initial.key()));if(parsed==null)continue;pageRows.add(parsed);
            if(debugging&&details.size()<12){var lore=stack.get(DataComponents.LORE);ArrayList<String> relevant=new ArrayList<>();if(lore!=null)for(var line:lore.lines())if(line.getString().toLowerCase(java.util.Locale.ROOT).contains("syphon"))relevant.add(line.getString());details.add("slot="+slot.index+" item="+BuiltInRegistries.ITEM.getKey(stack.getItem())+" name='"+stack.getHoverName().getString()+"' internal="+AttributeShardResolver.resolveInternalId(stack)+" custom="+stack.has(DataComponents.CUSTOM_DATA)+" lore="+relevant+" rarity="+parsed.rarity()+" tier="+parsed.tier()+" ownership="+parsed.ownership()+" key="+parsed.key());}
        }
        java.util.EnumMap<AttributeMenuParser.Ownership,Integer> states=new java.util.EnumMap<>(AttributeMenuParser.Ownership.class);for(var state:AttributeMenuParser.Ownership.values())states.put(state,0);for(var row:pageRows)states.put(row.ownership(),states.get(row.ownership())+1);
        AttributeMenuDetector.Page page=AttributeMenuDetector.page(screen);
        return new Scan(page,List.copyOf(pageRows),filterView(screen,page),candidates,Map.copyOf(states),List.copyOf(navigation),List.copyOf(details));
    }
    private static void rebuildRows(){var globalProgress=AttributeSessionData.progress();Map<String,AttributeMenuParser.Parsed> progress=globalProgress;
        if(!filterKey.isBlank()){HashMap<String,AttributeMenuParser.Parsed> filtered=new HashMap<>();for(var pageRows:FILTER_PAGES.values())AttributeSessionData.mergeObservations(filtered,pageRows);progress=filtered;}
        known=0;maxed=0;ArrayList<Row> next=new ArrayList<>();
        for(var parsed:globalProgress.values()){if(parsed.ownership()==AttributeMenuParser.Ownership.OWNED||parsed.ownership()==AttributeMenuParser.Ownership.MAXED)known++;if(parsed.ownership()==AttributeMenuParser.Ownership.MAXED)maxed++;}
        for(var parsed:progress.values()){
            if(parsed.ownership()==AttributeMenuParser.Ownership.UNKNOWN)continue;
            if(!AttributeMenuParser.isMissing(parsed.ownership()))continue;
            boolean quantityKnown=parsed.progress().known()&&parsed.progress().purchaseRemaining()>0;long needed=quantityKnown?parsed.progress().purchaseRemaining():0;
            String subtype=parsed.key().startsWith("ATTRIBUTE:")?parsed.key().substring(10):"";var identity=HuntingShardPriceIdentity.resolveAttribute(subtype);ItemStack head=AttributeShardHeadCatalog.head(subtype);if(head.isEmpty())head=parsed.stack();String source=parsed.sourceName().isBlank()?(identity==null?parsed.name().getString():identity.displayName()+" Shard"):parsed.sourceName();source=AttributeMenuParser.sanitizeSourceName(source);var sourceName=net.minecraft.network.chat.Component.literal(source);next.add(new Row(head,sourceName,parsed.rarity(),subtype,needed,quantityKnown,marketUnitPrice(subtype)));
        }
        complete=AttributeSessionData.complete();cachedRevision=AttributeSessionData.revision();cachedPriceRevision=ShardPriceService.revision();rows=List.copyOf(next);sortRows();scroll=0;
    }
    private static void observeFilterView(Scan scan){FilterView next=scan.filter();
        if(next.confirmed()){
            if(!next.filtered()){if(!filterKey.isBlank()){FILTER_PAGES.clear();filterKey="";filterLabel="";filterTotalPages=1;scroll=0;}return;}
            if(!next.key().equals(filterKey)){FILTER_PAGES.clear();filterKey=next.key();filterLabel=next.label();scroll=0;}
        }
        // Hypixel briefly replaces or empties menu controls while changing pages. Keep the
        // last explicitly selected filter during that loading state instead of treating the
        // temporary tooltip as a new filter and discarding every page already observed.
        if(filterKey.isBlank())return;
        filterTotalPages=Math.max(1,scan.page().total());if(!scan.rows().isEmpty())FILTER_PAGES.put(scan.page().current(),scan.rows());
    }
    private static FilterView filterView(AbstractContainerScreen<?> screen,AttributeMenuDetector.Page page){Minecraft client=Minecraft.getInstance();for(var slot:screen.getMenu().slots){if(client.player!=null&&slot.container==client.player.getInventory())continue;ItemStack stack=slot.getItem();if(stack.isEmpty()||!stack.getHoverName().getString().toLowerCase(java.util.Locale.ROOT).contains("filter"))continue;List<String> tooltip=net.minecraft.client.gui.screens.Screen.getTooltipFromItem(client,stack).stream().map(net.minecraft.network.chat.Component::getString).toList();String selected=selectedFilter(tooltip);if(!selected.isBlank()){boolean filtered=!selected.equalsIgnoreCase("all");return new FilterView(filtered,filtered?selected.toLowerCase(java.util.Locale.ROOT):"",selected,true);}}return FilterView.UNKNOWN;}
    static String selectedFilter(List<String> tooltip){if(tooltip==null)return "";for(String raw:tooltip){String line=raw==null?"":raw.trim();int marker=line.indexOf('▶');if(marker<0&&line.startsWith(">"))marker=0;if(marker>=0){String value=line.substring(marker+1).trim();if(!value.isBlank())return value;}}return "";}
    public static DebugSnapshot debugSnapshot(){String title=cachedScreen==null?"none":cachedScreen.getTitle().getString();return new DebugSnapshot(title,AttributeSessionData.visitedPages(),AttributeSessionData.totalPages(),AttributeSessionData.progress().size(),rows.size(),dirty,List.copyOf(diagnosticEntries));}
    private static ShardSortMode sortMode(){return ShardSortMode.parse(ConfigManager.get().hunting.attributeSort);}
    private static void sortRows(){
        boolean descending=ConfigManager.get().hunting.attributeSortDescending;Comparator<Row> value=switch(sortMode()){
            case RARITY->Comparator.comparingInt(row->row.rarity()==null?Integer.MAX_VALUE:row.rarity().ordinal());case QUANTITY->Comparator.comparingLong(Row::needed);case PRICE->Comparator.comparingLong(row->{Long price=totalPrice(row);return price==null?0:price;});};
        if(descending)value=value.reversed();Comparator<Row> reliable=switch(sortMode()){case RARITY->Comparator.comparing((Row row)->row.rarity()==null);case QUANTITY->Comparator.comparing((Row row)->!row.quantityKnown());case PRICE->Comparator.comparing((Row row)->totalPrice(row)==null);};
        ArrayList<Row> sorted=new ArrayList<>(rows);sorted.sort(reliable.thenComparing(value).thenComparing(row->row.name().getString(),String.CASE_INSENSITIVE_ORDER));rows=List.copyOf(sorted);
    }
    private static void refreshPrices(long revision){ArrayList<Row> priced=new ArrayList<>(rows.size());for(Row row:rows)priced.add(new Row(row.stack(),row.name(),row.rarity(),row.subtype(),row.needed(),row.quantityKnown(),marketUnitPrice(row.subtype())));rows=List.copyOf(priced);cachedPriceRevision=revision;sortRows();}
    private static Long marketUnitPrice(String subtype){var identity=HuntingShardPriceIdentity.resolveAttribute(subtype);var quote=identity==null?null:ShardPriceService.quote(identity.bazaarName());return quote==null?null:quote.instantBuy();}
    private static Long totalPrice(Row row){return row.unitPrice()==null||!row.quantityKnown()?null:safeMultiply(row.unitPrice(),row.needed());}
    static String bazaarQuery(String shardName){String value=shardName==null?"":shardName.trim();return value.replaceFirst("(?i)\\s+shard$","").trim();}
    private static long safeMultiply(long left,long right){try{return Math.multiplyExact(left,right);}catch(ArithmeticException ignored){return Long.MAX_VALUE;}}
    private static boolean validAttributeSlot(int slot){return slot>=9&&slot<=44&&slot%9!=0&&slot%9!=8;}
    private record Row(ItemStack stack,net.minecraft.network.chat.Component name,SkyblockRarity rarity,String subtype,long needed,boolean quantityKnown,Long unitPrice){}
    private record Scan(AttributeMenuDetector.Page page,List<AttributeMenuParser.Parsed> rows,FilterView filter,int candidates,Map<AttributeMenuParser.Ownership,Integer> states,List<String> navigation,List<String> details){}
    private record FilterView(boolean filtered,String key,String label,boolean confirmed){private static final FilterView UNKNOWN=new FilterView(false,"","",false);}
    public record DebugSnapshot(String title,int visitedPages,int totalPages,int discovered,int rows,boolean awaitingStableContent,List<String> entries){}
    private record Bounds(int x,int y,int width,int height,int viewportTop,int viewportBottom,int visibleRows,int maxScroll){boolean contains(double px,double py){return px>=x&&px<x+width&&py>=y&&py<y+height;}boolean inViewport(double px,double py){return px>=x&&px<x+width&&py>=viewportTop&&py<viewportBottom;}}
}
