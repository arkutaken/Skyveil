package name.skyveil.client.mining;

import java.util.List;
import java.util.regex.Pattern;

/** Server-reported widget status; never infers readiness from a guessed cooldown. */
public final class PickaxeAbilityParser {
    private static final Pattern HEADER=Pattern.compile("(?i)^Pickaxe Ability(?: Cooldowns?)?\\s*(?::\\s*(.*))?$");
    private static final Pattern TIME=Pattern.compile("(?i)^(?:\\d+(?:\\.\\d+)?s?|\\d+:\\d{2}|(?:\\d+h\\s*)?(?:\\d+m\\s*)?(?:\\d+(?:\\.\\d+)?s)?)$");
    public record Reading(String ability,String status,boolean ready){}
    private PickaxeAbilityParser(){}
    public static Reading parse(List<String> lines){
        for(int i=0;i<lines.size();i++){
            var header=HEADER.matcher(clean(lines.get(i)));
            if(!header.matches())continue;
            String ability="Pickaxe Ability";
            String inline=header.group(1);
            if(inline!=null&&!inline.isBlank()){
                var result=namedStatus(ability,inline);if(result!=null)return result;
                ability=inline;
            }
            // Limit lookahead to this widget's few rows. Do not consume a timer
            // belonging to an unrelated section farther down the player list.
            for(int next=i+1;next<Math.min(lines.size(),i+4);next++){
                String line=clean(lines.get(next));if(line.isEmpty())break;
                var result=namedStatus(ability,line);if(result!=null)return result;
                if(next==i+1&&ability.equals("Pickaxe Ability")&&(!line.contains(":")||line.startsWith("Ability: "))){
                    ability=line.replaceFirst("^Ability:\\s*","");continue;
                }
                break;
            }
            return null;
        }
        return null;
    }
    private static Reading namedStatus(String ability,String raw){
        var direct=status(ability,raw);
        if(direct!=null)return direct;
        // Hypixel can place the ability name and its status on one widget row.
        var named=Pattern.compile("^(.+?)(?:\\s*:\\s*|\\s+-\\s+|\\s+\\()(.+?)\\)?$").matcher(clean(raw));
        if(named.matches())return status(named.group(1),named.group(2));
        return null;
    }
    private static Reading status(String ability,String raw){
        String value=clean(raw).replaceFirst("(?i)^(?:Cooldown|Ready in|Available in|Remaining):?\\s*","");
        if(value.matches("(?i)(?:Ready|Available)(?:!)?"))return new Reading(ability,"Ready",true);
        if(value.matches("(?i)Active(?:!)?"))return new Reading(ability,"Active",false);
        if(!value.isBlank()&&TIME.matcher(value).matches())return new Reading(ability,value,false);
        return null;
    }
    private static String clean(String value){return value.replaceAll("\\u00a7.","").replace('\u00a0',' ').replace('\u202f',' ').strip();}
}
