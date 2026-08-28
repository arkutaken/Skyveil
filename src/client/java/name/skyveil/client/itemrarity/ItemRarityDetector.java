package name.skyveil.client.itemrarity;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads only the canonical rarity footer shown in an item's tooltip. */
public final class ItemRarityDetector {
    private static final Pattern RARITY_WORD=Pattern.compile("(?:^| )(VERY SPECIAL|COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|LEGENJERRY|MYTHIC|DIVINE|SPECIAL|SUPREME|ULTIMATE|ADMIN)(?= |$)");
    private static final Map<ItemStack,CacheEntry> CACHE=Collections.synchronizedMap(new WeakHashMap<>());

    private ItemRarityDetector() {}

    public static SkyblockRarity detect(ItemStack stack) {
        if(stack==null||stack.isEmpty())return null;
        int fingerprint=ItemStack.hashItemAndComponents(stack);
        CacheEntry cached=CACHE.get(stack);
        if(cached!=null&&cached.fingerprint==fingerprint)return cached.highlight.rarity;
        Highlight highlight=detectHighlightUncached(stack);
        // An incomplete network stack can gain lore later without changing its item id. Do not
        // retain UNKNOWN; successful results are still invalidated by the full component hash.
        if(highlight!=null)CACHE.put(stack,new CacheEntry(fingerprint,highlight));
        return highlight==null?null:highlight.rarity;
    }

    public static Highlight detectHighlight(ItemStack stack) {
        if(stack==null||stack.isEmpty())return null;
        int fingerprint=ItemStack.hashItemAndComponents(stack);
        CacheEntry cached=CACHE.get(stack);
        if(cached!=null&&cached.fingerprint==fingerprint)return cached.highlight;
        Highlight highlight=detectHighlightUncached(stack);
        if(highlight!=null)CACHE.put(stack,new CacheEntry(fingerprint,highlight));
        return highlight;
    }

    /**
     * Reads the formatting applied to a particular portion of a tooltip line. This is used for
     * embedded values such as "Held Item: Dwarf Turtle Shelmet", where the value has its own
     * rarity color and the label commonly has a different color.
     */
    public static Highlight detectStyledTextHighlight(Component line,String targetText) {
        if(line==null||targetText==null||targetText.isBlank())return null;
        String targetKey=textKey(targetText);
        Highlight detected=null;
        for(Component part:line.toFlatList()) {
            String text=part.getString().trim();
            String partKey=textKey(text);
            if(partKey.isBlank()||text.indexOf(':')>=0||partKey.contains("helditem")||partKey.contains("petitem"))continue;
            if(!targetKey.contains(partKey)&&!partKey.contains(targetKey))continue;
            TextColor color=part.getStyle().getColor();
            int rgb=color==null?-1:color.getValue()&0xFFFFFF;
            SkyblockRarity rarity=rgb<0?null:SkyblockRarity.fromRgb(rgb);
            if(rarity!=null)detected=new Highlight(rarity,rgb);
        }
        // Do not fall back to the root style: in Hypixel's line the root commonly owns the
        // orange "Held Item:" label while a child owns the pet item's actual rarity color.
        return detected;
    }

    private static String textKey(String value) {
        return value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]","");
    }

    private static Highlight detectHighlightUncached(ItemStack stack) {
        // An explicit displayed lore rarity is authoritative. Inspect tooltip lines from bottom
        // to top while ignoring hidden metadata and rarity words inside descriptions.
        var lore=stack.get(DataComponents.LORE);
        return lore==null?null:detectTooltipLore(lore.lines());
    }

    static Highlight detectTooltipLore(List<Component> lines) {
        for(int index=lines.size()-1;index>=0;index--) {
            Component line=lines.get(index);
            if(line.getString().isBlank())continue;
            Highlight highlight=fromLoreLine(line);
            if(highlight!=null)return highlight;
        }
        return null;
    }

    private static Highlight fromLoreLine(Component component) {
        String normalized=normalize(component.getString());
        Matcher matcher=RARITY_WORD.matcher(normalized);
        while(matcher.find()) {
            SkyblockRarity rarity=SkyblockRarity.fromLabel(matcher.group(1));
            if(rarity==null)continue;
            int styledColor=findRarityWordColor(component,matcher.group(1),rarity);
            if(styledColor>=0)return new Highlight(rarity,rarity.rgb());
        }
        return null;
    }

    private static int findRarityWordColor(Component line,String token,SkyblockRarity rarity) {
        for(Component part:line.toFlatList()) {
            String text=normalize(part.getString());
            if(!text.contains(token))continue;
            TextColor color=part.getStyle().getColor();
            if(color!=null&&(color.getValue()&0xFFFFFF)==rarity.rgb())return color.getValue()&0xFFFFFF;
        }
        TextColor root=line.getStyle().getColor();
        return root!=null&&(root.getValue()&0xFFFFFF)==rarity.rgb()?root.getValue()&0xFFFFFF:-1;
    }

    private static String normalize(String text) {
        return text.toUpperCase(java.util.Locale.ROOT)
            .replace('_',' ').replaceAll("[^A-Z ]+"," ").replaceAll("\\s+"," ").trim();
    }

    public record Highlight(SkyblockRarity rarity,int rgb) {}
    private record CacheEntry(int fingerprint,Highlight highlight) {}
}
