package thaumicfutures;

import com.google.gson.*;
import java.util.function.Predicate;

/** Transform the server's loaded definitions, so the book and progression share one snapshot. */
public final class FutureResearch {
    public static final String CATEGORY = "THAUMICFUTURES";

    private FutureResearch() {}

    public static JsonArray apply(String folder, JsonArray input, Predicate<String> loaded) {
        var result=filter(folder,input,loaded);
        // Hand-placed layout (Data/BookLayouts/20261115-futures-omniverse): catalog coordinates are authoritative.
        return result;
    }

    /** Pure dependency filtering, separate from the live registry's generated enchantment pages. */
    public static JsonArray filter(String folder, JsonArray input, Predicate<String> loaded) {
        JsonArray result = new JsonArray();
        for (JsonElement element : input) {
            JsonObject row = element.getAsJsonObject().deepCopy();
            if (!enabled(row, loaded)) continue;
            if (folder.equals("research_catalog")) {
                JsonArray remaining = new JsonArray();
                for (JsonElement entry : row.getAsJsonArray("research")) {
                    JsonObject research = entry.getAsJsonObject();
                    if (!enabled(research, loaded)) continue;
                    remaining.add(research);
                }
                row.add("research", remaining);
            }
            result.add(row);
        }
        return result;
    }

    private static boolean enabled(JsonObject row, Predicate<String> loaded) {
        if (row.has("omniverse_items")) for (var item : row.getAsJsonArray("omniverse_items"))
            if (!net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(
                    net.minecraft.resources.ResourceLocation.parse(item.getAsString()))) return false;
        if (!row.has("omniverse_requires")) return true;
        for (JsonElement mod : row.getAsJsonArray("omniverse_requires"))
            if (!loaded.test(mod.getAsString())) return false;
        return true;
    }
}
