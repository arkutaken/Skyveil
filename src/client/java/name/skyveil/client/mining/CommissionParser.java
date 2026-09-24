package name.skyveil.client.mining;

import java.util.*;
import java.util.regex.Pattern;

/** Reads only the Commissions widget, never unrelated percentages elsewhere in TAB. */
public final class CommissionParser {
    private static final Pattern HEADER=Pattern.compile("(?i)^Commissions?(?:\\s*\\([^)]*\\))?\\s*:?$");
    private static final Pattern PROGRESS=Pattern.compile("(?i)^(.+?):\\s*(DONE!?|COMPLETED!?|[0-9]+(?:\\.[0-9]+)?%|[0-9,]+\\s*/\\s*[0-9,]+)\\s*$");
    public record Commission(String name,String progress,double fraction){}
    private CommissionParser(){}
    public static List<Commission> parse(List<String> lines){
        var result=new ArrayList<Commission>();boolean inside=false;
        for(String raw:lines){
            String line=raw.replaceAll("§.","").trim();
            if(HEADER.matcher(line).matches()){inside=true;continue;}
            if(!inside)continue;
            var match=PROGRESS.matcher(line);
            if(!match.matches())break;
            String name=match.group(1).trim(),progress=match.group(2).toUpperCase(Locale.ROOT);
            double fraction;
            try{
                if(progress.startsWith("DONE")||progress.startsWith("COMPLETED")){fraction=1;progress="Done";}
                else if(progress.endsWith("%"))fraction=Double.parseDouble(progress.substring(0,progress.length()-1))/100;
                else{
                    String[] parts=progress.replace(",","").split("/");
                    double total=Double.parseDouble(parts[1].trim());
                    if(total<=0)continue;
                    fraction=Double.parseDouble(parts[0].trim())/total;
                }
            }catch(NumberFormatException invalid){continue;}
            if(name.isEmpty()||!Double.isFinite(fraction))continue;
            result.add(new Commission(name,progress,Math.clamp(fraction,0,1)));
            if(result.size()==6)break;
        }
        return List.copyOf(result);
    }
}
