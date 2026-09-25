package name.skyveil.client.stats;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Matches explicit action-bar stats, including Hypixel resource-pack glyphs. */
public final class StatParser {
    private static final String N="([0-9][0-9,]*(?:\\.[0-9]+)?)";
    // Glyph identities verified against Hypixel's current resource-pack icon mapping:
    // https://github.com/SkyblockerMod/Skyblocker/blob/main/src/main/java/de/hysky/skyblocker/utils/SkyBlockIcons.java
    private static final Pattern RESOURCE=Pattern.compile(
        "(?<![\\d.,/])"+N+"\\s*/\\s*"+N+"\\s*(❤|♥|\\uE010|Health\\b|[✎\\uE003](?:\\s*Mana\\b)?|Mana\\b|[♨\\uE028](?:\\s*Vitality\\b)?|Vitality\\b)"
        +"(?:\\s*"+N+"[ʬ\\uE017])?",Pattern.CASE_INSENSITIVE);
    private static final Pattern SCALAR=Pattern.compile("(?<![\\d.,/])"+N+"\\s*([❈\\uE008](?:\\s*Defense\\b)?|Defense\\b|[✦\\uE022](?:\\s*Speed\\b)?|Speed\\b)",Pattern.CASE_INSENSITIVE);
    private StatParser(){}
    // start/end are offsets in the original text (end exclusive), not the stripped
    // matching buffer. Scalar stats use maximum=0; overflow is separate from the bar.
    public record Reading(PlayerStat stat,double value,double maximum,int start,int end,double overflow){
        public Reading(PlayerStat stat,double value,double maximum,int start,int end){this(stat,value,maximum,start,end,0);}
        public double fraction(){return maximum>0?Math.max(0,Math.min(1,value/maximum)):0;}
    }
    public static List<Reading> parse(String text){
        List<Reading> result=new ArrayList<>();
        if(text==null||text.isEmpty())return result;
        // Keep source offsets so callers can remove values without losing other components' styles.
        StringBuilder normalized=new StringBuilder();
        int[] offsets=new int[text.length()];
        for(int i=0;i<text.length();i++){
            if(text.charAt(i)=='\u00a7'&&i+1<text.length()){i++;continue;}
            offsets[normalized.length()]=i;
            normalized.append(text.charAt(i)=='\u00a0'?' ':text.charAt(i));
        }
        var resource=RESOURCE.matcher(normalized);
        while(resource.find()){
            String symbol=resource.group(3).toLowerCase(java.util.Locale.ROOT);
            PlayerStat stat=symbol.startsWith("❤")||symbol.startsWith("♥")||symbol.startsWith("\uE010")||symbol.startsWith("health")?PlayerStat.HEALTH
                :symbol.startsWith("♨")||symbol.startsWith("\uE028")||symbol.startsWith("vitality")?PlayerStat.VITALITY:PlayerStat.MANA;
            double value=number(resource.group(1)),max=number(resource.group(2)),overflow=resource.group(4)==null?0:number(resource.group(4));
            if(Double.isFinite(value)&&Double.isFinite(max)&&Double.isFinite(overflow)&&max>0)
                result.add(new Reading(stat,value,max,offsets[resource.start()],offsets[resource.end()-1]+1,overflow));
        }
        var scalar=SCALAR.matcher(normalized);
        while(scalar.find()){
            String symbol=scalar.group(2);
            PlayerStat stat=symbol.startsWith("❈")||symbol.startsWith("\uE008")||symbol.equalsIgnoreCase("Defense")?PlayerStat.DEFENSE:PlayerStat.SPEED;
            double value=number(scalar.group(1));
            if(Double.isFinite(value))result.add(new Reading(stat,value,0,offsets[scalar.start()],offsets[scalar.end()-1]+1));
        }
        return result;
    }
    private static double number(String value){try{return Double.parseDouble(value.replace(",",""));}catch(NumberFormatException ignored){return Double.NaN;}}
}