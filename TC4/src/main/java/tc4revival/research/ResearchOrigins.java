package tc4revival.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

/** Source namespaces travel with the catalog through contributions and server synchronization. */
public final class ResearchOrigins {
    private ResearchOrigins() {}

    public static String namespace(String key) {
        int separator = key.indexOf(':');
        return separator < 0 ? "thaumcraft" : key.substring(0, separator);
    }

    public static String modId(JsonObject entry, String fallback) {
        String modId = GsonHelper.getAsString(entry, "sourceModId", fallback);
        if (!modId.matches("[a-z0-9_.-]+"))
            throw new IllegalArgumentException("Invalid research source modid: " + modId);
        return modId;
    }

    public static void annotate(JsonArray categories, String modId) {
        for (var value : categories) {
            var category = value.getAsJsonObject();
            if (!category.has("sourceModId")) category.addProperty("sourceModId", modId);
            for (var research : category.getAsJsonArray("research")) {
                var entry = research.getAsJsonObject();
                if (!entry.has("sourceModId")) entry.addProperty("sourceModId", modId);
            }
        }
    }
}
