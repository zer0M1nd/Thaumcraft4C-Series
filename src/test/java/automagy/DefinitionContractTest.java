package automagy;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import tc4revival.research.*;
import static org.junit.jupiter.api.Assertions.*;

/** Uses the fixed dependency's real parsers, so a plausible but unsupported JSON shape fails here. */
class DefinitionContractTest {
    private JsonObject json(String path) throws IOException {
        try(var input=Objects.requireNonNull(getClass().getResourceAsStream(path),path);
            var reader=new InputStreamReader(input,StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
    @Test void allHandbookRecipesUseTheBaseModsActualSchema() throws IOException {
        var pages=ResearchPages.decode(json("/data/automagy/research_pages/automagy.json").getAsJsonArray("entries"));
        var recipes=ResearchPageRecipes.decode(json("/data/automagy/research_page_recipes/automagy.json").getAsJsonArray("entries"));
        assertDoesNotThrow(()->recipes.validate(pages));
        Set<String> research=new HashSet<>();
        for(var category:json("/data/automagy/research_catalog/automagy.json").getAsJsonArray("categories"))
            for(var value:category.getAsJsonObject().getAsJsonArray("research")) {
                var entry=value.getAsJsonObject();String key=entry.get("key").getAsString();
                assertTrue(research.add(key),"Duplicate research "+key);
                assertEquals(entry.get("pageCount").getAsInt(),pages.require(key).size(),key);
            }
        assertEquals(research,pages.entries().keySet());
    }
    @Test void researchPrerequisitesResolveAndHaveNoDependencyCycles() throws IOException {
        var graph=new LinkedHashMap<String,List<String>>();
        for(var category:json("/data/automagy/research_catalog/automagy.json").getAsJsonArray("categories"))
            for(var value:category.getAsJsonObject().getAsJsonArray("research")){
                var entry=value.getAsJsonObject();var dependencies=new ArrayList<String>();
                for(String field:List.of("parents","parentsHidden"))if(entry.has(field))
                    for(var parent:entry.getAsJsonArray(field))dependencies.add(parent.getAsString());
                graph.put(entry.get("key").getAsString(),dependencies);
            }
        var available=new HashSet<>(graph.keySet());
        // Inspect the pinned, addon-local base artifact; no game or registry bootstrap is required.
        try(var zip=new java.util.zip.ZipFile(".deps/maven/local/tc4/thaumcraft/0.4.10/thaumcraft-0.4.10.jar")){
            var entries=zip.entries();while(entries.hasMoreElements()){
                var entry=entries.nextElement();if(!entry.getName().contains("/research_catalog/")||!entry.getName().endsWith(".json"))continue;
                try(var reader=new InputStreamReader(zip.getInputStream(entry),StandardCharsets.UTF_8)){
                    var root=JsonParser.parseReader(reader).getAsJsonObject();
                    for(var category:root.getAsJsonArray("categories"))for(var research:category.getAsJsonObject().getAsJsonArray("research"))
                        available.add(research.getAsJsonObject().get("key").getAsString());
                }
            }
        }
        graph.forEach((key,parents)->parents.forEach(parent->assertTrue(available.contains(parent),key+" has unresolved prerequisite "+parent)));
        var completed=new HashSet<String>();
        for(String key:graph.keySet())visitResearch(key,graph,new LinkedHashSet<>(),completed);
    }
    private void visitResearch(String key,Map<String,List<String>> graph,Set<String> visiting,Set<String> completed){
        if(completed.contains(key)||!graph.containsKey(key))return;
        assertTrue(visiting.add(key),"Research dependency cycle: "+visiting+" -> "+key);
        for(String parent:graph.get(key))visitResearch(parent,graph,visiting,completed);
        visiting.remove(key);completed.add(key);
    }
    @Test void netherResearchRequiresBothObservationAndKill() throws IOException {
        var entries=json("/data/automagy/research_catalog/automagy.json").getAsJsonArray("categories").get(0).getAsJsonObject().getAsJsonArray("research");
        Set<String> required=new HashSet<>(List.of("ZOMBIE","GHAST","SKELETON","SLIME","BAT","WISP"));
        for(var value:entries){var entry=value.getAsJsonObject();String key=entry.get("key").getAsString();
            for(String family:new ArrayList<>(required))if(key.equals("automagy:NETHERRUNE_"+family)){
                assertTrue(entry.get("isHidden").getAsBoolean());
                assertFalse(entry.has("entityTriggers"),"A scan alone must not bypass kill discovery");required.remove(family);
            }
        }
        assertTrue(required.isEmpty());
        var rod=json("/data/automagy/recipes/dimension_lure.json").getAsJsonArray("components").get(0).getAsJsonObject();
        assertEquals(-1,rod.get("damage").getAsInt());
        assertEquals("automagy:enchanted",rod.getAsJsonObject("ingredient").get("type").getAsString());
        assertEquals(1,rod.getAsJsonObject("ingredient").get("minimum").getAsInt());
    }
    @Test void externalResourcesAreOwnedByAutomagyAndLanguageFilesHaveSources() throws IOException {
        var definition=json("/data/automagy/thaumcraft/original_resources.json");
        var mapping=definition.getAsJsonObject("mapping");
        assertEquals("Automagy",definition.get("legacy_modid").getAsString());
        assertEquals(64,definition.get("sha256").getAsString().length());
        for(var entry:mapping.entrySet()) {
            assertTrue(entry.getKey().startsWith("assets/automagy/"));
            assertFalse(entry.getKey().contains(".."));
            assertTrue(entry.getValue().getAsString().startsWith("assets/automagy/"));
        }
        for(String language:definition.getAsJsonObject("languages").keySet()) {
            assertTrue(mapping.has(language));assertTrue(mapping.get(language).getAsString().endsWith(".lang"));
        }
    }
    @Test void focusDepthAndConditionalOrnamentHaveAtlasAndModelResources() throws IOException {
        var sources=json("/assets/minecraft/atlases/blocks.json").getAsJsonArray("sources");
        assertTrue(sources.asList().stream().anyMatch(e->e.getAsJsonObject().get("resource").getAsString().equals("automagy:item/crafting_focus_depth")));
        assertEquals("minecraft:builtin/entity",json("/assets/automagy/models/item/crafting_focus_ornament_renderer.json").get("parent").getAsString());
        assertEquals("minecraft:item/generated",json("/assets/automagy/models/item/crafting_focus_ornament.json").get("parent").getAsString());
        var mapping=json("/data/automagy/thaumcraft/original_resources.json").getAsJsonObject("mapping");
        for(String texture:List.of("crafting_focus_depth","crafting_focus_ornament"))assertTrue(mapping.has("assets/automagy/textures/item/"+texture+".png"));
    }

}
