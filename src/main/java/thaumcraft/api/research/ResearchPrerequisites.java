package thaumcraft.api.research;

import com.google.gson.*;
import java.util.*;
import java.util.function.*;
import net.minecraft.resources.ResourceLocation;
import tc4revival.research.*;

/** Server reload-time parent transforms. The materialized JSON is sent through the ordinary definition sync.
 * Register during setup; config changes take effect on /reload. Never grants or fabricates completion. */
public class ResearchPrerequisites {
    public record Parents(List<String> visible,List<String> hidden) {
        public Parents { visible=List.copyOf(visible);hidden=List.copyOf(hidden); }
    }
    public interface Provider { Parents resolve(ResearchDefinition entry, ResearchCatalog catalog, Parents current); }
    private static final Map<ResourceLocation,Provider> PROVIDERS=new TreeMap<>();
    public static synchronized void register(ResourceLocation id,Provider provider) {
        if(PROVIDERS.putIfAbsent(id,Objects.requireNonNull(provider))!=null)throw new IllegalArgumentException("Duplicate prerequisite provider "+id);
    }
    /** Remove a parent when its configuration disables it; this removes both visible and hidden edges. */
    public static void conditional(ResourceLocation id,String research,String parent,BooleanSupplier required) {
        register(id,(entry,catalog,current)->!entry.key().equals(research)||required.getAsBoolean()?current:
            new Parents(current.visible().stream().filter(p->!p.equals(parent)).toList(),current.hidden().stream().filter(p->!p.equals(parent)).toList()));
    }
    /** Replaces a frozen prerequisite list with all currently enabled research selected by the addon.
     * The target and its descendants are excluded to prevent circular KAMI gates. */
    public static void allEnabled(ResourceLocation id,String research,Predicate<ResearchDefinition> include) {
        register(id,(entry,catalog,current)->{
            if(!entry.key().equals(research))return current;
            var parents=catalog.all().stream().filter(ResearchDefinition::countsForProgress).filter(ResearchCatalogs::configuredEnabled).filter(include)
                .filter(candidate->!dependsOn(candidate.key(),research,catalog,new HashSet<>())).map(ResearchDefinition::key).toList();
            return new Parents(parents,List.of());
        });
    }
    private static boolean dependsOn(String key,String target,ResearchCatalog catalog,Set<String> seen) {
        if(key.equals(target))return true;
        if(!seen.add(key))return false;
        var entry=catalog.require(key);
        return java.util.stream.Stream.of(entry.parents(),entry.hiddenParents(),entry.parentsAny()).flatMap(List::stream)
            .anyMatch(parent->dependsOn(parent,target,catalog,seen));
    }
    public static synchronized JsonObject materialize(JsonObject source) {
        return materialize(source,tc4revival.aspect.TCAspects.catalog());
    }
    /** Explicit catalog overload for data validation without loading game registries. */
    public static synchronized JsonObject materialize(JsonObject source,tc4revival.aspect.AspectCatalog aspects) {
        var result=source.deepCopy();var catalog=ResearchDefinitions.decode(source,aspects).catalog();
        for(var category:result.getAsJsonArray("categories"))for(var element:category.getAsJsonObject().getAsJsonArray("research")) {
            var row=element.getAsJsonObject();var entry=catalog.require(row.get("key").getAsString());
            var parents=new Parents(entry.parents(),entry.hiddenParents());
            for(var provider:PROVIDERS.values())parents=Objects.requireNonNull(provider.resolve(entry,catalog,parents));
            for(var key:java.util.stream.Stream.concat(parents.visible().stream(),parents.hidden().stream()).toList()) {
                catalog.require(key);if(key.equals(entry.key()))throw new IllegalArgumentException("Self prerequisite "+key);
            }
            var visible=new JsonArray();parents.visible().forEach(visible::add);row.add("parents",visible);
            var hidden=new JsonArray();parents.hidden().forEach(hidden::add);row.add("parentsHidden",hidden);
        }
        var resolved=ResearchDefinitions.decode(result,aspects).catalog();
        for(var entry:resolved.all())for(var parent:java.util.stream.Stream.of(entry.parents(),entry.hiddenParents(),entry.parentsAny()).flatMap(List::stream).toList())
            if(dependsOn(parent,entry.key(),resolved,new HashSet<>()))throw new IllegalArgumentException("Cyclic prerequisites at "+entry.key());
        var disabled=new JsonArray();catalog.all().stream().filter(entry->!ResearchCatalogs.configuredEnabled(entry)).forEach(entry->disabled.add(entry.key()));
        result.add("disabledResearch",disabled);
        return result;
    }
}
