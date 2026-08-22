package name.skyveil.client.pet;

import name.skyveil.client.config.ConfigManager;
import net.minecraft.network.chat.Component;

import java.util.regex.Pattern;

/** Suppresses only Hypixel's observed Autopet equipped notification. */
public final class AutoPetRuleMessageFilter {
    private static final Pattern MESSAGE=Pattern.compile(
        "(?i)^Autopet\\s+equipped\\s+your\\s+\\[\\s*Lvl\\s+\\d{1,4}\\s*]\\s+.+!\\s+VIEW\\s+RULE$");

    private AutoPetRuleMessageFilter() {}

    public static boolean shouldSuppress(Component component,boolean overlay){
        return shouldSuppressText(component==null?"":component.getString(),overlay,ConfigManager.get().petDisplay.hideAutoPetRuleMessage);
    }

    static boolean shouldSuppressText(String text,boolean overlay,boolean enabled){return enabled&&!overlay&&matchesText(text);}
    static boolean matchesText(String text){
        return text!=null&&MESSAGE.matcher(text.trim().replaceAll("\\s+"," ")).matches();
    }
}
