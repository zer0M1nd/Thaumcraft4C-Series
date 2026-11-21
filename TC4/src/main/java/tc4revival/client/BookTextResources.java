package tc4revival.client;

import com.google.gson.JsonParser;
import java.util.*;
import net.minecraft.locale.Language;
import net.minecraft.server.packs.resources.ResourceManager;
import tc4revival.research.BookTextPatches;

/** All addon namespaces participate; the highest-priority pack wins for an identical resource path. */
public final class BookTextResources {
    private static final org.slf4j.Logger LOGGER=com.mojang.logging.LogUtils.getLogger();
    private static volatile List<BookTextPatches> rules=List.of();
    private static final Set<String> warned=new HashSet<>();
    private static Language language;
    private static String locale="";
    private static Map<String,String> rawDiffs=Map.of();
    private static List<String> phrases=List.of();
    private static long generation;
    private BookTextResources() {}
    public static long generation(){return generation;}
    public static void reload(ResourceManager manager) {
        var loaded=new ArrayList<BookTextPatches>();
        for(var entry:new TreeMap<>(manager.listResources("book_text",id->id.getPath().endsWith(".json"))).entrySet()) {
            try(var reader=entry.getValue().openAsReader()) {loaded.add(BookTextPatches.decode(JsonParser.parseReader(reader).getAsJsonObject()));}
            catch(Exception failure){LOGGER.warn("Cannot load book text rules {}",entry.getKey(),failure);}
        }
        rules=List.copyOf(loaded);language=null;warned.clear();generation++;
    }
    private static void warn(String message){if(warned.add(message))LOGGER.warn(message);}
    public static String patch(String locale,String key,String source) {
        for(var rule:rules)source=rule.apply(locale,key,source,BookTextResources::warn);
        return source;
    }
    private static void refresh(String selected) {
        if(language==Language.getInstance()&&locale.equals(selected))return;
        language=Language.getInstance();locale=selected;
        var words=new LinkedHashSet<String>();var diffs=new HashMap<String,String>();
        for(var rule:rules)if(rule.languages().contains(selected)) {
            words.addAll(rule.protect());
            for(var p:rule.patches())for(String key:p.keys()) {
                String source=language.getOrDefault(key),after=patch(selected,key,source);
                if(!source.equals(after))diffs.put(source,after);
            }
        }
        phrases=words.stream().sorted(Comparator.comparingInt(String::length).reversed()).toList();rawDiffs=Map.copyOf(diffs);
    }
    public static List<String> protect(String locale){refresh(locale);return phrases;}
    /** Covers legacy CUSTOM renderers that resolve Language strings before handing them to Font. */
    public static String patchResolved(String locale,String source){refresh(locale);return rawDiffs.getOrDefault(source,source);}
}
