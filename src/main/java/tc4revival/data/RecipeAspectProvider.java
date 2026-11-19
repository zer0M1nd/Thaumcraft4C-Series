package tc4revival.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import tc4revival.aspect.TCAspects;
import tc4revival.aspect.recipe.RecipeAspectEngine;
import tc4revival.aspect.recipe.RecipeAspectJson;

public  class RecipeAspectProvider implements DataProvider {
    private final PackOutput output;
    public RecipeAspectProvider(PackOutput output) { this.output = output; }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        try {
            var data = LegacyRecipeEvidence.convert(read("item-definitions.json"), read("registered-aspects.json"),
                    read("crafting-recipes.json"), read("thaumcraft-recipes.json"));
            new RecipeAspectEngine(data, TCAspects.catalog());
            return DataProvider.saveStable(cache, RecipeAspectJson.encode(data),
                    output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("thaumcraft/recipe_aspect_inputs/legacy.json"));
        } catch (IOException error) { return CompletableFuture.failedFuture(error); }
    }
    private JsonArray read(String file) throws IOException {
        String directory = System.getProperty("tc4revival.legacyBaseline");
        if (directory == null) throw new IllegalStateException("DataGen baseline directory was not configured");
        try (var reader = Files.newBufferedReader(Path.of(directory).resolve(file), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonArray();
        }
    }
    @Override public String getName() { return "TC4 legacy recipe aspect calculation inputs"; }
}
