package name.skyveil.client.craftcost;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import name.skyveil.client.auction.AuctionPrices;
import name.skyveil.client.bazaar.BazaarPrices;
import name.skyveil.client.bazaar.BazaarTooltip;
import name.skyveil.client.config.ConfigManager;
import name.skyveil.client.SkyblockSession;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Retains only the last hovered item's calculation for one second. No on-disk or historical cache. */
public final class CraftCostTooltip {
    private static JsonObject catalog;
    private static final class Visibility {static final CraftCostVisibility RULES=new CraftCostVisibility(catalog());}
    private static CompoundTag lastItem;
    private static long calculatedAt,lastAuctionRevision,lastBazaarRevision;
    private static CraftCostCalculator.Result lastResult;
    private CraftCostTooltip(){}
    public static void register(){
        net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack,context,flag,lines)->{
            if(!SkyblockSession.isActive()||!ConfigManager.get().fullCraftCost)return;
            var decorated=decorate(stack,lines);
            if(decorated!=lines){lines.clear();lines.addAll(decorated);}
        });
    }
    /** Also called at final screen tooltip construction to refresh tooltips cached by other consumers. */
    public static java.util.List<Component> decorate(net.minecraft.world.item.ItemStack stack,java.util.List<Component> lines){
        if(stack.isEmpty())return lines;
        var custom=stack.get(DataComponents.CUSTOM_DATA);if(custom==null)return lines;
        var extra=BazaarTooltip.attributes(custom.copyTag());
        if(extra.getStringOr("id","").isBlank())return lines;
        // Also clean reused tooltips when the auction-price option is disabled.
        if(name.skyveil.client.auction.AuctionTooltip.isMinion(extra.getStringOr("id","")))
            lines=name.skyveil.client.auction.AuctionTooltip.craftCostOnly(lines);
        if(isInherentlySoulbound(extra.getStringOr("id",""))||!Visibility.RULES.shouldShow(extra))
            return name.skyveil.client.auction.AuctionTooltip.removePriceLines(lines,"Full craft cost: ");
        long now=System.currentTimeMillis();
        // A market replacement invalidates the hovered result immediately, even
        // inside its normal one-second reuse window.
        long auctionRevision=AuctionPrices.revision(),bazaarRevision=BazaarPrices.revision();
        if(!extra.equals(lastItem)||now-calculatedAt>=1000||auctionRevision!=lastAuctionRevision||bazaarRevision!=lastBazaarRevision){
            try{lastResult=new CraftCostCalculator(catalog(),CraftCostTooltip::price).calculate(extra);}
            catch(RuntimeException invalid){lastResult=new CraftCostCalculator.Result(null,java.util.Set.of("Unsupported item data"));}
            lastItem=extra.copy();calculatedAt=now;lastAuctionRevision=auctionRevision;lastBazaarRevision=bazaarRevision;
        }
        Component priceLine=lastResult.coins()==null&&AuctionPrices.isLoading()
            ?Component.empty().append(Component.literal("Full craft cost: ").withColor(0xFFAA00))
                .append(Component.literal("Loading...").withColor(0xFFFFFF))
            :BazaarTooltip.priceLine("Full craft cost",lastResult.coins(),stack.getCount(),0xFFAA00);
        if(lastResult.coins()==null&&!AuctionPrices.isLoading()&&!lastResult.missing().isEmpty())
            priceLine=priceLine.copy().append(Component.literal(missingSummary(lastResult.missing().size())).withColor(0xAAAAAA));
        if(lastResult.coins()!=null&&!lastResult.materials().isEmpty()){
            var parts=new java.util.ArrayList<String>();
            lastResult.materials().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).forEach(entry->{
                String name=entry.getKey().equals("KUUDRA_TEETH")?"Kuudra Teeth":"Heavy Pearls";
                parts.add(((long)entry.getValue()*stack.getCount())+" "+name);
            });
            priceLine=priceLine.copy().append(Component.literal(" + "+String.join(", ",parts)).withColor(0xAAAAAA));
        }
        return name.skyveil.client.auction.AuctionTooltip.replacePriceLine(lines,"Full craft cost: ",priceLine);
    }
    static String missingSummary(int count){return " ("+count+" unpriced input"+(count==1?"":"s")+")";}
    public static String diagnostic(net.minecraft.world.item.ItemStack stack){
        var custom=stack.get(DataComponents.CUSTOM_DATA);
        if(custom==null)return "Craft cost: no item data";
        var result=new CraftCostCalculator(catalog(),CraftCostTooltip::price).calculate(BazaarTooltip.attributes(custom.copyTag()));
        return "Craft cost unpriced inputs: "+String.join(", ",new java.util.TreeSet<>(result.missing()));
    }
    /** Table levels cost XP, not coins; books retain their own market identity. */
    public static CompoundTag paidEnchantments(CompoundTag extra){
        var enchants=extra.getCompoundOrEmpty("enchantments").copy();
        if(extra.getStringOr("id","").equals("ENCHANTED_BOOK"))return enchants;
        var table=catalog().getAsJsonObject("tableEnchants");
        for(String key:new java.util.ArrayList<>(enchants.keySet())){
            int level=enchants.getIntOr(key,0);
            var maximum=table.get(key.toUpperCase(java.util.Locale.ROOT));
            if(level<=0||(maximum!=null&&level<=maximum.getAsInt()))enchants.remove(key);
        }
        return enchants;
    }
    public static void reset(){lastItem=null;lastResult=null;calculatedAt=0;}
    static Double price(String id){
        id=bazaarKey(id);
        var quote=BazaarPrices.quote(id);
        if(BazaarPrices.isProduct(id))return quote==null?null:quote.buy();
        return AuctionPrices.unmodifiedQuote(auctionKey(id));
    }
    static String bazaarKey(String id){
        return switch(id){case "COCOA_BEANS"->"INK_SACK:3";case "LAPIS_LAZULI"->"INK_SACK:4";default->id;};
    }
    static String auctionKey(String id){
        if(id.startsWith("ENCHANTMENT_")){
            int split=id.lastIndexOf('_');
            if(split>12)id="BOOK:"+id.substring(12,split)+"="+id.substring(split+1);
        }
        return id;
    }
    /** Item-definition binding only; Museum donations and instance flags do not change this. */
    public static boolean isInherentlySoulbound(String id){
        var values=catalog().getAsJsonObject("soulboundItems");
        if(values==null)return false;
        var value=values.get(id.trim().toUpperCase(java.util.Locale.ROOT));
        return value!=null&&(value.getAsString().equals("COOP")||value.getAsString().equals("SOLO"));
    }
    static JsonObject catalog(){
        if(catalog!=null)return catalog;
        try(var stream=CraftCostTooltip.class.getResourceAsStream("/assets/skyveil/data/craft_cost_catalog.json")){
            if(stream==null)throw new IllegalStateException("Craft cost catalog missing");
            catalog=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
            return catalog;
        }catch(java.io.IOException error){throw new IllegalStateException(error);}
    }
}