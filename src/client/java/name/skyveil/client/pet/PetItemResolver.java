package name.skyveil.client.pet;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;

/** Resolves only representations that can be reconstructed safely from client-visible IDs. */
public final class PetItemResolver {
    private PetItemResolver() {}
    public static String displayName(String id){
        if(id==null||id.isBlank())return "";
        String[] words=id.toLowerCase(Locale.ROOT).replace("pet_item_","").split("_");
        StringBuilder result=new StringBuilder();
        for(String word:words)if(!word.isBlank())result.append(result.isEmpty()?"":" ").append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        return result.toString();
    }
    public static ItemStack resolve(String id,String name){
        String value=((id==null?"":id)+" "+(name==null?"":name)).toUpperCase(Locale.ROOT);
        if(value.contains("SHELMET"))return new ItemStack(Items.TURTLE_HELMET);
        if(value.contains("TEXTBOOK"))return new ItemStack(Items.ENCHANTED_BOOK);
        if(value.contains("SADDLE"))return new ItemStack(Items.SADDLE);
        if(value.contains("EXP SHARE"))return new ItemStack(Items.EXPERIENCE_BOTTLE);
        if(value.contains("TIER BOOST"))return new ItemStack(Items.NETHER_STAR);
        if(value.contains("PLUSHIE")||value.contains("RELIC")||value.contains("CLOVER")||value.contains("BANDANA"))return new ItemStack(Items.PLAYER_HEAD);
        return value.isBlank()?ItemStack.EMPTY:new ItemStack(Items.NAME_TAG);
    }
}
