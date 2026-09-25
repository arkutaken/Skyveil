package name.skyveil.client.auction;

import name.skyveil.client.bazaar.BazaarPrices;
import name.skyveil.client.bazaar.BazaarTooltip;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class AuctionTooltip {
    private AuctionTooltip(){}
    public static void register(){
        net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack,context,flag,lines)->{
            if(!name.skyveil.client.SkyblockSession.isActive()||!name.skyveil.client.config.ConfigManager.get().auctionTooltip)return;
            var decorated=decorate(stack,lines);
            if(decorated!=lines){lines.clear();lines.addAll(decorated);}
        });
    }
    /** Replace an earlier/cached value instead of appending duplicate rows from multiple tooltip paths. */
    public static List<Component> replacePriceLine(List<Component> lines,String prefix,Component value){
        var result=new ArrayList<Component>();boolean replaced=false;
        for(var line:lines){
            if(line.getString().startsWith(prefix)){
                if(!replaced){result.add(value);replaced=true;}
            }else result.add(line);
        }
        if(!replaced){result.add(Component.empty());result.add(value);}
        return result;
    }
    public static List<Component> removePriceLines(List<Component> lines,String... prefixes){
        var result=new ArrayList<>(lines);
        // Remove each price row's separator too; repeated refreshes must not grow the tooltip.
        for(int index=result.size()-1;index>=0;index--){
            String text=result.get(index).getString();
            if(Arrays.stream(prefixes).noneMatch(text::startsWith))continue;
            result.remove(index);
            if(index>0&&result.get(index-1).getString().isBlank())result.remove(--index);
        }
        return result.size()==lines.size()?lines:result;
    }
    /** Minion tiers are identified by their server ID, never by an editable name. */
    public static boolean isMinion(String id){
        return id!=null&&id.trim().toUpperCase(Locale.ROOT).matches("[A-Z0-9_]+_GENERATOR_[1-9][0-9]*");
    }
    /** Remove cached market rows too, while preserving the separate craft-cost row. */
    public static List<Component> craftCostOnly(List<Component> lines){
        return removePriceLines(lines,"Lowest BIN: ","Active BIN average: ","3-day average: ","Insta Buy: ","Insta Sell: ");
    }
    static boolean soulbound(List<Component> lines){
        return lines.stream().map(line->line.getString().replaceAll("\u00a7.","").trim())
            .anyMatch(text->text.matches("(?i)[*\\u2726\\u25c6\\u2666 ]*(?:Co-op )?Soulbound[*\\u2726\\u25c6\\u2666 ]*"));
    }
    public static String diagnostic(ItemStack stack,List<Component> lines){
        if(stack.isEmpty())return "Hold an item with a missing price in your main hand.";
        var custom=stack.get(DataComponents.CUSTOM_DATA);
        if(custom==null)return "Held item has no SkyBlock custom data.";
        var data=custom.copyTag();var extra=BazaarTooltip.attributes(data);
        String key=AuctionPrices.identity(extra),product=BazaarTooltip.product(data);
        Double price=AuctionPrices.current().prices().get(key);
        return "item="+extra.getStringOr("id","<missing>")+", key="+key+", soulbound="+soulbound(lines)
            +", bazaar="+BazaarPrices.isProduct(product)+", quote="+price
            +", tooltip line="+lines.stream().anyMatch(line->line.getString().startsWith("Lowest BIN: "));
    }
    static Component historyLine(AuctionHistory.Average history,int count){
        if(history==null)return Component.empty()
            .append(Component.literal("3-day average: ").withColor(0xFFAA00))
            .append(Component.literal("No similar items found, cannot calculate").withColor(0xFFFFFF));
        var line=BazaarTooltip.priceLine("3-day average",history.price(),count,0xFFAA00);
        if(!history.complete())line=line.copy().append(Component.literal(" (collecting history)").withColor(0xAAAAAA));
        return line;
    }
    // Tooltip paths can reuse cached rows. Remove obsolete price lines before
    // applying current identity/market rules, especially for inherently bound items.
    public static List<Component> decorate(ItemStack stack,List<Component> lines){
        lines=removePriceLines(lines,"Active BIN average: ");
        if(stack==null||stack.isEmpty())return lines;
        var custom=stack.get(DataComponents.CUSTOM_DATA);if(custom==null)return lines;
        var data=custom.copyTag();var extra=BazaarTooltip.attributes(data);
        // Minions have no comparable auction row; do not start a market request for one.
        if(isMinion(extra.getStringOr("id","")))return craftCostOnly(lines);
        if(name.skyveil.client.craftcost.CraftCostTooltip.isInherentlySoulbound(extra.getStringOr("id","")))
            return removePriceLines(lines,"Lowest BIN: ","Active BIN average: ","3-day average: ");
        String bazaar=BazaarTooltip.product(data);
        name.skyveil.client.hunting.ShardPriceService.ensureFresh();
        if(BazaarPrices.isProduct(bazaar)||bazaar.startsWith("SHARD_"))return lines;
        String key=AuctionPrices.identity(extra);if(key.isBlank())return lines;
        Double quote=AuctionPrices.quote(key);
        var result=removePriceLines(lines,"Lowest BIN: ");
        if(quote!=null)result=replacePriceLine(result,"Lowest BIN: ",BazaarTooltip.priceLine("Lowest BIN",quote,stack.getCount(),0xFFAA00));
        var history=AuctionPrices.threeDayAverage(extra);
        var historyLine=historyLine(history,stack.getCount());
        return replacePriceLine(result,"3-day average: ",historyLine);
    }
}