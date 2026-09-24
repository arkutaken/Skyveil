package name.skyveil.client.pet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses only Hypixel's explicit [Lvl N] pet-name indicator. */
public final class PetLevelParser {
    private static final Pattern INDICATOR=Pattern.compile("(?i)\\[\\s*lvl\\s*(\\d{1,4})\\s*]\\s*(.+?)\\s*$");
    private static final Pattern WIDGET_PREFIX=Pattern.compile("(?i)^\\s*lvl\\s*(\\d{1,4})\\s*[:|-]?\\s*(.+?)\\s*$");
    private static final Pattern WIDGET_SUFFIX=Pattern.compile("(?i)^(.+?)\\s*[(\\[]\\s*lvl\\s*(\\d{1,4})\\s*[)\\]]\\s*$");
    private PetLevelParser() {}

    public static Parsed parse(String displayedName) {
        if(displayedName==null)return null;
        String plain=displayedName.replaceAll("\u00a7.","").trim();
        Matcher matcher=INDICATOR.matcher(plain);
        if(!matcher.find())return null;
        try {
            int level=Integer.parseInt(matcher.group(1));
            String name=matcher.group(2).trim();
            return level>0&&!name.isEmpty()?new Parsed(level,name):null;
        } catch(NumberFormatException ignored) { return null; }
    }

    /** Widget lines are already scoped to the active Pet section, so accept its compact layouts too. */
    public static Parsed parseWidget(String displayedName) {
        Parsed bracketed=parse(displayedName);if(bracketed!=null)return bracketed;
        if(displayedName==null)return null;String plain=displayedName.replaceAll("\u00a7.","").trim();
        Matcher suffix=WIDGET_SUFFIX.matcher(plain);if(suffix.find())return parsed(suffix.group(2),suffix.group(1));
        Matcher prefix=WIDGET_PREFIX.matcher(plain);if(prefix.find())return parsed(prefix.group(1),prefix.group(2));
        return null;
    }

    private static Parsed parsed(String levelText,String nameText) {
        try {int level=Integer.parseInt(levelText);String name=nameText.trim();return level>0&&!name.isEmpty()?new Parsed(level,name):null;}
        catch(NumberFormatException ignored){return null;}
    }

    public record Parsed(int level,String name) {}
}
