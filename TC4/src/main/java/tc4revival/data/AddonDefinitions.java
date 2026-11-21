package tc4revival.data;

import com.google.gson.*;
import java.nio.file.Files;
import java.util.*;
import net.minecraftforge.fml.ModList;

/** Immutable startup definitions from installed mods, in reproducible mod-id order. */
public  class AddonDefinitions {
    private AddonDefinitions() {}
    public static JsonArray bundled(String path) {
        try (var stream = AddonDefinitions.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalArgumentException("Missing definition " + path);
            try (var reader = new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonArray();
            }
        } catch (java.io.IOException e) { throw new IllegalStateException(path, e); }
    }
    /** Each mod owns data/<modid>/thaumcraft/<kind>.json. These are startup data, not reloadable packs. */
    public static JsonArray collect(String kind, JsonArray base) {
        var result = base.deepCopy();
        var mods = ModList.get();
        if (mods == null) return result; // Pure data tests have no Forge mod loader.
        for (var mod : mods.getMods().stream().sorted(Comparator.comparing(m -> m.getModId())).toList()) {
            if (mod.getModId().equals("thaumcraft")) continue;
            var path = mod.getOwningFile().getFile().findResource("data", mod.getModId(), "thaumcraft", kind + ".json");
            if (!Files.isRegularFile(path)) continue;
            try (var reader = Files.newBufferedReader(path)) {
                var rows=JsonParser.parseReader(reader).getAsJsonArray();
                var aliasesPath=mod.getOwningFile().getFile().findResource("data",mod.getModId(),"thaumcraft","definition_aliases.json");
                var aliases=new HashSet<String>();
                if(Files.isRegularFile(aliasesPath))try(var aliasesReader=Files.newBufferedReader(aliasesPath)){
                    var declarations=JsonParser.parseReader(aliasesReader).getAsJsonObject();
                    if(declarations.has(kind))for(var alias:declarations.getAsJsonArray(kind))aliases.add(alias.getAsString());
                }
                for(var row:rows) {
                    var value=row.getAsJsonObject();
                    var id=value.get(kind.equals("focus_upgrades")?"key":"tag").getAsString();
                    validateNamespace(mod.getModId(),id,aliases);
                    if(kind.equals("focus_upgrades")&&value.has("id"))throw new IllegalArgumentException("Addon upgrades use named keys, not reserved numeric IDs: "+id);
                }
                result.addAll(rows);
            } catch (Exception e) { throw new IllegalArgumentException("Invalid addon definitions: " + path, e); }
        }
        return thaumcraft.api.data.StartupDefinitions.normalize(kind,result,mods::isLoaded,thaumcraft.api.data.StartupDefinitions.rules());
    }
    /** Explicit per-definition aliases preserve saved addon IDs across module
     * splits. They never permit an addon to claim a core or vanilla identifier;
     * the receiving registries still reject duplicate definitions. */
    public static void validateNamespace(String owner,String id,Set<String> aliases){
        var key=net.minecraft.resources.ResourceLocation.tryParse(id);
        if(key!=null&&key.getNamespace().equals(owner))return;
        if(key==null||key.getNamespace().equals("thaumcraft")||key.getNamespace().equals("minecraft")||!aliases.contains(id))
            throw new IllegalArgumentException("Definition must use its owning namespace or an explicit addon migration alias: "+id);
    }
    public static JsonArray load(String kind) {
        return collect(kind, bundled("/data/thaumcraft/thaumcraft/" + kind + ".json"));
    }
}
