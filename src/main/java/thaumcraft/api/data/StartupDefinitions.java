package thaumcraft.api.data;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;
import net.minecraftforge.fml.ModList;

/** Declarative type-registration extensions, evaluated before addon classes/common setup. */
public class StartupDefinitions {
    public record Rules(boolean allowAspectCycles, Map<String,String> aspectAliases, Map<String,Integer> capacities) {
        public Rules {
            aspectAliases=Map.copyOf(aspectAliases); capacities=Map.copyOf(capacities);
            for(var key:aspectAliases.keySet())canonical(aspectAliases,key);
        }
        public String canonical(String id) { return canonical(aspectAliases,id); }
        private static String canonical(Map<String,String> aliases,String id) {
            var path=new HashSet<String>();
            while(aliases.containsKey(id)) {
                if(!path.add(id))throw new IllegalArgumentException("Cyclic aspect alias " + id);
                id=aliases.get(id);
            }
            return id;
        }
    }
    private static final Rules EMPTY=new Rules(false,Map.of(),Map.of());
    private static ModList source;
    private static Rules cached;
    private StartupDefinitions() {}

    public static synchronized Rules rules() {
        var mods=ModList.get();
        if(mods==null)return EMPTY;
        if(source==mods && cached!=null)return cached;
        boolean cycles=false;
        var aliases=new TreeMap<String,String>();var capacities=new TreeMap<String,Integer>();
        for(var mod:mods.getMods().stream().sorted(Comparator.comparing(m->m.getModId())).toList()) {
            var path=mod.getOwningFile().getFile().findResource("data",mod.getModId(),"thaumcraft","definition_extensions.json");
            if(!Files.isRegularFile(path))continue;
            try(var reader=Files.newBufferedReader(path)) {
                var json=JsonParser.parseReader(reader).getAsJsonObject();
                if(json.get("format").getAsInt()!=1)throw new IllegalArgumentException("Unsupported definition extensions");
                cycles|=json.has("allow_aspect_cycles") && json.get("allow_aspect_cycles").getAsBoolean();
                if(json.has("aspect_aliases"))for(var entry:json.getAsJsonObject("aspect_aliases").entrySet()) {
                    String from=entry.getKey(),to=entry.getValue().getAsString();
                    if(!from.contains(":") || from.startsWith("thaumcraft:") || from.startsWith("minecraft:") || !to.startsWith(mod.getModId()+":"))
                        throw new IllegalArgumentException("Aspect alias must explicitly migrate an addon ID to its declaring owner: " + from);
                    if(aliases.putIfAbsent(from,to)!=null)throw new IllegalArgumentException("Duplicate aspect alias " + from);
                }
                if(json.has("capacity_defaults")) {
                    var declaration=json.getAsJsonObject("capacity_defaults");
                    String resource=declaration.get("resource").getAsString();
                    if(!resource.startsWith("data/"+mod.getModId()+"/") || resource.contains("..") || resource.contains("\\"))
                        throw new IllegalArgumentException("Capacity defaults must reference an owned resource");
                    var defaults=mod.getOwningFile().getFile().findResource(resource.split("/"));
                    try(var defaultsReader=Files.newBufferedReader(defaults)) {
                        var values=JsonParser.parseReader(defaultsReader).getAsJsonObject().getAsJsonObject(declaration.get("member").getAsString());
                        var allowed=new HashSet<String>();declaration.getAsJsonArray("targets").forEach(e->allowed.add(e.getAsString()));
                        for(var entry:values.entrySet()) {
                            if(!allowed.contains(entry.getKey()))throw new IllegalArgumentException("Undeclared capacity override " + entry.getKey());
                            int value=entry.getValue().getAsInt();
                            if(value<1 || value>Integer.MAX_VALUE/100)throw new IllegalArgumentException("Invalid capacity " + entry.getKey());
                            if(capacities.putIfAbsent(entry.getKey(),value)!=null)throw new IllegalArgumentException("Conflicting startup capacity " + entry.getKey());
                        }
                    }
                }
            } catch(Exception error) {throw new IllegalArgumentException("Invalid startup extension " + path,error);}
        }
        var result=new Rules(cycles,aliases,capacities);source=mods;cached=result;return result;
    }

    public static boolean enabled(JsonObject row, Predicate<String> loaded) {
        if(row.has("required_mods"))for(var mod:row.getAsJsonArray("required_mods"))if(!loaded.test(mod.getAsString()))return false;
        if(row.has("required_any_mods")) {
            boolean any=false;for(var mod:row.getAsJsonArray("required_any_mods"))any|=loaded.test(mod.getAsString());
            if(!any)return false;
        }
        return true;
    }

    public static JsonArray normalize(String kind, JsonArray input, Predicate<String> loaded, Rules rules) {
        var output=new JsonArray();
        for(var element:input) {
            var row=element.getAsJsonObject().deepCopy();
            if(!enabled(row,loaded))continue;
            if(kind.equals("aspects")) {
                var id=row.get("tag").getAsString();
                if(!rules.canonical(id).equals(id))continue;
                if(row.has("components")) {
                    var components=new JsonArray();
                    row.getAsJsonArray("components").forEach(e->components.add(rules.canonical(e.getAsString())));
                    row.add("components",components);
                }
            }
            if(kind.equals("wand_rods")) {
                var capacity=rules.capacities().get(row.get("tag").getAsString());
                if(capacity!=null)row.addProperty("capacity",capacity);
            }
            output.add(row);
        }
        if(kind.equals("aspects")) {
            var ids=new HashSet<String>();output.forEach(e->ids.add(e.getAsJsonObject().get("tag").getAsString()));
            for(var target:rules.aspectAliases().values())if(!ids.contains(rules.canonical(target)))
                throw new IllegalArgumentException("Missing canonical aspect " + target);
        }
        return output;
    }
}
