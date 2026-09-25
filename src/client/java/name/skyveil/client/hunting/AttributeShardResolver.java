package name.skyveil.client.hunting;

import name.skyveil.client.itemrarity.SkyblockRarity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resolves only the stable Attribute Shard identity needed by the retained progress panel. */
public final class AttributeShardResolver {
    private static final Logger LOGGER=LoggerFactory.getLogger("skyveil-attribute-shards");
    private static final Pattern CONCRETE_ID=Pattern.compile("^ATTRIBUTE_SHARD_(.+);(\\d+)$");
    private static final Pattern DISPLAY_TIER=Pattern.compile("\\s+[IVXL]+$",Pattern.CASE_INSENSITIVE);
    private static final Map<String,String> RARITIES=loadRarities();
    private static final Map<String,String> ABILITY_NAMES=loadAbilityNames();
    private static final Map<String,String> DISPLAY_TO_SUBTYPE=displayLookup();
    private AttributeShardResolver(){}

    // Prefer structured IDs. Only known catalog names are accepted by the display
    // fallback; arbitrary named items must not become attribute identities.
    public static Identity resolve(ItemStack stack){
        if(stack==null||stack.isEmpty())return null;
        String internalId=resolveInternalId(stack);
        Matcher concrete=CONCRETE_ID.matcher(internalId);
        if(concrete.matches()){
            int level=parsePositiveInt(concrete.group(2));
            String subtype=normalizeId(concrete.group(1));
            return level>0&&RARITIES.containsKey(subtype)?new Identity(subtype,level):null;
        }
        if(!"ATTRIBUTE_SHARD".equals(internalId))return resolveDisplayedName(stack.getHoverName().getString());
        CompoundTag attributes=extraAttributes(stack);
        if(attributes==null)return resolveDisplayedName(stack.getHoverName().getString());
        Map<String,Integer> values=numericCompound(attributes,"attributes");
        if(values.size()!=1)return resolveDisplayedName(stack.getHoverName().getString());
        var value=values.entrySet().iterator().next();
        return RARITIES.containsKey(value.getKey())?new Identity(value.getKey(),value.getValue()):resolveDisplayedName(stack.getHoverName().getString());
    }

    static Identity resolveDisplayedName(String displayedName){
        String withoutTier=DISPLAY_TIER.matcher(displayedName==null?"":displayedName.trim()).replaceFirst("");
        String subtype=DISPLAY_TO_SUBTYPE.get(normalizeDisplay(withoutTier));return subtype==null?null:new Identity(subtype,1);
    }

    public static String abilityName(String subtype){return ABILITY_NAMES.get(normalizeId(subtype));}

    public static String resolveInternalId(ItemStack stack){
        CompoundTag attributes=extraAttributes(stack);
        if(attributes==null)return "";
        return normalizeId(string(attributes,"id"));
    }

    public static SkyblockRarity rarity(String subtype){return SkyblockRarity.fromLabel(RARITIES.get(normalizeId(subtype)));}
    public static int catalogSize(){return RARITIES.size();}

    private static CompoundTag extraAttributes(ItemStack stack){
        if(stack==null||stack.isEmpty())return null;
        var custom=stack.get(DataComponents.CUSTOM_DATA);
        if(custom==null||custom.isEmpty())return null;
        CompoundTag root=custom.copyTag(),found=findExtraAttributes(root,0);
        return found==null?root:found;
    }

    private static CompoundTag findExtraAttributes(CompoundTag tag,int depth){
        if(depth>7)return null;
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

    private static Map<String,Integer> numericCompound(CompoundTag parent,String key){
        Tag value=tag(parent,key);if(value==null)return Map.of();
        var compound=value.asCompound();if(compound.isEmpty())return Map.of();
        TreeMap<String,Integer> result=new TreeMap<>();
        for(var entry:compound.get().entrySet())entry.getValue().asInt().ifPresent(level->{if(level>0)result.put(normalizeId(entry.getKey()),level);});
        return result;
    }

    private static Tag tag(CompoundTag parent,String wanted){for(var entry:parent.entrySet())if(entry.getKey().equalsIgnoreCase(wanted))return entry.getValue();return null;}
    private static String string(CompoundTag tag,String wanted){for(String key:tag.keySet())if(key.equalsIgnoreCase(wanted))return tag.getStringOr(key,"");return "";}
    private static String normalizeId(String value){return value==null?"":value.trim().toUpperCase(Locale.ROOT).replace(' ','_');}
    private static int parsePositiveInt(String value){try{return Math.max(0,Integer.parseInt(value));}catch(NumberFormatException ignored){return 0;}}
    private static String normalizeDisplay(String value){return value==null?"":value.replaceAll("(?:\\u00c2)?\\u00a7.","").trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+","_").replaceAll("^_+|_+$","");}

    private static Map<String,String> loadRarities(){
        Properties properties=new Properties();
        try(var stream=AttributeShardResolver.class.getResourceAsStream("/assets/skyveil/data/attribute_shard_rarity.properties")){
            if(stream==null)throw new IllegalStateException("resource is missing");
            properties.load(new InputStreamReader(stream,StandardCharsets.UTF_8));
            TreeMap<String,String> result=new TreeMap<>();
            for(String key:properties.stringPropertyNames()){
                String rarity=normalizeId(properties.getProperty(key));
                if(SkyblockRarity.fromLabel(rarity)!=null)result.put(normalizeId(key),rarity);
            }
            result.remove("REPTILOID");
            return Map.copyOf(result);
        }catch(Exception exception){LOGGER.error("Could not load Attribute Shard rarity catalog; progress parsing is disabled",exception);return Map.of();}
    }

    private static Map<String,String> loadAbilityNames(){
        Properties properties=new Properties();
        try(var stream=AttributeShardResolver.class.getResourceAsStream("/assets/skyveil/data/attribute_ability_names.properties")){
            if(stream==null)throw new IllegalStateException("resource is missing");properties.load(new InputStreamReader(stream,StandardCharsets.UTF_8));TreeMap<String,String> result=new TreeMap<>();
            for(String key:properties.stringPropertyNames()){String subtype=normalizeId(key),name=properties.getProperty(key,"").trim();if(RARITIES.containsKey(subtype)&&!name.isEmpty())result.put(subtype,name);}return Map.copyOf(result);
        }catch(Exception exception){LOGGER.error("Could not load Attribute ability display-name catalog",exception);return Map.of();}
    }
    private static Map<String,String> displayLookup(){TreeMap<String,String> result=new TreeMap<>();ABILITY_NAMES.forEach((subtype,name)->result.put(normalizeDisplay(name),subtype));return Map.copyOf(result);}

    public record Identity(String structuredSubtype,int level){}
}
