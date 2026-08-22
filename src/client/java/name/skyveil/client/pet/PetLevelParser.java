package name.skyveil.client.pet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses only Hypixel's explicit [Lvl N] pet-name indicator. */
public final class PetLevelParser {
    private static final Pattern INDICATOR=Pattern.compile("(?i)\\[\\s*lvl\\s*(\\d{1,4})\\s*]\\s*(.+?)\\s*$");
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

    public record Parsed(int level,String name) {}
}
