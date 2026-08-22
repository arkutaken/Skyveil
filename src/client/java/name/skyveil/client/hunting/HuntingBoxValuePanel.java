package name.skyveil.client.hunting;

import name.skyveil.client.config.ConfigManager;
import name.skyveil.client.gui.SkyveilTheme;
import name.skyveil.client.itemrarity.SkyblockRarity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Paged Hunting Box holdings panel valued from Skyveil's shard-only Bazaar snapshot. */
public final class HuntingBoxValuePanel {
    private static final Pattern OWNED=Pattern.compile("^Owned:\\s*([\\d,]+)\\s+Shards?$",Pattern.CASE_INSENSITIVE);
    private static final int SPACING=5,PREFERRED_WIDTH=244,MIN_WIDTH=150,HEADER=50,ROW_HEIGHT=20,FOOTER=15;
    private static final int PRICE_TEXT=0xFFFFE8B0;
    private static AbstractContainerScreen<?> cachedScreen;private static boolean dirty=true;
    private static int pendingFingerprint=Integer.MIN_VALUE,pendingConfirmations,scroll,totalPages=1;
    private static final Map<String,Entry> ENTRIES=new HashMap<>();private static final Map<Integer,Set<String>> PAGE_KEYS=new HashMap<>();private static final Set<Integer> VISITED=new HashSet<>();
    private static Bounds bounds;
    private HuntingBoxValuePanel(){}

    public static void render(AbstractContainerScreen<?> screen,GuiGraphicsExtractor graphics,int guiLeft,int guiTop,int guiWidth,int guiHeight,int mouseX,int mouseY){
        if(!ConfigManager.get().hunting.huntingBoxValue||!HuntingBoxMenuDetector.matches(screen)){clearUnless(screen);return;}
        int leftSpace=guiLeft-SPACING-2,rightSpace=graphics.guiWidth()-(guiLeft+guiWidth)-SPACING-2,available=Math.max(leftSpace,rightSpace);if(available<MIN_WIDTH){bounds=null;return;}
        int width=Math.min(PREFERRED_WIDTH,available),x=leftSpace>=rightSpace?guiLeft-SPACING-width:guiLeft+guiWidth+SPACING,y=guiTop,height=guiHeight;
        if(screen!=cachedScreen)beginScreen(screen);if(dirty)stabilize(screen);
        List<Row> rows=rows();int viewportTop=y+HEADER,viewportBottom=y+height-FOOTER,visible=Math.max(1,(viewportBottom-viewportTop)/ROW_HEIGHT),maxScroll=Math.max(0,rows.size()-visible);
        scroll=Math.max(0,Math.min(scroll,maxScroll));bounds=new Bounds(x,y,width,height,viewportTop,viewportBottom,maxScroll);
        int background=switch(ConfigManager.get().darkMode){case "DARK_PURPLE"->0xEE170D24;case "DARK"->0xEE15171C;default->0xE8211A2B;};
        graphics.fill(x,y,x+width,y+height,background);graphics.outline(x,y,width,height,SkyveilTheme.ACCENT);
        graphics.text(Minecraft.getInstance().font,"Hunting Box Value",x+6,y+6,SkyveilTheme.TEXT,true);
        long total=0;int priced=0,unpriced=0;for(Row row:rows)if(row.unitPrice()==null)unpriced++;else{priced++;total=safeAdd(total,safeMultiply(row.unitPrice(),row.amount()));}ShardPriceService.Status priceStatus=ShardPriceService.status();
        String incomplete=VISITED.size()<totalPages?"+":"";
        graphics.text(Minecraft.getInstance().font,"Sell value: "+ShardPriceService.format(total)+incomplete,x+6,y+18,0xFFFFD45C,false);
        String missingState=priceStatus==ShardPriceService.Status.LOADING?" loading":priceStatus==ShardPriceService.Status.UNAVAILABLE?" unavailable":" no data";
        graphics.text(Minecraft.getInstance().font,"Pages "+VISITED.size()+"/"+totalPages+" • "+priced+" priced"+(unpriced>0?" • "+unpriced+missingState:""),x+6,y+30,unpriced==0?0xFF55FF55:0xFFFFAA55,false);
        graphics.enableScissor(x+1,viewportTop,x+width-1,viewportBottom);
        if(rows.isEmpty())graphics.text(Minecraft.getInstance().font,"No owned shards observed",x+6,viewportTop+5,SkyveilTheme.SECONDARY,false);
        for(int index=0;index<visible&&scroll+index<rows.size();index++){
            Row row=rows.get(scroll+index);int rowY=viewportTop+index*ROW_HEIGHT;graphics.fill(x+1,rowY,x+width-1,rowY+ROW_HEIGHT,(index&1)==0?0x302E243C:0x30372A49);graphics.item(row.stack(),x+3,rowY+2);
            String unavailable=priceStatus==ShardPriceService.Status.LOADING?"loading…":"no data";
            String values=row.unitPrice()==null?unavailable:priceValues(row.unitPrice(),row.amount());
            Component label=row.stack().getHoverName().copy().append(Component.literal(" x"+row.amount()).withStyle(ChatFormatting.GRAY));
            int valuesWidth=Minecraft.getInstance().font.width(values),nameWidth=Math.max(30,width-32-valuesWidth);var nameLine=Minecraft.getInstance().font.split(label,nameWidth);
            if(!nameLine.isEmpty())graphics.text(Minecraft.getInstance().font,nameLine.getFirst(),x+22,rowY+6,0xFFFFFFFF,true);
            graphics.text(Minecraft.getInstance().font,values,x+width-5-valuesWidth,rowY+6,PRICE_TEXT,false);
            if(mouseX>=x+2&&mouseX<x+width-2&&mouseY>=rowY&&mouseY<rowY+ROW_HEIGHT)graphics.setTooltipForNextFrame(Minecraft.getInstance().font,row.stack(),mouseX,mouseY);
        }
        graphics.disableScissor();graphics.text(Minecraft.getInstance().font,"Hypixel Bazaar • "+priceStatus.name().toLowerCase(Locale.ROOT),x+5,y+height-12,SkyveilTheme.SECONDARY,false);
    }

