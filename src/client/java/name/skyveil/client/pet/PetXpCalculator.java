package name.skyveil.client.pet;

import name.skyveil.client.itemrarity.SkyblockRarity;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Standard rarity-shifted pet XP progression with direct-lore data taking precedence. */
public final class PetXpCalculator {
    private static final int[] LEVEL_XP={
        100,110,120,130,145,160,175,190,210,230,250,275,300,330,360,400,440,490,540,600,
        660,730,800,880,960,1050,1150,1260,1380,1510,1650,1800,1960,2130,2310,2500,2700,2920,3160,3420,
        3700,4000,4350,4750,5200,5700,6300,7000,7800,8700,9700,10800,12000,13300,14700,16200,17800,19500,
        21300,23200,25200,27400,29800,32400,35200,38200,41400,44800,48400,52200,56200,60400,64800,69400,
        74200,79200,84700,90700,97200,104200,111700,119700,128200,137200,146700,156700,167700,179700,192700,
        206700,221700,237700,254700,272700,291700,311700,333700,357700,383700,411700,441700,476700,516700,
        561700,611700,666700,726700,791700,861700,936700,1016700,1101700,1191700,1286700,1386700,1496700,
        1616700,1746700,1886700
    };
    private static final Pattern FRACTION=Pattern.compile("([0-9][0-9,]*(?:\\.[0-9]+)?\\s*[kmb]?)\\s*/\\s*([0-9][0-9,]*(?:\\.[0-9]+)?\\s*[kmb]?)",Pattern.CASE_INSENSITIVE);
    private static final Map<String,Integer> SPECIAL_CAPS=Map.of("GOLDEN_DRAGON",200,"JADE_DRAGON",200,"ROSE_DRAGON",200);
    private PetXpCalculator() {}

    public static int maxLevel(String internalId,String name){
        String id=internalId==null?"":internalId.toUpperCase(Locale.ROOT);
        if(SPECIAL_CAPS.containsKey(id))return SPECIAL_CAPS.get(id);
        String normalized=(name==null?"":name).toUpperCase(Locale.ROOT).replace(' ','_');
        return SPECIAL_CAPS.getOrDefault(normalized,100);
    }

    /** Resolves an exact level from Hypixel's structured cumulative pet XP. */
    public static int levelFromTotalXp(double totalXp,String internalId,SkyblockRarity rarity){
        int cap=maxLevel(internalId,internalId);
        if(totalXp<0||rarity==null)return 0;
        String id=internalId==null?"":internalId.toUpperCase(Locale.ROOT);
        int offset=id.equals("BINGO")?0:switch(rarity){case COMMON->0;case UNCOMMON->6;case RARE->11;case EPIC->16;default->20;};
        boolean dragon=SPECIAL_CAPS.containsKey(id);
        double remaining=totalXp;
        int level=1;
        while(level<cap){
            double cost=requirement(offset,level,dragon);
            if(cost<=0||remaining<cost)break;
            remaining-=cost;level++;
        }
        return level;
    }

    public static Result calculate(List<String> lore,String internalId,int displayedLevel,int maxLevel,SkyblockRarity rarity,double totalXp){
        boolean maxLine=lore.stream().anyMatch(line->line.toUpperCase(Locale.ROOT).contains("MAX LEVEL"));
        if(maxLine||displayedLevel>=maxLevel)return new Result(0,0,true,true);
        for(int index=0;index<lore.size();index++){
            String line=lore.get(index),lower=line.toLowerCase(Locale.ROOT);
            String previous=index==0?"":lore.get(index-1).toLowerCase(Locale.ROOT);
            if(!(lower.contains("xp")||lower.contains("pet exp")||previous.contains("progress to level")))continue;
            Matcher matcher=FRACTION.matcher(line);
            if(matcher.find()){
                double current=number(matcher.group(1)),required=number(matcher.group(2));
                if(required>0&&current>=0&&current<=required)return new Result(current,required,true,false);
            }
        }
        Result fromTotal=fromTotalXp(totalXp,internalId,displayedLevel,maxLevel,rarity);
        return fromTotal==null?new Result(0,0,false,false):fromTotal;
    }

    private static Result fromTotalXp(double totalXp,String internalId,int displayedLevel,int maxLevel,SkyblockRarity rarity){
        if(totalXp<0||rarity==null||displayedLevel<1||displayedLevel>=maxLevel)return null;
        String id=internalId==null?"":internalId.toUpperCase(Locale.ROOT);
        int offset=id.equals("BINGO")?0:switch(rarity){case COMMON->0;case UNCOMMON->6;case RARE->11;case EPIC->16;default->20;};
        boolean dragon=SPECIAL_CAPS.containsKey(id);
        double required=requirement(offset,displayedLevel,dragon);
        if(required<=0)return null;
        double spent=0;
        for(int level=1;level<displayedLevel;level++){
            double cost=requirement(offset,level,dragon);if(cost<=0)return null;spent+=cost;
        }
        double current=totalXp-spent;
        // Never force disagreeing metadata into the displayed [Lvl N] interval.
        if(current<0||current>=required)return null;
        return new Result(current,required,true,false);
    }

    private static double requirement(int offset,int currentLevel,boolean dragon){
        if(currentLevel>=100)return dragon&&currentLevel<200?1886700:-1;
        int index=offset+currentLevel-1;return index>=0&&index<LEVEL_XP.length?LEVEL_XP[index]:-1;
    }

    private static double number(String value){
        String normalized=value.replace(",","").replaceAll("\\s+","").toLowerCase(Locale.ROOT);
        double multiplier=1;
        if(normalized.endsWith("k")){multiplier=1_000;normalized=normalized.substring(0,normalized.length()-1);}
        else if(normalized.endsWith("m")){multiplier=1_000_000;normalized=normalized.substring(0,normalized.length()-1);}
        else if(normalized.endsWith("b")){multiplier=1_000_000_000;normalized=normalized.substring(0,normalized.length()-1);}
        try{return Double.parseDouble(normalized)*multiplier;}catch(NumberFormatException ignored){return -1;}
    }
    public record Result(double current,double required,boolean known,boolean maxed){}
}
