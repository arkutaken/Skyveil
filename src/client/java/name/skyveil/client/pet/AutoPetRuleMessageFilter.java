package name.skyveil.client.pet;

import name.skyveil.client.config.ConfigManager;
import net.minecraft.network.chat.Component;

import java.util.regex.Pattern;

/** Suppresses Hypixel's Autopet equip notification across its observed text variants. */
public final class AutoPetRuleMessageFilter {
    private static final Pattern MESSAGE=Pattern.compile(
        "(?i)(?:^|\\s)auto\\s*pet\\b.{0,180}\\bequipped\\s+your\\b");

    private AutoPetRuleMessageFilter() {}

    public static boolean shouldSuppress(Component component,boolean overlay){
        return shouldSuppressText(component==null?"":component.getString(),overlay,ConfigManager.get().petDisplay.hideAutoPetRuleMessage);
    }

    static boolean shouldSuppressText(String text,boolean overlay,boolean enabled){return enabled&&!overlay&&matchesText(text);}
    static boolean matchesText(String text){
        if(text==null)return false;
        String normalized=text.replaceAll("(?:\\u00c2)?\\u00a7.","").replace('\u00a0',' ').replaceAll("[\\u200B-\\u200D\\uFEFF]","").trim().replaceAll("\\s+"," ");
        return MESSAGE.matcher(normalized).find();
    }
}
