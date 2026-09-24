package name.skyveil.client.dungeon;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Adds server-authored dungeon drop floor and base-stat quality to item tooltips. */
public final class DungeonItemTooltip {
    private static final Pattern RARITY_FOOTER=Pattern.compile("(?:^| )(?:VERY SPECIAL|COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|LEGENJERRY|MYTHIC|DIVINE|SPECIAL|SUPREME|ULTIMATE|ADMIN)(?: |$)");
    private DungeonItemTooltip() {}

    public static List<Component> decorate(ItemStack stack,List<Component> original) {
        Info info=inspect(stack);
        return info==null?original:decorate(original,info);
    }

    static List<Component> decorate(List<Component> original,Info info) {
        if(original==null||original.isEmpty()||info==null)return original;
        Component line=Component.literal("Floor: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(info.floor()).withStyle(ChatFormatting.AQUA))
            .append(Component.literal("  •  Quality: ").withStyle(ChatFormatting.DARK_GRAY))
            .append(Component.literal(info.quality()+"/50").withStyle(qualityColor(info.quality())));
        ArrayList<Component> result=new ArrayList<>(original);
        int insertion=result.size();
        for(int index=result.size()-1;index>=0;index--){if(isRarityFooter(result.get(index))){insertion=index;break;}}
        result.add(insertion,line);
        return List.copyOf(result);
    }

    static Info inspect(ItemStack stack) {
        if(stack==null||stack.isEmpty())return null;
        var custom=stack.get(DataComponents.CUSTOM_DATA);
        if(custom==null||custom.isEmpty())return null;
        CompoundTag root=custom.copyTag(),attributes=findExtraAttributes(root,0);
        return inspectAttributes(attributes==null?root:attributes);
    }

    static Info inspectAttributes(CompoundTag attributes) {
        Integer quality=integer(attributes,"baseStatBoostPercentage","base_stat_boost_percentage");
        Integer tier=integer(attributes,"item_tier");
        if(quality==null||quality<1||quality>50||tier==null)return null;
        String floor=floor(tier,string(attributes,"dungeon_skill_req"));
        return floor==null?null:new Info(floor,quality);
    }

    static String floor(int tier,String requirement) {
        if(tier==0)return "E";
        if(tier<1||tier>10)return null;
        String[] parts=(requirement==null?"":requirement.trim()).split(":",2);
        if(parts.length==2&&parts[0].equalsIgnoreCase("CATACOMBS")) {
            try{if(Integer.parseInt(parts[1].trim())-tier>19&&tier>=4)return "M"+(tier-3);}catch(NumberFormatException ignored){}
        }
        if(tier>=8)return "M"+(tier-3);
        return "F"+tier;
    }

    private static ChatFormatting qualityColor(int quality) {
        if(quality<=17)return ChatFormatting.RED;
        if(quality<=33)return ChatFormatting.YELLOW;
        if(quality<=49)return ChatFormatting.GREEN;
        return ChatFormatting.AQUA;
    }

    private static boolean isRarityFooter(Component line) {
        String normalized=line.getString().toUpperCase(Locale.ROOT).replace('_',' ')
            .replaceAll("[^A-Z ]+"," ").replaceAll("\\s+"," ").trim();
        return RARITY_FOOTER.matcher(normalized).find();
    }

    private static CompoundTag findExtraAttributes(CompoundTag tag,int depth) {
        if(tag==null||depth>7)return null;
        for(var entry:tag.entrySet())if(entry.getKey().equalsIgnoreCase("ExtraAttributes")){
            var compound=entry.getValue().asCompound();if(compound.isPresent())return compound.get();
        }
        for(Tag value:tag.values()){
            var compound=value.asCompound();if(compound.isPresent()){CompoundTag found=findExtraAttributes(compound.get(),depth+1);if(found!=null)return found;}
            var list=value.asList();if(list.isPresent())for(Tag child:list.get()){
                var nested=child.asCompound();if(nested.isPresent()){CompoundTag found=findExtraAttributes(nested.get(),depth+1);if(found!=null)return found;}
            }
        }
        return null;
    }

    private static Integer integer(CompoundTag tag,String... wanted) {
        if(tag==null)return null;
        for(var entry:tag.entrySet())for(String key:wanted)if(entry.getKey().equalsIgnoreCase(key)){
            var value=entry.getValue().asInt();if(value.isPresent())return value.get();
        }
        return null;
    }

    private static String string(CompoundTag tag,String wanted) {
        if(tag==null)return "";
        for(String key:tag.keySet())if(key.equalsIgnoreCase(wanted))return tag.getStringOr(key,"");
        return "";
    }

    public record Info(String floor,int quality) {}
}
