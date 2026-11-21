package tc4revival.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import tc4revival.research.ResearchPageRecipes;
import tc4revival.research.ResearchPages;

public  class ResearchPageRecipesProvider implements DataProvider {
    private final PackOutput output;
    public ResearchPageRecipesProvider(PackOutput output) { this.output = output; }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        var directory = Path.of(java.util.Objects.requireNonNull(System.getProperty("tc4revival.researchBaseline")));
        try (var recipesReader = Files.newBufferedReader(directory.resolve("research-page-recipes.json"), StandardCharsets.UTF_8);
             var pagesReader = Files.newBufferedReader(directory.resolve("research-pages.json"), StandardCharsets.UTF_8)) {
            var entries = JsonParser.parseReader(recipesReader).getAsJsonArray();
            var imported=JsonParser.parseReader(pagesReader).getAsJsonArray();BookDefinitionMigration.upgrade(imported,"thaumcraft");
            var pages = ResearchPages.decode(imported);
            ResearchPageRecipes.decode(entries).validate(pages);
            ResearchPagesProvider.preserveAspectOrder(entries);
            ResearchPageRecipes.decode(entries).validate(pages);
            var root = new JsonObject(); root.addProperty("format", 1); root.add("entries", entries);
            return DataProvider.saveStable(cache, root, output.getOutputFolder(PackOutput.Target.DATA_PACK)
                    .resolve("thaumcraft/research_page_recipes/legacy.json"));
        } catch (java.io.IOException failure) { return CompletableFuture.failedFuture(failure); }
    }
    @Override public String getName() { return "TC4 named research page recipes"; }
}
