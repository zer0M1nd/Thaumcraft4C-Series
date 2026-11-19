package thaumicomniverse.recipe;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import tc4revival.aspect.TCAspects;
import tc4revival.content.InfusionRecipe;

/** Resolve costs once per reload, then use the core's ordinary recipe/network representation. */
public final class CertificateRecipeSerializer extends InfusionRecipe.Serializer {
    public static JsonObject expand(JsonObject source) {
        JsonObject recipe = source.deepCopy();
        JsonObject aspects = new JsonObject();
        for (var aspect : TCAspects.catalog().all())
            aspects.addProperty(aspect.tag(), aspect.isPrimal() ? 640 : 64);
        recipe.add("aspects", aspects);
        if (recipe.has("optional_components")) {
            for (var entry : recipe.getAsJsonArray("optional_components")) {
                var row = entry.getAsJsonObject();
                if (ModList.get().isLoaded(row.get("mod").getAsString()))
                    recipe.getAsJsonArray("components").add(row.get("component").deepCopy());
            }
            recipe.remove("optional_components");
        }
        return recipe;
    }

    @Override public InfusionRecipe fromJson(ResourceLocation id, JsonObject json) {
        return super.fromJson(id, expand(json));
    }
}
