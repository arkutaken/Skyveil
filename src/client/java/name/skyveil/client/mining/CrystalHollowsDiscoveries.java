package name.skyveil.client.mining;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;

/** Stores entry observations, not guessed structure centers. Scoped to one client world. */
final class CrystalHollowsDiscoveries {
    enum Site {
        DIVAN("Mines of Divan","D",0xFF55FF99),
        GOBLIN("Goblin Queen's Den","G",0xFFFFBB55),
        TEMPLE("Jungle Temple","J",0xFFCC77FF),
        CITY("Precursor City","P",0xFF77DDFF),
        BAL("Bal","B",0xFFFF7755),
        KING("King Yolkar","K",0xFFFFDD55);
        final String label,letter;
        final int color;
        Site(String label,String letter,int color){this.label=label;this.letter=letter;this.color=color;}
        static Site fromLocation(String location){
            if(location==null)return null;
            return switch(location){
                case "Mines of Divan" -> DIVAN;
                case "Goblin Queen's Den","Goblin Queens Den","Goblin Hideout" -> GOBLIN;
                case "Jungle Temple" -> TEMPLE;
                case "Lost Precursor City","Precursor City" -> CITY;
                case "Khazad-dûm","Khazad-dum","Bal" -> BAL;
                default -> null;
            };
        }
    }
    record Entry(Site site,double x,double y,double z){}
    private final EnumMap<Site,Entry> entries=new EnumMap<>(Site.class);
    private Site current;
    void observe(String location,double x,double y,double z){
        // Missing scoreboard data is not evidence of leaving a structure.
        if(location==null)return;
        Site next=Site.fromLocation(location);
        if(next!=null&&next!=current&&Double.isFinite(x)&&Double.isFinite(y)&&Double.isFinite(z))
            entries.put(next,new Entry(next,x,y,z));
        current=next;
    }
    static boolean isGoblinKing(String name){
        return name!=null&&name.replaceAll("\\u00a7.","").strip()
            .replaceFirst("^\\[NPC\\]\\s*","").equals("King Yolkar");
    }
    void observeKing(String name,double x,double y,double z){
        if(isGoblinKing(name)&&Double.isFinite(x)&&Double.isFinite(y)&&Double.isFinite(z))
            entries.put(Site.KING,new Entry(Site.KING,x,y,z));
    }
    Collection<Entry> entries(){return List.copyOf(entries.values());}
    void clear(){entries.clear();current=null;}
}
