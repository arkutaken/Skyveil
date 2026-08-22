package name.skyveil.client.itemsearch;

import com.google.common.collect.ImmutableMultimap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.MissingItemModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/** Lazy self-contained index generated from the MIT-licensed NEU item repository. */
final class ItemSearchCatalog {
    private static final Logger LOGGER=LoggerFactory.getLogger("skyveil-item-search");
    private static final Pattern FORMATTING=Pattern.compile("(?i)§[0-9A-FK-OR]");
    private static volatile List<Entry> entries;
    private ItemSearchCatalog(){}

    static List<Entry> search(String input){
        if(isLoreSearch(input))return List.of();String query=normalize(input);if(query.isBlank())return List.of();String[] terms=query.split(" ");ArrayList<Entry> matches=new ArrayList<>();
        for(Entry entry:entries())if(matches(entry.searchText,terms))matches.add(entry);
        matches.sort(Comparator.comparing((Entry entry)->!entry.cleanName.toLowerCase(Locale.ROOT).startsWith(query)).thenComparing(entry->entry.cleanName,String.CASE_INSENSITIVE_ORDER).thenComparing(entry->entry.raw.i));
        return List.copyOf(matches);
    }
    static boolean isLoreSearch(String input){return input!=null&&input.trim().regionMatches(true,0,"lore:",0,5);}
    private static String loreQuery(String input){String value=input==null?"":input.trim();return value.length()<=5?"":value.substring(5);}
    static boolean matchesLore(ItemStack stack,String input){ItemLore lore=stack==null?null:stack.get(DataComponents.LORE);return lore!=null&&matchesLore(lore.lines(),input);}
    static boolean matchesLore(List<Component> lore,String input){String query=normalize(loreQuery(input));if(!isLoreSearch(input)||query.isBlank()||lore==null||lore.isEmpty())return false;StringBuilder tooltip=new StringBuilder();for(Component line:lore)tooltip.append(line.getString()).append(' ');return normalize(tooltip.toString()).contains(query);}
    static int size(){return entries().size();}
    static List<Entry> headEntries(){return entries().stream().filter(Entry::hasHeadTexture).toList();}
    static List<Entry> allEntries(){return entries();}
    private static boolean matches(String value,String[] terms){for(String term:terms)if(!value.contains(term))return false;return true;}
    private static List<Entry> entries(){List<Entry> current=entries;if(current!=null)return current;synchronized(ItemSearchCatalog.class){if(entries!=null)return entries;ArrayList<Entry> loaded=new ArrayList<>();
        try(var stream=ItemSearchCatalog.class.getResourceAsStream("/assets/skyveil/data/item_search_catalog.json")){
            if(stream==null)throw new IllegalStateException("item_search_catalog.json is missing");List<Raw> raw=new Gson().fromJson(new InputStreamReader(stream,StandardCharsets.UTF_8),new TypeToken<List<Raw>>(){}.getType());if(raw!=null)for(Raw item:raw)if(item!=null&&item.i!=null&&item.n!=null)loaded.add(new Entry(item));
        }catch(Exception exception){LOGGER.error("Could not load the SkyBlock Item Search catalog",exception);}entries=List.copyOf(loaded);LOGGER.info("Loaded {} searchable SkyBlock items",entries.size());return entries;}}
    private static String normalize(String value){return clean(value).toLowerCase(Locale.ROOT).replace('_',' ').replaceAll("[^a-z0-9 ]"," ").trim().replaceAll("\\s+"," ");}
    private static String clean(String value){return FORMATTING.matcher(value==null?"":value.replace("Â§","§")).replaceAll("");}
    private static ItemStack build(Raw raw){
        Item item=baseItem(raw.b);ItemStack stack=new ItemStack(item);stack.set(DataComponents.CUSTOM_NAME,legacy(raw.n));
        if(raw.l!=null&&!raw.l.isEmpty()){ArrayList<Component> lore=new ArrayList<>();for(String line:raw.l)lore.add(legacy(line));stack.set(DataComponents.LORE,new ItemLore(lore));}
        Identifier model=parseId(raw.m);if(model!=null&&hasLoadedModel(model))stack.set(DataComponents.ITEM_MODEL,model);
        if(raw.t!=null&&!raw.t.isBlank()){try{UUID uuid=raw.u==null||raw.u.isBlank()?UUID.nameUUIDFromBytes(raw.i.getBytes(StandardCharsets.UTF_8)):UUID.fromString(raw.u);PropertyMap properties=new PropertyMap(ImmutableMultimap.of("textures",new Property("textures",raw.t)));stack.set(DataComponents.PROFILE,ResolvableProfile.createResolved(new GameProfile(uuid,"SkyveilItem",properties)));}catch(Exception ignored){}}
        return stack;
    }
    private static Item baseItem(String value){if("minecraft:skull".equals(value))return Items.PLAYER_HEAD;Identifier id=parseId(value);if(id==null)return Items.PAPER;Item item=BuiltInRegistries.ITEM.getValue(id);return item==null||item==Items.AIR?Items.PAPER:item;}
    private static boolean hasLoadedModel(Identifier model){
        Minecraft client=Minecraft.getInstance();
        return client!=null&&client.getModelManager()!=null&&!(client.getModelManager().getItemModel(model) instanceof MissingItemModel);
    }
    private static Identifier parseId(String value){if(value==null||value.isBlank())return null;int split=value.indexOf(':');try{return split<0?Identifier.fromNamespaceAndPath("minecraft",value):Identifier.fromNamespaceAndPath(value.substring(0,split),value.substring(split+1));}catch(Exception ignored){return null;}}
    static Component legacy(String input){String value=input==null?"":input.replace("Â§","§");MutableComponent result=Component.empty();ArrayList<ChatFormatting> formats=new ArrayList<>();StringBuilder text=new StringBuilder();for(int index=0;index<value.length();index++){char current=value.charAt(index);if(current=='§'&&index+1<value.length()){append(result,text,formats);ChatFormatting next=ChatFormatting.getByCode(value.charAt(++index));if(next==ChatFormatting.RESET)formats.clear();else if(next!=null&&next!=ChatFormatting.ITALIC){if(next.isColor())formats.removeIf(ChatFormatting::isColor);if(!formats.contains(next))formats.add(next);}continue;}text.append(current);}append(result,text,formats);return result;}
    private static void append(MutableComponent result,StringBuilder text,List<ChatFormatting> formats){if(text.isEmpty())return;result.append(Component.literal(text.toString()).withStyle(formats.toArray(ChatFormatting[]::new)).withStyle(style->style.withItalic(false)));text.setLength(0);}
    static final class Entry{private final Raw raw;final String cleanName,searchText;private ItemStack stack;Entry(Raw raw){this.raw=raw;cleanName=clean(raw.n);searchText=normalize(cleanName+" "+raw.i);}String internalName(){return raw.i;}String name(){return cleanName;}String baseId(){return raw.b;}String modelId(){return raw.m;}boolean hasHeadTexture(){return raw.t!=null&&!raw.t.isBlank();}boolean craftable(){return raw.c;}String recipeQuery(){return cleanName;}int loreLines(){return raw.l==null?0:raw.l.size();}String loreText(){if(raw.l==null)return "";StringBuilder value=new StringBuilder();for(String line:raw.l)value.append(clean(line)).append(' ');return value.toString().trim();}String lastLoreLine(){if(raw.l==null)return "";for(int index=raw.l.size()-1;index>=0;index--){String line=clean(raw.l.get(index)).trim();if(!line.isBlank())return line;}return "";}ItemStack stack(){if(stack==null)stack=build(raw);return stack;}}
    private static final class Raw{String i,n,b,m,u,t;List<String> l;boolean c;}
}
