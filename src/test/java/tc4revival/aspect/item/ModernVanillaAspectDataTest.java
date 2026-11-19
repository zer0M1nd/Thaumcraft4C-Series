package tc4revival.aspect.item;

import java.util.*;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.*;
import tc4revival.aspect.entity.*;
import static org.junit.jupiter.api.Assertions.*;

class ModernVanillaAspectDataTest {
    private JsonElement data(String kind,String name) throws Exception {
        try(var reader=new java.io.InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/data/tc4ai_aspects/"+kind+"/"+name+".json")),java.nio.charset.StandardCharsets.UTF_8)) {return JsonParser.parseReader(reader);}
    }
    @Test void bundledTablesPreserveQuantitiesAndKeepEntityFramesOutOfBlocks() throws Exception {
        var catalog=new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
        var items=ItemAspectJson.decode(data("item_aspects","ai_generated"));assertEquals(0,items.size());
        var resolver=new ItemAspectResolver(items,catalog);assertEquals(0,resolver.itemCount());
        for(var row:items)for(var id:row.items())assertEquals(row.amounts(catalog).view(),resolver.resolve(id,t->true).orElseThrow().view());
        var blocks=BlockScanMapping.decode(data("block_scan_targets","ai_modern"));assertEquals(549,blocks.rules().size());
        // Scan mappings remain valid even when an item now uses its original aspect table.
        assertTrue(blocks.resolve("minecraft:cherry_log",Map.of("axis","x")).isPresent());
        for(var frame:List.of("minecraft:item_frame","minecraft:glow_item_frame")) assertTrue(blocks.resolve(frame,Map.of()).isEmpty());
        assertFalse(resolver.contains("minecraft:item_frame"));
        assertTrue(explicit().contains("minecraft:glow_item_frame"));
        var entities=EntityAspectJson.decode(data("entity_aspects","ai_generated"));assertEquals(43,entities.size());
        assertEquals(43,new EntityAspectResolver(entities,catalog).ruleCount());
    }

    @Test void importedItemsCannotReplaceOriginalFullOrPartialDefinitions() throws Exception {
        var catalog=new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
        var imported=ItemAspectJson.decode(data("item_aspects","ai_generated"));
        var direct=new ItemAspectResolver(imported,catalog);
        var recipes=tc4revival.aspect.recipe.RecipeAspectJson.decode(resource("/data/thaumcraft/recipe_aspect_inputs/legacy.json"));
        var aliases=resource("/data/thaumcraft/legacy_item_mappings/vanilla.json").getAsJsonObject();
        var keys=new LinkedHashMap<String,tc4revival.aspect.recipe.RecipeAspectData.Key>();
        for(var value:aliases.getAsJsonArray("items")) {
            var row=value.getAsJsonObject();
            keys.put(row.get("item").getAsString(),new tc4revival.aspect.recipe.RecipeAspectData.Key(row.get("legacy_item").getAsString(),row.get("metadata").getAsInt()));
        }
        var groups=new LinkedHashMap<tc4revival.aspect.recipe.RecipeAspectData.Key,Integer>();
        for(var value:aliases.getAsJsonArray("groups")) {
            var row=value.getAsJsonObject();
            groups.put(new tc4revival.aspect.recipe.RecipeAspectData.Key(row.get("item").getAsString(),row.get("metadata").getAsInt()),row.getAsJsonArray("group").get(0).getAsInt());
        }
        var identity=new LegacyItemIdentity(keys,groups,recipes);
        var baseline=new tc4revival.aspect.recipe.RecipeAspectEngine(recipes,catalog);
        for(var row:imported)for(var id:row.items()) {
            var key=identity.recipeKey(id);
            if(key!=null) {
                var original=baseline.fork().object(identity.scanKey(key));
                assertTrue(original==null || original.view().isEmpty(),"AI overrides existing legacy aspects: "+id);
            }
        }
        for(var file:List.of("vanilla_direct")) {
            for(var row:ItemAspectJson.decode(resource("/data/thaumcraft/item_aspects/"+file+".json")))
                for(var id:row.items())assertFalse(direct.contains(id),"AI overrides original direct rule: "+id);
        }
        for(var file:List.of("vanilla_equipment","thaumcraft_equipment")) {
            for(var row:ItemAspectJson.decode(resource("/data/thaumcraft/item_bonus_aspects/"+file+".json")))
                for(var id:row.items())assertFalse(direct.contains(id),"AI overrides partial equipment rule: "+id);
        }
        var session=new ItemBaseAspectSession(identity,recipes,catalog,direct,(id,tag)->true);
        var chest=session.base("minecraft:chest").orElseThrow();
        assertEquals(Map.of(catalog.require("arbor"),6,catalog.require("vacuos"),4),chest.view());
        assertTrue(explicit().contains("minecraft:cherry_log"),"Modern item retained in complete table");
        assertEquals(Map.of(catalog.require("arbor"),6,catalog.require("vacuos"),4),explicit().resolve("minecraft:chest",t->true).orElseThrow().view());
    }

    private ItemAspectResolver explicit() throws Exception {return new ItemAspectResolver(ItemAspectJson.decode(resource("/data/thaumcraft/item_aspects/explicit.json")),new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList()));}

    private JsonElement resource(String path) throws Exception {
        try(var reader=new java.io.InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream(path),path),java.nio.charset.StandardCharsets.UTF_8)) {return JsonParser.parseReader(reader);}
    }
}
