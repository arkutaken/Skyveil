package name.skyveil.client.hunting;

import name.skyveil.client.itemrarity.ItemRarityDetector;
import name.skyveil.client.itemrarity.SkyblockRarity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Locale;

/** Parses only structured Attribute Shard stacks and their client-visible progression lore. */
public final class AttributeMenuParser {
    private static final Pattern TIER=Pattern.compile("(?:^|\\s)([IVX]{1,5})$");
    private static final Pattern TO_NEXT=Pattern.compile("(?i)\\bsyphon\\s+([\\d,]+)\\s+shards?\\s+to\\s+(level up|unlock)!");
    private static final Pattern SOURCE=Pattern.compile("(?i)^\\s*source:\\s*(.+?)\\s*$");
    private static final Pattern SOURCE_UID=Pattern.compile("(?i)\\s*\\([A-Z]\\d+\\)\\s*$");
    private AttributeMenuParser(){}
    public static Parsed parse(ItemStack original,long ownedBaseShards){
        var identity=AttributeShardResolver.resolve(original);if(identity==null||"REPTILOID".equals(identity.structuredSubtype()))return null;
        ItemStack stack=original.copy();Component name=stack.getHoverName().copy();java.util.List<String> loreText=new java.util.ArrayList<>();
        var lore=stack.get(DataComponents.LORE);if(lore!=null)for(Component line:lore.lines())loreText.add(line.getString());
        ProgressText text=parseProgressText(name.getString(),loreText);int tier=text.tier(),toNext=text.toNext();
        SkyblockRarity rarity=ItemRarityDetector.detect(stack);
        if(rarity==null)rarity=AttributeShardResolver.rarity(identity.structuredSubtype());
        AttributeProgression.Result progress=AttributeProgression.calculate(rarity,tier,toNext,ownedBaseShards);
        String key="ATTRIBUTE:"+identity.structuredSubtype();
        Ownership ownership=text.ownership();
        return new Parsed(key,stack,name,rarity,tier,toNext,ownership,progress,sourceName(loreText));
    }

    /** Pure parsing boundary used by sanitized real-menu fixtures. */
    static ProgressText parseProgressText(String displayedName,Iterable<String> lore){
        int tier=romanSuffix(displayedName),toNext=-1;boolean unlock=false;
        if(lore!=null)for(String line:lore){Matcher matcher=TO_NEXT.matcher(line==null?"":line);if(matcher.find()){
            unlock="unlock".equalsIgnoreCase(matcher.group(2));try{toNext=Integer.parseInt(matcher.group(1).replace(",",""));}catch(NumberFormatException ignored){}
        }}
        return new ProgressText(tier,toNext,classifyOwnership(tier,unlock));
    }
    static String sourceName(Iterable<String> lore){if(lore!=null)for(String line:lore){Matcher matcher=SOURCE.matcher(line==null?"":line);if(matcher.matches())return sanitizeSourceName(matcher.group(1));}return "";}
    static String sanitizeSourceName(String value){return SOURCE_UID.matcher(value==null?"":value.trim()).replaceFirst("").trim();}
    static Parsed withOwned(Parsed row,long ownedBaseShards){
        if(row==null)return null;
        AttributeProgression.Result progress=AttributeProgression.calculate(row.rarity(),row.tier(),row.toNext(),ownedBaseShards);
        return new Parsed(row.key(),row.stack(),row.name(),row.rarity(),row.tier(),row.toNext(),row.ownership(),progress,row.sourceName());
    }
    static Parsed withProgress(Parsed row,int tier,int toNext,Ownership ownership,long ownedBaseShards){
        if(row==null)return null;AttributeProgression.Result progress=AttributeProgression.calculate(row.rarity(),tier,toNext,ownedBaseShards);
        return new Parsed(row.key(),row.stack(),row.name(),row.rarity(),tier,toNext,ownership,progress,row.sourceName());
    }
    static Parsed restore(String subtype,int tier,int toNext,Ownership ownership,String sourceName,long ownedBaseShards){
        if(subtype==null||subtype.isBlank()||ownership==null)return null;
        SkyblockRarity rarity=AttributeShardResolver.rarity(subtype);if(rarity==null)return null;
        String clean=sanitizeSourceName(sourceName);var identity=HuntingShardPriceIdentity.resolveAttribute(subtype);
        if(clean.isBlank()&&identity!=null)clean=identity.displayName()+" Shard";
        ItemStack stack=AttributeShardHeadCatalog.head(subtype);Component name=Component.literal(clean.isBlank()?subtype.replace('_',' '):clean);
        AttributeProgression.Result progress=AttributeProgression.calculate(rarity,tier,toNext,ownedBaseShards);
        return new Parsed("ATTRIBUTE:"+subtype,stack,name,rarity,tier,toNext,ownership,progress,clean);
    }
    private static int romanSuffix(String value){Matcher matcher=TIER.matcher(value==null?"":value.trim().toUpperCase(Locale.ROOT));return matcher.find()?roman(matcher.group(1)):0;}
    private static int roman(String value){int total=0,last=0;for(int index=value.length()-1;index>=0;index--){int current=switch(value.charAt(index)){case 'I'->1;case 'V'->5;case 'X'->10;default->0;};total+=current<last?-current:current;last=Math.max(last,current);}return total;}
    static Ownership classifyOwnership(int tier,boolean unlockPrompt){return tier==AttributeProgression.MAX_TIER?Ownership.MAXED:tier>0&&tier<AttributeProgression.MAX_TIER?Ownership.OWNED:tier==0&&unlockPrompt?Ownership.UNOWNED:Ownership.UNKNOWN;}
    static boolean isMissing(Ownership ownership){return ownership==Ownership.UNOWNED||ownership==Ownership.OWNED;}
    public enum Ownership{UNOWNED,OWNED,MAXED,UNKNOWN}
    record ProgressText(int tier,int toNext,Ownership ownership){}
    public record Parsed(String key,ItemStack stack,Component name,SkyblockRarity rarity,int tier,int toNext,Ownership ownership,AttributeProgression.Result progress,String sourceName){}
}
