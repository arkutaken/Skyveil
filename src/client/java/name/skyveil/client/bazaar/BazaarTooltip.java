package name.skyveil.client.bazaar;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

public final class BazaarTooltip {
    private BazaarTooltip(){}
    /** Handles legacy tags and component wrappers without matching display names. */
    public static CompoundTag attributes(CompoundTag data){
        var found=findAttributes(data,0);
        return found==null?data:found;
    }
    private static CompoundTag findAttributes(CompoundTag data,int depth){
        if(depth>8)return null;
        for(var entry:data.entrySet())if(entry.getKey().equalsIgnoreCase("ExtraAttributes")){
            var compound=entry.getValue().asCompound();
            if(compound.isPresent())return compound.get();
        }
        for(var entry:data.entrySet()){
            var compound=entry.getValue().asCompound();
            if(compound.isPresent()){
                var found=findAttributes(compound.get(),depth+1);
                if(found!=null)return found;
                if(entry.getKey().equals("minecraft:custom_data")&&compound.get().contains("id"))return compound.get();
            }
        }
        return null;
    }
    // A single-enchantment book maps to one Bazaar product. Mixed books cannot
    // be valued as a single product and therefore return an empty key.
    public static String product(CompoundTag data){
        var extra=attributes(data);
        String id=extra.getStringOr("id","");
        if(id.equals("ENCHANTED_BOOK")){
            var enchantments=extra.getCompoundOrEmpty("enchantments");
            if(enchantments.keySet().size()!=1)return "";
            String enchantment=enchantments.keySet().iterator().next();
            return "ENCHANTMENT_"+enchantment.toUpperCase(Locale.ROOT)+"_"+enchantments.getIntOr(enchantment,0);
        }
        return switch(id){
            case "COCOA_BEANS"->"INK_SACK:3";
            case "LAPIS_LAZULI"->"INK_SACK:4";
            default->id;
        };
    }
    public static List<Component> decorate(ItemStack stack,List<Component> lines){
        // Decoration may run repeatedly on a cached tooltip; replace our previous rows.
        lines=name.skyveil.client.auction.AuctionTooltip.removePriceLines(lines,"Insta Buy: ","Insta Sell: ");
        if(stack==null||stack.isEmpty())return lines;
        var custom=stack.get(DataComponents.CUSTOM_DATA);
        if(custom==null)return lines;
        // Copy NBT once per decoration; all identity checks use the same snapshot.
        var data=custom.copyTag();
        var extra=attributes(data);
        // Keep minion pricing limited to replacement ingredients on every tooltip path.
        if(name.skyveil.client.auction.AuctionTooltip.isMinion(extra.getStringOr("id","")))
            return name.skyveil.client.auction.AuctionTooltip.craftCostOnly(lines);
        if(name.skyveil.client.craftcost.CraftCostTooltip.isInherentlySoulbound(extra.getStringOr("id","")))
            return name.skyveil.client.auction.AuctionTooltip.removePriceLines(lines,"Insta Buy: ","Insta Sell: ");
        String id=product(data);
        var shard=name.skyveil.client.hunting.HuntingShardPriceIdentity.resolve(stack);
        if(shard!=null)id=shard.bazaarName();
        if(id.isBlank())return lines;
        var quote=BazaarPrices.quote(id);
        if(quote==null)return lines;
        var result=new ArrayList<>(lines);
        if(result.isEmpty()||!result.getLast().getString().isBlank())result.add(Component.empty());
        result.add(priceLine("Insta Buy",quote.buy(),stack.getCount(),0xFFAA00));
        result.add(priceLine("Insta Sell",quote.sell(),stack.getCount(),0x55FF55));
        return result;
    }
    public static Component priceLine(String label,Double price,int count,int color){
        var line=Component.empty()
            .append(Component.literal(label+": ").withColor(color))
            .append(Component.literal(format(price)).withColor(0xFFFFFF));
        if(count>1)line
            .append(Component.literal(" Total: ").withColor(color))
            .append(Component.literal(format(price==null?null:price*count)).withColor(0xFFFFFF));
        return line;
    }
    private static String format(Double price){
        if(price==null)return "Unavailable";
        var format=new java.text.DecimalFormat("#,##0.#",java.text.DecimalFormatSymbols.getInstance(Locale.ROOT));
        return format.format(price);
    }
}