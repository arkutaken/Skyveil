package name.skyveil.client.hunting;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.google.common.collect.ImmutableMultimap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.UUID;

/** Self-contained source-specific player heads for Attribute Menu rows. */
final class AttributeShardHeadCatalog {
    private static final Logger LOGGER=LoggerFactory.getLogger("skyveil-attribute-heads");
    private static final Map<String,HeadProfile> HEADS=load();
    private AttributeShardHeadCatalog(){}

    // Build a fresh stack from bundled texture data so callers can customize it
    // without modifying the catalog or requiring an online profile lookup.
    static ItemStack head(String subtype){HeadProfile data=HEADS.get(normalize(subtype));if(data==null)return ItemStack.EMPTY;Property texture=new Property("textures",data.texture());PropertyMap properties=new PropertyMap(ImmutableMultimap.of("textures",texture));GameProfile profile=new GameProfile(data.uuid(),"SkyveilShard",properties);ItemStack head=new ItemStack(Items.PLAYER_HEAD);head.set(DataComponents.PROFILE,ResolvableProfile.createResolved(profile));return head;}
    static int size(){return HEADS.size();}
    static boolean has(String subtype){return HEADS.containsKey(normalize(subtype));}

    private static Map<String,HeadProfile> load(){
        Properties properties=new Properties();
        try(var stream=AttributeShardHeadCatalog.class.getResourceAsStream("/assets/skyveil/data/attribute_shard_heads.properties")){
            if(stream==null)throw new IllegalStateException("resource is missing");properties.load(new InputStreamReader(stream,StandardCharsets.UTF_8));
            TreeMap<String,HeadProfile> result=new TreeMap<>();HashSet<String> textures=new HashSet<>();
            for(String key:properties.stringPropertyNames()){
                String[] fields=properties.getProperty(key,"").split("\\|",2);if(fields.length!=2||!textures.add(fields[1]))continue;
                result.put(normalize(key),new HeadProfile(UUID.fromString(fields[0]),fields[1]));
            }
            if(result.size()!=AttributeShardResolver.catalogSize())throw new IllegalStateException("expected "+AttributeShardResolver.catalogSize()+" unique heads, found "+result.size());
            return Map.copyOf(result);
        }catch(Exception exception){LOGGER.error("Could not load unique Attribute Shard source heads",exception);return Map.of();}
    }
    private static String normalize(String value){return value==null?"":value.trim().toUpperCase(Locale.ROOT).replace(' ','_');}
    private record HeadProfile(UUID uuid,String texture){}
}
