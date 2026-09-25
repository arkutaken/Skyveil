package name.skyveil.client.mining;

import java.util.Set;

/** North-up region map: X increases rightwards, Z increases downwards. */
public final class CrystalHollowsMap {
    public static final int MIN=201, MAX=824;
    private static final Set<String> AREAS=Set.of("Crystal Hollows","Jungle","Jungle Temple",
        "Mithril Deposits","Mines of Divan","Goblin Holdout","Goblin Queen's Den",
        "Precursor Remnants","Lost Precursor City","Crystal Nucleus","Magma Fields",
        "Khazad-dûm","Khazad-dum","Fairy Grotto","Jungle Village","Goblin Queens Den","Goblin Hideout","Precursor City","Bal");
    private CrystalHollowsMap(){}
    // Normalize a world X or Z coordinate to the closed 0..1 map interval.
    public static double fraction(double coordinate){
        return Math.clamp((coordinate-MIN)/(MAX-MIN),0,1);
    }
    public static boolean isLocation(String line){return location(line)!=null;}
    public static String location(String line){
        String text=line.replaceAll("\\u00a7.","").replace('\u00a0',' ').strip();
        for(String prefix:new String[]{"Area:","Location:","⏣","\uE067"}){
            if(text.startsWith(prefix)){
                String area=text.substring(prefix.length()).strip();
                return AREAS.contains(area)?area:null;
            }
        }
        return null;
    }
    public static String region(double x,double y,double z){
        if(y<64)return "Magma Fields";
        if(x>=450&&x<=560&&z>=450&&z<=560)return "Crystal Nucleus";
        if(z<512)return x<512?"Jungle":"Mithril Deposits";
        return x<512?"Goblin Holdout":"Precursor Remnants";
    }
}
