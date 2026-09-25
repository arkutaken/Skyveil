package name.skyveil.client.itemsearch;

import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;

/** Public bridge between the bundled SkyBlock head catalog and persistent UI icons. */
public final class SkyBlockHeadIcons {
    public static final String PREFIX="skyblock-head:";
    private static volatile Data data;
    private SkyBlockHeadIcons(){}

    public static List<HeadIcon> all(){return data().icons;}
    public static List<HeadIcon> heads(){return data().heads;}
    public static List<HeadIcon> npcs(){return data().npcs;}
    public static boolean isKey(String value){return value!=null&&value.startsWith(PREFIX);}
    public static boolean exists(String key){return isKey(key)&&data().byKey.containsKey(key);}
    public static ItemStack resolve(String key){HeadIcon icon=data().byKey.get(key);return icon==null?ItemStack.EMPTY:icon.stack();}

    // Build the picker index once. Collapse pet/minion tiers to their highest
    // catalog variant and exclude cosmetic-item categories to reduce duplicate icons.
    private static Data data(){Data current=data;if(current!=null)return current;synchronized(SkyBlockHeadIcons.class){if(data!=null)return data;List<ItemSearchCatalog.Entry> entries=ItemSearchCatalog.headEntries().stream().filter(entry->!isItemSkin(entry)&&(isNpcId(entry.internalName())||!isExcludedItemCategory(entry))).toList();HashMap<String,ItemSearchCatalog.Entry> highestPets=new HashMap<>(),highestMinions=new HashMap<>();for(var entry:entries){if(isPet(entry))highestPets.merge(petBase(entry.internalName()),entry,(left,right)->petTier(right.internalName())>petTier(left.internalName())?right:left);if(isMinion(entry))highestMinions.merge(minionBase(entry.internalName()),entry,(left,right)->minionTier(right.internalName())>minionTier(left.internalName())?right:left);}LinkedHashMap<String,HeadIcon> byKey=new LinkedHashMap<>();for(var entry:entries){if(isPet(entry)&&highestPets.get(petBase(entry.internalName()))!=entry||isMinion(entry)&&highestMinions.get(minionBase(entry.internalName()))!=entry)continue;String key=PREFIX+entry.internalName();byKey.putIfAbsent(key,new HeadIcon(key,entry));}List<HeadIcon> icons=List.copyOf(byKey.values()),npcs=icons.stream().filter(HeadIcon::isNpc).toList(),heads=icons.stream().filter(icon->!icon.isNpc()).toList();data=new Data(icons,heads,npcs,Map.copyOf(byKey));return data;}}
    private static boolean isItemSkin(ItemSearchCatalog.Entry entry){String id=entry.internalName(),name=entry.name().toLowerCase(Locale.ROOT);return id.equals("SKIN")||id.startsWith("SKIN_")||id.endsWith("_SKIN")||id.contains("_SKIN_")||name.matches(".*\\bskin\\b.*");}
    private static boolean isExcludedItemCategory(ItemSearchCatalog.Entry entry){String id=entry.internalName(),name=entry.name().toLowerCase(Locale.ROOT),lore=entry.loreText().toLowerCase(Locale.ROOT),type=entry.lastLoreLine().toUpperCase(Locale.ROOT);boolean dye=id.equals("DYE")||id.startsWith("DYE_")||id.endsWith("_DYE")||id.contains("_DYE_")||name.matches(".*\\bdye$");boolean island=id.contains("FURNITURE")||lore.contains("furniture")||lore.contains("placed on your island")||lore.contains("island size")||lore.contains("island cosmetic");boolean accessory=hasType(type,"ACCESSORY")||hasType(type,"HATCESSORY");boolean equipment=hasType(type,"NECKLACE")||hasType(type,"CLOAK")||hasType(type,"BELT")||hasType(type,"GLOVES")||hasType(type,"BRACELET");return dye||island||accessory||equipment;}
    private static boolean hasType(String line,String type){return (" "+line+" ").matches(".*[^A-Z]"+type+"[^A-Z].*");}
    private static boolean isPet(ItemSearchCatalog.Entry entry){return entry.name().startsWith("[Lvl ")&&entry.internalName().matches(".+;[0-9]+$");}
    private static String petBase(String id){int split=id.lastIndexOf(';');return split<0?id:id.substring(0,split);}
    private static int petTier(String id){int split=id.lastIndexOf(';');if(split<0)return -1;try{return Integer.parseInt(id.substring(split+1));}catch(NumberFormatException ignored){return -1;}}
    private static boolean isMinion(ItemSearchCatalog.Entry entry){return entry.name().contains(" Minion ")&&entry.internalName().matches(".+_GENERATOR_[0-9]+$");}
    private static String minionBase(String id){int split=id.lastIndexOf('_');return split<0?id:id.substring(0,split);}
    private static int minionTier(String id){int split=id.lastIndexOf('_');if(split<0)return -1;try{return Integer.parseInt(id.substring(split+1));}catch(NumberFormatException ignored){return -1;}}
    private static boolean isNpcId(String id){return id!=null&&(id.endsWith("_NPC")||id.contains("_NPC_")||id.endsWith("_MAYOR_MONSTER"));}

    public static final class HeadIcon {
        private final String key;private final ItemSearchCatalog.Entry entry;
        private HeadIcon(String key,ItemSearchCatalog.Entry entry){this.key=key;this.entry=entry;}
        public String key(){return key;}
        public String internalName(){return entry.internalName();}
        public String name(){return entry.name();}
        public boolean isNpc(){return isNpcId(entry.internalName());}
        public ItemStack stack(){return entry.stack();}
    }
    private record Data(List<HeadIcon> icons,List<HeadIcon> heads,List<HeadIcon> npcs,Map<String,HeadIcon> byKey){}
}
