package conciliummodern;

import com.google.gson.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArsResourceContractTest {
    private static JsonObject read(String name)throws Exception{return JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/thaumicconcilium/"+name+".json"))).getAsJsonObject();}
    @Test void optionalCriterionAndSurvivalResearchDoNotRequireUnrelatedMagicMods()throws Exception{
        var advancement=read("advancements/crimson_raid_skill");assertTrue(advancement.getAsJsonArray("conditions").asList().stream().anyMatch(e->e.getAsJsonObject().get("type").getAsString().equals("forge:mod_loaded")&&e.getAsJsonObject().get("modid").getAsString().equals("arsmagicalegacy")));
        var rows=read("research_catalog/main").getAsJsonArray("categories").get(0).getAsJsonObject().getAsJsonArray("research");JsonObject school=null,rod=null;
        for(var row:rows){var value=row.getAsJsonObject();if(value.get("key").getAsString().equals("thaumicconcilium:ARS_SCHOOLS"))school=value;if(value.get("key").getAsString().equals("thaumicconcilium:ROD_witchwood"))rod=value;}
        assertNotNull(school);assertTrue(school.get("isAutoUnlock").getAsBoolean());assertNotNull(rod);var parents=rod.getAsJsonArray("parents").toString();assertTrue(parents.contains("thaumicconcilium:ARS_SCHOOLS"));assertFalse(parents.contains("forbidden_magic:SCHOOLS"));
    }
    @Test void originalRaidCostsAndMoonstoneBraceletIngredientsStayDistinctFromVinteum()throws Exception{
        var raid=read("spell_parts/crimson_raid");assertEquals(3000,raid.get("manaCost").getAsInt());assertEquals(300,raid.get("burnout").getAsInt());var recipe=raid.getAsJsonArray("recipe");assertEquals(9,recipe.size());assertEquals(10000,recipe.get(8).getAsJsonObject().get("amount").getAsInt());assertEquals("DARK",recipe.get(8).getAsJsonObject().getAsJsonArray("types").get(0).getAsString());
        var bracelet=read("recipes/witchwood_bracelet");long moons=bracelet.getAsJsonArray("components").asList().stream().filter(e->e.getAsJsonObject().getAsJsonObject("ingredient").get("item").getAsString().equals("arsmagicalegacy:moonstone")).count();assertEquals(2,moons);assertEquals("thaumicconcilium:witchwood_staff",bracelet.getAsJsonObject("center").getAsJsonObject("ingredient").get("item").getAsString());
    }
}
