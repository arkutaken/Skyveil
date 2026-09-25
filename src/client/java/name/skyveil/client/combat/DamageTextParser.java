package name.skyveil.client.combat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Strict parser for damage-only Hypixel name tags; health and descriptive labels never match. */
final class DamageTextParser {
    /**
     * Hypixel uses different non-alphanumeric glyphs for damage sources such as
     * critical, ferocity, and Crimson Swipe hits. Keep the numeric grammar strict,
     * but accept a short run of symbol/punctuation decorations on either side so
     * newly introduced damage-source glyphs are not silently discarded.
     */
    private static final Pattern DAMAGE=Pattern.compile("^([^\\p{L}\\p{N}\\s,./]{0,6})([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+(?:\\.[0-9]+)?)([kKmMbBtT]?)([^\\p{L}\\p{N}\\s,./]{0,6})$");
    // BigInteger preserves large damage values before compact display formatting.
    private DamageTextParser(){}

    static Optional<Parsed> parse(String raw){
        if(raw==null)return Optional.empty();String stripped=raw.strip();
        // A leading minus is not damage dealt, and structural punctuation belongs
        // to health/progress labels rather than a floating damage splash.
        if(stripped.indexOf('-')>=0||stripped.indexOf('−')>=0||stripped.indexOf('(')>=0||stripped.indexOf(')')>=0||stripped.indexOf('[')>=0||stripped.indexOf(']')>=0||stripped.indexOf(':')>=0||stripped.indexOf('%')>=0)return Optional.empty();
        Matcher matcher=DAMAGE.matcher(stripped);if(!matcher.matches())return Optional.empty();
        String suffix=matcher.group(3).toUpperCase(Locale.ROOT);if(matcher.group(2).contains(".")&&suffix.isEmpty())return Optional.empty();
        int exponent=switch(suffix){case "K"->3;case "M"->6;case "B"->9;case "T"->12;default->0;};
        try{
            BigDecimal decimal=new BigDecimal(matcher.group(2).replace(",","")).movePointRight(exponent);
            BigInteger value=decimal.toBigIntegerExact();if(value.signum()<=0)return Optional.empty();
            boolean decorated=!matcher.group(1).isEmpty()||!matcher.group(4).isEmpty();return Optional.of(new Parsed(value,decorated));
        }catch(ArithmeticException|NumberFormatException ignored){return Optional.empty();}
    }
    static String format(BigInteger value){
        String digits=value.max(BigInteger.ZERO).toString();StringBuilder result=new StringBuilder(digits.length()+digits.length()/3);
        for(int index=0;index<digits.length();index++){if(index>0&&(digits.length()-index)%3==0)result.append(',');result.append(digits.charAt(index));}return result.toString();
    }
    record Parsed(BigInteger value,boolean decorated){}
}
