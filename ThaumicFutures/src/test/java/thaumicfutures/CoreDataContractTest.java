package thaumicfutures;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.*;
import tc4revival.aspect.item.*;
import static org.junit.jupiter.api.Assertions.*;

/** Exercise the shipped resources through TC4's real parsers, not a parallel loader. */
class CoreDataContractTest {
    @Test void declaredServiceProvidersExistInTheCompiledModule() throws Exception {
        try (var files=Files.list(Path.of("src/main/resources/META-INF/services"))) {
            for(var file:files.toList()) for(var line:Files.readAllLines(file)) {
                String provider=line.split("#",2)[0].strip();
                if(!provider.isEmpty()) assertNotNull(getClass().getClassLoader().getResource(provider.replace('.','/')+".class"),
                        "Unresolvable service provider in "+file+": "+provider);
            }
        }
    }
    private static JsonElement json(Path path) throws Exception { return JsonParser.parseString(Files.readString(path)); }
    private static AspectCatalog catalog(boolean avaritia) throws Exception {
        JsonArray base;
        try (var pinned = new java.util.zip.ZipFile("../../../References/Projects/ThaumicOmniverse/evidence/core-jars/Thaumcraft-4-Modern-Port-0.4.68.jar")) {
            base = JsonParser.parseString(new String(pinned.getInputStream(pinned.getEntry("data/thaumcraft/aspect_registry.json")).readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray();
        }
        var additions = json(Path.of("src/main/resources/data/thaumicfutures/thaumcraft/aspects.json")).getAsJsonArray();
        base.addAll(ConditionalAspectDefinitions.filter(additions, id -> avaritia && id.equals("avaritia")));
        return AspectDefinitions.decode(base);
    }
    @Test void everyShippedFileIsWellFormedWithOptionalModsAbsent() throws Exception {
        var catalog = catalog(false);
        try (var files = Files.list(Path.of("src/main/resources/data/thaumicfutures/item_aspects"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                var absent=ItemAspectJson.decodeKnown(json(file), catalog, id -> false, mod -> false);
                // Authored universal tag rules survive absent optional mods, but cannot match absent tags.
                assertTrue(absent.stream().allMatch(rule->rule.items().equals(List.of("*"))&&rule.requiredTag()!=null),file.toString());
                assertTrue(new ItemAspectResolver(absent,catalog).resolve("minecraft:stone",tag->false).isEmpty(),file.toString());
                new ItemAspectResolver(ItemAspectJson.decodeKnown(json(file), catalog, id -> true, mod -> true), catalog);
            }
        }
        try (var files = Files.list(Path.of("src/main/resources/data/thaumicfutures/block_aspects"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".json")).toList())
                assertTrue(new BlockAspectTable(List.of(json(file)), catalog, id -> false, mod -> false).rules().isEmpty());
        }
    }
    @Test void missingGregTechAspectDoesNotEraseMekanismMetal() throws Exception {
        var catalog = catalog(false);
        var rules = ItemAspectJson.decodeKnown(json(Path.of("src/main/resources/data/thaumicfutures/item_aspects/mekanism.json")), catalog, id -> id.equals("mekanism:ingot_osmium"), mod -> mod.equals("mekanism"));
        var result = new ItemAspectResolver(rules, catalog).resolve("mekanism:ingot_osmium", tag -> false).orElseThrow();
        assertEquals(3, result.amount(catalog.require("metallum")));
        assertTrue(rules.stream().flatMap(r -> r.aspects().stream()).noneMatch(q -> q.aspect().contains("nebrisum")));
    }
    @Test void shippedNativeNbtFamiliesRemainDistinct() throws Exception {
        var catalog=catalog(true);
        for(String file:List.of("tconstruct")) {
            var rules=ItemAspectJson.decodeKnown(json(Path.of("src/main/resources/data/thaumicfutures/item_aspects/"+file+".json")),catalog,id->true,mod->true);
            var resolver=new ItemAspectResolver(rules,catalog);
            String id="tconstruct:pick_head";
            var variants=rules.stream().filter(r->r.items().contains(id)).toList();
            assertTrue(variants.size()>1);
            assertTrue(variants.stream().allMatch(r->!r.nbt().isEmpty()),id);
            var values=new HashSet<Map<?,?>>();
            for(var rule:variants) {
                var tag=rule.nbt().example();var value=resolver.resolve(id,t->false,tag).orElseThrow();
                assertEquals(rule.amounts(catalog).view(),value.view(),rule.nbt().canonical());
                values.add(value.view());
            }
            assertTrue(resolver.resolve(id,t->false).isEmpty());
        }
    }
    @Test void vanillaCopperReplacementRequiresItsModernTargetMod() throws Exception {
        var catalog=catalog(false);var table=json(Path.of("src/main/resources/data/thaumicfutures/item_aspects/mekanism.json"));
        assertTrue(ItemAspectJson.decodeKnown(table,catalog,id->id.equals("minecraft:copper_ingot"),mod->false).isEmpty());
        var present=ItemAspectJson.decodeKnown(table,catalog,id->id.equals("minecraft:copper_ingot"),mod->mod.equals("mekanism"));
        assertEquals(1,present.size());
        assertFalse(new ItemAspectResolver(present,catalog).resolve("minecraft:copper_ingot",tag->false).orElseThrow().isEmpty());
    }
    @Test void collaborationResearchUsesValidCoreSchema() throws Exception {
        var definitions = new JsonArray();
        try (var pinned = new java.util.zip.ZipFile("../../../References/Projects/ThaumicOmniverse/evidence/core-jars/Thaumcraft-4-Modern-Port-0.4.68.jar")) {
            definitions.addAll(JsonParser.parseString(new String(pinned.getInputStream(pinned.getEntry("data/thaumcraft/aspect_registry.json")).readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray());
        }
        try (var files = Files.list(Path.of("src/main/resources/data/thaumicfutures/research_page_recipes"))) {
            for (var file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                assertDoesNotThrow(() -> tc4revival.research.ResearchPageRecipes.decode(json(file).getAsJsonObject().getAsJsonArray("entries")), file.toString());
            }
        }
        definitions.addAll(ConditionalAspectDefinitions.filter(json(Path.of("src/main/resources/data/thaumicfutures/thaumcraft/aspects.json")).getAsJsonArray(), id -> true));
        var aspects = AspectDefinitions.decode(definitions);
        // Parent mirrors can be declared in another file; decode the merged catalog as reload does.
        var categories=new JsonArray();
        try(var in=getClass().getResourceAsStream("/data/thaumcraft/research_catalog/legacy.json")) {
            categories.addAll(JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("categories"));
        }
        for(var folder:List.of("../../TC4/src/main/resources/data/thaumcraft/research_catalog","src/main/resources/data/thaumicfutures/research_catalog"))
            try(var files=Files.list(Path.of(folder))) {
                for(var file:files.filter(path->path.toString().endsWith(".json")).sorted().toList())
                    categories.addAll(json(file).getAsJsonObject().getAsJsonArray("categories"));
            }
        var merged=new LinkedHashMap<String,JsonObject>();
        for(var value:categories){
            var category=value.getAsJsonObject();String key=category.get("key").getAsString();
            if(merged.containsKey(key))merged.get(key).getAsJsonArray("research").addAll(category.getAsJsonArray("research"));
            else merged.put(key,category.deepCopy());
        }
        var resolved=new JsonArray();merged.values().forEach(resolved::add);
        assertDoesNotThrow(() -> tc4revival.research.ResearchCatalog.decode(resolved,aspects));
    }
}
