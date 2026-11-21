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
import tc4revival.aspect.TCAspects;
import tc4revival.research.ResearchCatalog;

/** Converts observations of registered data, never legacy Java, into the modern server catalog. */
public  class ResearchCatalogProvider implements DataProvider {
    private final PackOutput output;
    public ResearchCatalogProvider(PackOutput output) { this.output = output; }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        var path = Path.of(java.util.Objects.requireNonNull(System.getProperty("tc4revival.researchBaseline")))
                .resolve("research-catalog.json");
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            var categories = JsonParser.parseReader(reader).getAsJsonArray();
            modernize(categories);
            ResearchCatalog.decode(categories, TCAspects.catalog());
            // saveStable sorts object keys. Arrays preserve old aspect iteration/tie-breaking order.
            for (var category : categories) for (var research : category.getAsJsonObject().getAsJsonArray("research")) {
                var entry = research.getAsJsonObject();
                var ordered = new com.google.gson.JsonArray();
                entry.getAsJsonObject("aspects").entrySet().forEach(amount -> {
                    var row = new JsonObject();
                    row.addProperty("aspect", amount.getKey());
                    row.add("amount", amount.getValue());
                    ordered.add(row);
                });
                entry.add("aspects", ordered);
            }
            var root = new JsonObject();
            root.addProperty("format", 1);
            root.add("categories", categories);
            return DataProvider.saveStable(cache, root, output.getOutputFolder(PackOutput.Target.DATA_PACK)
                    .resolve("thaumcraft/research_catalog/legacy.json"));
        } catch (java.io.IOException failure) { return CompletableFuture.failedFuture(failure); }
    }
    static void modernize(com.google.gson.JsonArray categories) {
        for(var category:categories)category.getAsJsonObject().getAsJsonArray("research").asList().removeIf(entry->entry.getAsJsonObject().get("key").getAsString().equals("BANNERS"));
        for(var category:categories)for(var element:category.getAsJsonObject().getAsJsonArray("research")) {
            var entry=element.getAsJsonObject();
            if(entry.get("key").getAsString().equals("INFUSIONENCHANTMENT"))
                entry.addProperty("pageCount",entry.get("pageCount").getAsInt()+InfusionEnchantmentRecipes.MODERN.size());
            if(entry.get("key").getAsString().equals("ENTROPICPROCESSING"))
                entry.addProperty("pageCount",entry.get("pageCount").getAsInt()+4);
        }
    }
    @Override public String getName() { return "TC4 research catalog metadata"; }
}