    public static boolean mouseScrolled(AbstractContainerScreen<?> screen,double mouseX,double mouseY,double vertical){Bounds area=bounds;if(screen!=cachedScreen||area==null||area.maxScroll()==0||!area.inViewport(mouseX,mouseY))return false;if(vertical>0)scroll=Math.max(0,scroll-1);else if(vertical<0)scroll=Math.min(area.maxScroll(),scroll+1);return vertical!=0;}
    public static boolean mouseClicked(AbstractContainerScreen<?> screen,double mouseX,double mouseY){return screen==cachedScreen&&bounds!=null&&bounds.contains(mouseX,mouseY);}
    public static void onContainerUpdate(int containerId){if(cachedScreen!=null&&cachedScreen.getMenu().containerId==containerId)dirty=true;}
    public static void reset(){cachedScreen=null;dirty=true;pendingFingerprint=Integer.MIN_VALUE;pendingConfirmations=0;scroll=0;totalPages=1;ENTRIES.clear();PAGE_KEYS.clear();VISITED.clear();bounds=null;}
    private static void clearUnless(AbstractContainerScreen<?> screen){if(screen==cachedScreen)reset();}
    private static void beginScreen(AbstractContainerScreen<?> screen){reset();cachedScreen=screen;}
    private static void stabilize(AbstractContainerScreen<?> screen){int fingerprint=fingerprint(screen);if(fingerprint!=pendingFingerprint){pendingFingerprint=fingerprint;pendingConfirmations=1;return;}if(++pendingConfirmations<2)return;scan(screen);dirty=false;}
    private static int fingerprint(AbstractContainerScreen<?> screen){Minecraft client=Minecraft.getInstance();int result=screen.getMenu().containerId;for(var slot:screen.getMenu().slots){if(client.player!=null&&slot.container==client.player.getInventory())continue;result=31*result+slot.index;result=31*result+ItemStack.hashItemAndComponents(slot.getItem());}return result;}
    private static void scan(AbstractContainerScreen<?> screen){
        Minecraft client=Minecraft.getInstance();HuntingBoxMenuDetector.Page page=HuntingBoxMenuDetector.page(screen);totalPages=Math.max(totalPages,page.total());HashSet<String> currentKeys=new HashSet<>();Map<String,Long> attributeQuantities=new HashMap<>();
        for(var slot:screen.getMenu().slots){if(client.player!=null&&slot.container==client.player.getInventory())continue;ItemStack stack=slot.getItem();if(stack.isEmpty())continue;Long amount=owned(stack);if(amount==null)continue;
            String key=stableKey(stack);currentKeys.add(key);ItemStack copy=stack.copy();ENTRIES.put(key,new Entry(copy,HuntingShardPriceIdentity.resolve(stack),amount));
            AttributeShardResolver.Identity identity=AttributeShardResolver.resolve(stack);if(identity!=null)attributeQuantities.put("ATTRIBUTE:"+identity.structuredSubtype(),amount);
        }
        Set<String> previous=PAGE_KEYS.put(page.current(),Set.copyOf(currentKeys));if(previous!=null)for(String key:previous)if(!currentKeys.contains(key))ENTRIES.remove(key);VISITED.add(page.current());AttributeSessionData.observeOwnedPage(page.current(),attributeQuantities);ShardPriceService.ensureFresh();
    }
    private static List<Row> rows(){ArrayList<Row> rows=new ArrayList<>();for(Entry entry:ENTRIES.values())if(entry.amount()>0){var quote=entry.identity()==null?null:ShardPriceService.quote(entry.identity().bazaarName());rows.add(new Row(entry.stack(),entry.amount(),quote==null?null:quote.instantSell()));}rows.sort(Comparator.comparingLong((Row row)->row.unitPrice()==null?Long.MIN_VALUE:safeMultiply(row.unitPrice(),row.amount())).reversed().thenComparing(row->row.stack().getHoverName().getString(),String.CASE_INSENSITIVE_ORDER));return List.copyOf(rows);}
    private static Long owned(ItemStack stack){var lore=stack.get(DataComponents.LORE);if(lore==null)return null;for(var line:lore.lines()){Matcher matcher=OWNED.matcher(line.getString().trim());if(matcher.matches())try{return Long.parseLong(matcher.group(1).replace(",",""));}catch(NumberFormatException ignored){return null;}}return null;}
    private static String stableKey(ItemStack stack){var market=HuntingShardPriceIdentity.resolve(stack);if(market!=null)return market.shardId();var identity=AttributeShardResolver.resolve(stack);if(identity!=null)return identity.structuredSubtype();return stack.getHoverName().getString().replaceAll("(?:\\u00c2)?\\u00a7.","").replaceAll("\\s+"," ").trim().toUpperCase(Locale.ROOT);}
    private static long safeMultiply(long left,long right){try{return Math.multiplyExact(left,right);}catch(ArithmeticException ignored){return Long.MAX_VALUE;}}
    private static long safeAdd(long left,long right){try{return Math.addExact(left,right);}catch(ArithmeticException ignored){return Long.MAX_VALUE;}}
    private static String priceValues(long unitPrice,long amount){long total=safeMultiply(unitPrice,amount);return ShardPriceService.format(unitPrice)+" ea • "+ShardPriceService.format(total)+" total";}
    private record Entry(ItemStack stack,HuntingShardPriceIdentity.MarketIdentity identity,long amount){}private record Row(ItemStack stack,long amount,Long unitPrice){}
    private record Bounds(int x,int y,int width,int height,int viewportTop,int viewportBottom,int maxScroll){boolean contains(double px,double py){return px>=x&&px<x+width&&py>=y&&py<y+height;}boolean inViewport(double px,double py){return px>=x&&px<x+width&&py>=viewportTop&&py<viewportBottom;}}
}
