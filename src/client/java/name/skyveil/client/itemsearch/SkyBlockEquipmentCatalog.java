package name.skyveil.client.itemsearch;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Resolves SkyBlock equipment categories from the bundled item catalog. */
public final class SkyBlockEquipmentCatalog {
    private static volatile Map<String,Integer> types;
    private SkyBlockEquipmentCatalog(){}

    public static int typeForInternalId(String internalId){
        if(internalId==null||internalId.isBlank())return -1;
        return types().getOrDefault(normalize(internalId),-1);
    }

    private static Map<String,Integer> types(){
        Map<String,Integer> current=types;if(current!=null)return current;
        synchronized(SkyBlockEquipmentCatalog.class){
            if(types!=null)return types;
            HashMap<String,Integer> loaded=new HashMap<>();
            for(ItemSearchCatalog.Entry entry:ItemSearchCatalog.allEntries()){
                int type=typeFromText(entry.lastLoreLine());
                if(type>=0)loaded.put(normalize(entry.internalName()),type);
            }
            types=Map.copyOf(loaded);return types;
        }
    }

    public static int typeFromText(String value){
        String text=" "+normalize(value).replace('_',' ')+" ";
        if(text.contains(" necklace "))return 0;
        if(text.contains(" cloak ")||text.contains(" cape "))return 1;
        if(text.contains(" belt "))return 2;
        if(text.contains(" gloves ")||text.contains(" glove ")||text.contains(" bracelet ")||text.contains(" gauntlet "))return 3;
        return -1;
    }

    private static String normalize(String value){return value==null?"":value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_ ]"," ").trim().replaceAll("\\s+"," ");}
}
