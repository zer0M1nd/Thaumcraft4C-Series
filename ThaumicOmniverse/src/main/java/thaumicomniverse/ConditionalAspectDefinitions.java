package thaumicomniverse;

import com.google.gson.JsonArray;
import java.util.function.Predicate;

/** Startup type registration only. Item/block membership and unknown aspects are handled by TC4. */
public final class ConditionalAspectDefinitions {
    private ConditionalAspectDefinitions() {}
    public static JsonArray filter(JsonArray definitions, Predicate<String> loaded) {
        JsonArray result = new JsonArray();
        for (var element : definitions) {
            var row = element.getAsJsonObject();
            boolean enabled = true;
            if (row.get("tag").getAsString().startsWith(ThaumicOmniverse.MOD_ID + ":")) {
                if(row.has("required_mods"))
                    for (var mod : row.getAsJsonArray("required_mods")) enabled &= loaded.test(mod.getAsString());
                if(row.has("required_any_mods")) {
                    boolean any=false;
                    for(var mod:row.getAsJsonArray("required_any_mods"))any|=loaded.test(mod.getAsString());
                    enabled&=any;
                }
            }
            if (enabled) result.add(element.deepCopy());
        }
        return result;
    }
}
