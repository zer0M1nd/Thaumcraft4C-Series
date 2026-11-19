package thaumicomniverse.research;

import com.google.gson.*;
import java.util.function.Predicate;

/** Transform the server's loaded definitions, so the book and progression share one snapshot. */
public final class CollaborationResearch {
    public static final String CATEGORY = "THAUMICOMNIVERSE";

    private CollaborationResearch() {}

    public static JsonArray apply(String folder, JsonArray input, Predicate<String> loaded) {
        var result=filter(folder,input,loaded);
        UniversalEnchantments.research(folder,result);
        // Hand-placed layout (Data/BookLayouts/20261115-futures-omniverse): catalog coordinates are authoritative.
        return result;
    }

    /** Pure dependency filtering, separate from the live registry's generated enchantment pages. */
    public static JsonArray filter(String folder, JsonArray input, Predicate<String> loaded) {
        JsonArray result = new JsonArray();
        for (JsonElement element : input) {
            JsonObject row = element.getAsJsonObject().deepCopy();
            if (!enabled(row, loaded)) continue;
            if (row.has("omniverse_requires_any")) thaumicomniverse.compat.AvaritiaRecipes.resolveDisplayItems(row);
            if (folder.equals("research_page_recipes") && row.has("omniverse_certificate")) {
                var expanded = thaumicomniverse.recipe.CertificateRecipeSerializer.expand(row.getAsJsonObject("omniverse_certificate"));
                var data = row.getAsJsonObject("data");
                var costs = new JsonArray();
                expanded.getAsJsonObject("aspects").entrySet().forEach(entry -> {
                    var cost = new JsonObject();
                    cost.addProperty("aspect", entry.getKey());
                    cost.add("amount", entry.getValue());
                    costs.add(cost);
                });
                data.add("aspects", costs);
                var ingredients = new JsonArray();
                for (var component : expanded.getAsJsonArray("components")) {
                    var stack = new JsonObject();
                    stack.addProperty("kind", "stack");
                    var item = component.getAsJsonObject().getAsJsonObject("ingredient").deepCopy();
                    item.addProperty("count", 1);
                    stack.add("stack", item);
                    ingredients.add(stack);
                }
                data.getAsJsonObject("ingredients").add("elements", ingredients);
                row.remove("omniverse_certificate");
            }
            if (folder.equals("research_catalog")) {
                JsonArray remaining = new JsonArray();
                for (JsonElement entry : row.getAsJsonArray("research")) {
                    JsonObject research = entry.getAsJsonObject();
                    if (!enabled(research, loaded)) continue;
                    // Copper/brass fallback must remain researchable when the TC6 addon is absent.
                    if (research.get("key").getAsString().equals("CONVENIENTTUBES") && !loaded.test("thaumicfutures")) {
                        var parents=new JsonArray();
                        for(var parent:research.getAsJsonArray("parents"))
                            if(!parent.getAsString().equals("ALCHEMICALBRASS"))parents.add(parent);
                        research.add("parents",parents);
                    }
                    remaining.add(research);
                }
                row.add("research", remaining);
            }
            result.add(row);
        }
        return result;
    }

    private static boolean enabled(JsonObject row, Predicate<String> loaded) {
        if (row.has("omniverse_requires_any")) {
            boolean any=false;
            for(var mod:row.getAsJsonArray("omniverse_requires_any")) any|=loaded.test(mod.getAsString());
            if(!any)return false;
        }
        if (row.has("omniverse_items")) for (var item : row.getAsJsonArray("omniverse_items"))
            if (!net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(
                    net.minecraft.resources.ResourceLocation.parse(item.getAsString()))) return false;
        if (!row.has("omniverse_requires")) return true;
        for (JsonElement mod : row.getAsJsonArray("omniverse_requires"))
            if (!loaded.test(mod.getAsString())) return false;
        return true;
    }
}
