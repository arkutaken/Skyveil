package name.skyveil.client.stats;

import java.util.regex.Pattern;
import java.util.Locale;

public final class SkillXpParser {
    private static final Pattern XP=Pattern.compile(
        "(?i)\\+\\s*([0-9][0-9,.]*(?:\\s*[kmb])?)\\s+(Farming|Mining|Combat|Foraging|Fishing|Enchanting|Alchemy|Taming|Carpentry|Runecrafting|Social|Hunting)\\b(?:\\s*\\(([^)]*)\\))?");
    public record Reading(String skill,String gain,String progress,double fraction,int start,int end){}
    private SkillXpParser(){}
    public static Reading parse(String text){
        if(text==null||text.isEmpty())return null;
        // Strip formatting for matching while retaining original character offsets
        // so the HUD can remove only this segment from the styled action bar.
        StringBuilder plain=new StringBuilder();int[] offsets=new int[text.length()];
        for(int i=0;i<text.length();i++){
            if(text.charAt(i)=='\u00a7'&&i+1<text.length()){i++;continue;}
            offsets[plain.length()]=i;plain.append(text.charAt(i)=='\u00a0'?' ':text.charAt(i));
        }
        var match=XP.matcher(plain);
        if(!match.find())return null;
        String progress=match.group(3)==null?"":match.group(3).trim();
        double fraction=-1;
        try{
            if(progress.endsWith("%"))fraction=number(progress.substring(0,progress.length()-1))/100;
            else if(progress.contains("/")){
                String[] pair=progress.split("/",2);
                double max=number(pair[1]);
                if(max>0)fraction=number(pair[0])/max;
            }
        }catch(NumberFormatException ignored){}
        if(!Double.isFinite(fraction))fraction=-1;
        if(fraction>=0)fraction=Math.min(1,fraction);
        return new Reading(match.group(2),"+"+match.group(1).trim(),progress,fraction,offsets[match.start()],offsets[match.end()-1]+1);
    }
    private static double number(String text){
        String value=text.replace(",","").replace(" ","").toLowerCase(Locale.ROOT);
        double multiplier=1;
        if(value.endsWith("k"))multiplier=1000;
        else if(value.endsWith("m"))multiplier=1_000_000;
        else if(value.endsWith("b"))multiplier=1_000_000_000;
        if(multiplier!=1)value=value.substring(0,value.length()-1);
        return Double.parseDouble(value)*multiplier;
    }
}