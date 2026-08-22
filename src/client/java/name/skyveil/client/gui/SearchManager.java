package name.skyveil.client.gui;

import name.skyveil.client.config.ConfigCategory;
import name.skyveil.client.config.ConfigSubcategory;
import name.skyveil.client.config.SettingDefinition;
import name.skyveil.client.config.SettingsRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Cached, registry-backed search over visible setting names only. */
public final class SearchManager {
    private static final Logger LOGGER=LoggerFactory.getLogger("skyveil-search");
    private static long indexedRevision=-1;
    private static List<IndexedFeature> index=List.of();

    private SearchManager() {}

    public static void initialize(){ensureIndex();}

    public static List<Result> search(String query){
        String normalizedQuery=normalize(query);
        if(normalizedQuery.isEmpty())return List.of();
        ensureIndex();
        List<Result> matches=new ArrayList<>();
        for(IndexedFeature feature:index){
            int rank=feature.normalizedName.equals(normalizedQuery)?0:feature.normalizedName.startsWith(normalizedQuery)?1:
                feature.normalizedName.contains(normalizedQuery)?2:-1;
            if(rank>=0)matches.add(new Result(feature.category,feature.subcategory,feature.setting,rank));
        }
        matches.sort(Comparator.comparingInt(Result::rank)
            .thenComparing(result->result.setting.name,String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Result::path,String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(matches);
    }

    public static String normalize(String value){
        return value==null?"":value.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+"," ");
    }

    private static void ensureIndex(){
        long revision=SettingsRegistry.revision();
        if(indexedRevision==revision)return;
        Map<String,IndexedFeature> unique=new LinkedHashMap<>();
        for(ConfigCategory category:SettingsRegistry.categories())for(ConfigSubcategory subcategory:category.subcategories)
            for(SettingDefinition setting:subcategory.settings){
                String identity=category.id+'\u0000'+subcategory.id+'\u0000'+setting.key;
                IndexedFeature feature=new IndexedFeature(category,subcategory,setting,normalize(setting.name));
                IndexedFeature previous=unique.putIfAbsent(identity,feature);
                if(previous!=null)LOGGER.warn("Duplicate settings registration ignored in search index: {} > {} > {}",category.displayName,subcategory.displayName,setting.key);
            }
        index=List.copyOf(unique.values());indexedRevision=revision;
    }

    public record Result(ConfigCategory category,ConfigSubcategory subcategory,SettingDefinition setting,int rank){
        public String path(){return "general".equals(subcategory.id)?category.displayName:category.displayName+" > "+subcategory.displayName;}
    }

    private record IndexedFeature(ConfigCategory category,ConfigSubcategory subcategory,SettingDefinition setting,String normalizedName) {}
}
