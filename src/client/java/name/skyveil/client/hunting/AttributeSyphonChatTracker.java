package name.skyveil.client.hunting;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Applies authoritative per-attribute syphon result lines to the durable HUD cache. */
public final class AttributeSyphonChatTracker {
    private static final Pattern START=Pattern.compile("(?i)^You used Syphon on (?:(?:([\\d,]+) Shards?)|(?:.+? Shard))!$");
    private static final Pattern RESULT=Pattern.compile("(?i)^\\+([\\d,]+) (.+?) Attribute \\(Level (\\d+)\\)(?: - ([\\d,]+) more to upgrade!| MAXED)$");
    private static final long RESULT_WINDOW_MS=4_000;
    private static long deadline;
    private static long remaining;
    private static boolean changed;
    private AttributeSyphonChatTracker(){}

    public static void onSystemMessage(Component component,boolean overlay){
        Minecraft client=Minecraft.getInstance();if(client==null||!client.isSameThread()||overlay||component==null)return;
        String text=normalize(component.getString());long now=System.currentTimeMillis();if(now>deadline)finish();
        Matcher start=START.matcher(text);if(start.matches()){finish();remaining=start.group(1)==null?1:number(start.group(1));deadline=now+RESULT_WINDOW_MS;return;}
        if(remaining<=0)return;Matcher result=RESULT.matcher(text);if(!result.matches())return;
        long amount=number(result.group(1));String attribute=result.group(2).trim();int tier=(int)Math.min(Integer.MAX_VALUE,number(result.group(3)));int toNext=result.group(4)==null?-1:(int)Math.min(Integer.MAX_VALUE,number(result.group(4)));
        if(amount>0&&tier>=1&&tier<=AttributeProgression.MAX_TIER)changed|=AttributeSessionData.observeSyphon(attribute,amount,tier,toNext);
        remaining=Math.max(0,remaining-amount);if(remaining==0)finish();else deadline=now+RESULT_WINDOW_MS;
    }
    static Event parseResult(String text){Matcher result=RESULT.matcher(normalize(text));if(!result.matches())return null;long amount=number(result.group(1));int tier=(int)Math.min(Integer.MAX_VALUE,number(result.group(3)));int toNext=result.group(4)==null?-1:(int)Math.min(Integer.MAX_VALUE,number(result.group(4)));return new Event(amount,result.group(2).trim(),tier,toNext);}
    static long parseStartAmount(String text){Matcher start=START.matcher(normalize(text));return start.matches()?(start.group(1)==null?1:number(start.group(1))):0;}
    private static long number(String value){try{return Long.parseLong(value.replace(",",""));}catch(Exception ignored){return 0;}}
    private static String normalize(String value){return value==null?"":value.replaceAll("(?i)§[0-9A-FK-OR]","").trim().replaceAll("\\s+"," ");}
    private static void finish(){if(changed)AttributeSessionData.flushSyphonUpdates();deadline=0;remaining=0;changed=false;}
    record Event(long amount,String attributeName,int tier,int toNext){}
}
