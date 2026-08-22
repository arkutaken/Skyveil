package name.skyveil.client.itemrarity;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads explicit SkyBlock metadata first, then the canonical rarity line at the bottom of item lore. */
public final class ItemRarityDetector {
    private static final Pattern METADATA=Pattern.compile("(?i)(?:rarity|tier)[^a-z]{0,12}(very[_ ]special|common|uncommon|rare|epic|legendary|legenjerry|mythic|divine|special|supreme|ultimate|admin)");
    private static final Pattern LORE_LINE=Pattern.compile("(?:^| )(VERY SPECIAL|COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|LEGENJERRY|MYTHIC|DIVINE|SPECIAL|SUPREME|ULTIMATE|ADMIN)(?= |$)");
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
        // The displayed final lore rarity is authoritative. In particular, custom item metadata
        // may describe a recombobulated item's base tier rather than its current visible tier.
        var lore=stack.get(DataComponents.LORE);
        if(lore!=null) {
            var lines=lore.lines();
            for(int index=lines.size()-1;index>=0;index--) {
                Highlight highlight=fromLoreLine(lines.get(index));
                if(highlight!=null)return highlight;
            }
        }
        var custom=stack.get(DataComponents.CUSTOM_DATA);
        if(custom!=null) {
            Matcher matcher=METADATA.matcher(custom.copyTag().toString());
            if(matcher.find()) {
                SkyblockRarity rarity=SkyblockRarity.fromLabel(matcher.group(1));
                if(rarity!=null)return new Highlight(rarity,rarity.rgb());
            }
        }
        return null;
    }

    private static Highlight fromLoreLine(Component component) {
        String normalized=component.getString().toUpperCase(java.util.Locale.ROOT)
            .replace('_',' ').replaceAll("[^A-Z ]+"," ").replaceAll("\\s+"," ").trim();
        Matcher matcher=LORE_LINE.matcher(normalized);
        if(!matcher.find())return null;
        SkyblockRarity rarity=SkyblockRarity.fromLabel(matcher.group(1));
        if(rarity==null)return null;
        String token=matcher.group(1);
        int styledColor=findTokenColor(component,token);
        return new Highlight(rarity,styledColor>=0?styledColor:rarity.rgb());
    }

    private static int findTokenColor(Component line,String token) {
        String firstWord=token.substring(0,token.indexOf(' ')<0?token.length():token.indexOf(' '));
        for(Component part:line.toFlatList()) {
            String text=part.getString().toUpperCase(java.util.Locale.ROOT);
            if(text.contains(token)||text.contains(firstWord)) {
                TextColor color=part.getStyle().getColor();
                if(color!=null)return color.getValue()&0xFFFFFF;
            }
        }
        TextColor root=line.getStyle().getColor();
        return root==null?-1:root.getValue()&0xFFFFFF;
    }

    public record Highlight(SkyblockRarity rarity,int rgb) {}
    private record CacheEntry(int fingerprint,Highlight highlight) {}
}
