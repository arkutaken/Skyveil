package name.skyveil.client.combat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

/** Classifies styled Hypixel damage splashes before they enter the melee window. */
final class DamageSourceClassifier {
    private DamageSourceClassifier(){}

    static DamageSource styledSource(Component component) {
        Set<Integer> colors=new HashSet<>();
        if(component!=null)for(Component part:component.toFlatList())if(part.getString().chars().anyMatch(Character::isDigit)&&part.getStyle().getColor()!=null)colors.add(part.getStyle().getColor().getValue()&0xFFFFFF);
        if(has(colors,ChatFormatting.GREEN)||has(colors,ChatFormatting.DARK_GREEN))return DamageSource.VENOMOUS;
        if(has(colors,ChatFormatting.BLUE)||has(colors,ChatFormatting.DARK_BLUE)||has(colors,ChatFormatting.AQUA)||has(colors,ChatFormatting.DARK_AQUA))return DamageSource.THUNDERLORD;
        if(has(colors,ChatFormatting.LIGHT_PURPLE)||has(colors,ChatFormatting.DARK_PURPLE))return DamageSource.PET;
        // Critical melee uses a white/yellow/gold/red gradient. A uniformly gold
        // number is Hypixel's fire/burning damage indicator.
        if(colors.size()==1&&has(colors,ChatFormatting.GOLD))return DamageSource.FIRE;
        if(colors.size()==1&&(has(colors,ChatFormatting.BLACK)||has(colors,ChatFormatting.RED)||has(colors,ChatFormatting.DARK_RED)))return DamageSource.OTHER;
        return DamageSource.MELEE;
    }

    static DamageSource physicalFollowUp(BigInteger primary,BigInteger followUp,boolean crimsonEquipped) {
        if(primary==null||primary.signum()<=0||followUp==null||followUp.signum()<=0)return DamageSource.OTHER;
        // Current Crimson Swipe is normally a fraction of its triggering melee hit.
        if(crimsonEquipped&&followUp.multiply(BigInteger.valueOf(4)).compareTo(primary.multiply(BigInteger.valueOf(3)))<0)return DamageSource.CRIMSON_SWIPE;
        BigInteger difference=followUp.subtract(primary).abs();
        if(difference.multiply(BigInteger.valueOf(5)).compareTo(primary)<=0)return DamageSource.FEROCITY;
        return DamageSource.OTHER;
    }

    private static boolean has(Set<Integer> colors,ChatFormatting formatting){Integer color=formatting.getColor();return color!=null&&colors.contains(color&0xFFFFFF);}
}
