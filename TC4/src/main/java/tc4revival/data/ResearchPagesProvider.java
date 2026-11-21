package tc4revival.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
import tc4revival.research.ResearchPages;

public  class ResearchPagesProvider implements DataProvider {
    private final PackOutput output;
    public ResearchPagesProvider(PackOutput output) { this.output = output; }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        var directory = Path.of(java.util.Objects.requireNonNull(System.getProperty("tc4revival.researchBaseline")));
        try (var pagesReader = Files.newBufferedReader(directory.resolve("research-pages.json"), StandardCharsets.UTF_8);
             var catalogReader = Files.newBufferedReader(directory.resolve("research-catalog.json"), StandardCharsets.UTF_8)) {
            var entries = JsonParser.parseReader(pagesReader).getAsJsonArray();
            var categories=JsonParser.parseReader(catalogReader).getAsJsonArray();ResearchCatalogProvider.modernize(categories);
            entries.asList().removeIf(entry->entry.getAsJsonObject().get("key").getAsString().equals("BANNERS"));
            appendCopperProcessing(entries);
            for(var entry:entries)if(entry.getAsJsonObject().get("key").getAsString().equals("INFUSIONENCHANTMENT")) {
                var pages=entry.getAsJsonObject().getAsJsonArray("pages");
                for(String name:InfusionEnchantmentRecipes.MODERN) {
                    var page=new JsonObject();page.addProperty("type","INFUSION_ENCHANTMENT");
                    var recipe=new JsonObject();recipe.addProperty("kind","reference");recipe.addProperty("class","modern");
                    var keys=new JsonArray();keys.add("thaumcraft:infusion/enchantment/"+name);recipe.add("keys",keys);page.add("recipe",recipe);pages.add(page);
                }
            }
            var catalog = ResearchCatalog.decode(categories, TCAspects.catalog());
            BookDefinitionMigration.upgrade(entries,"thaumcraft");
            ResearchPages.decode(entries).validate(catalog);
            preserveAspectOrder(entries);
            ResearchPages.decode(entries).validate(catalog);
            var root = new JsonObject(); root.addProperty("format", 1); root.add("entries", entries);
            return DataProvider.saveStable(cache, root, output.getOutputFolder(PackOutput.Target.DATA_PACK)
                    .resolve("thaumcraft/research_pages/legacy.json"));
        } catch (java.io.IOException failure) { return CompletableFuture.failedFuture(failure); }
    }
    static void preserveAspectOrder(JsonElement element) {
        if (element.isJsonArray()) element.getAsJsonArray().forEach(ResearchPagesProvider::preserveAspectOrder);
        else if (element.isJsonObject()) {
            var object = element.getAsJsonObject();
            if (object.has("aspects")) {
                var ordered = new JsonArray();
                object.getAsJsonObject("aspects").entrySet().forEach(amount -> {
                    var row = new JsonObject(); row.addProperty("aspect", amount.getKey()); row.add("amount", amount.getValue()); ordered.add(row);
                });
                object.add("aspects", ordered);
            }
            object.entrySet().forEach(entry -> preserveAspectOrder(entry.getValue()));
        }
    }
    static void appendCopperProcessing(JsonArray entries) {
        for(var entry:entries)if(entry.getAsJsonObject().get("key").getAsString().equals("ENTROPICPROCESSING")) {
            var pages=entry.getAsJsonObject().getAsJsonArray("pages");
            var text=new JsonObject();text.addProperty("type","TEXT");text.addProperty("text","thaumcraft.research.copper_processing");pages.add(text);
            for(String group:java.util.List.of("oxidize_","oxidize_waxed_","dewax_waxed_")) {
                var page=new JsonObject();page.addProperty("type","CRUCIBLE_CRAFTING");
                var recipe=new JsonObject();recipe.addProperty("kind","reference");recipe.addProperty("class","modern");
                var keys=new JsonArray();
                for(String shape:java.util.List.of("copper","cut_copper","cut_copper_stairs","cut_copper_slab"))
                    for(String stage:java.util.List.of("","exposed_","weathered_","oxidized_")) {
                        if(!group.startsWith("dewax")&&stage.equals("oxidized_"))continue;
                        String name=stage.isEmpty()&&shape.equals("copper")?"copper_block":stage+shape;
                        keys.add("thaumcraft:crucible/"+group+name);
                    }
                recipe.add("keys",keys);page.add("recipe",recipe);pages.add(page);
            }
        }
    }
    @Override public String getName() { return "TC4 research book pages"; }
}
