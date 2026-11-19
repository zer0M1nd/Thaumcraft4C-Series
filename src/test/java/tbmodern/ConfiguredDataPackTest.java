package tbmodern;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import tbmodern.resources.ConfiguredDataPack;
import static org.junit.jupiter.api.Assertions.*;

class ConfiguredDataPackTest {
    private static JsonObject file(Map<ResourceLocation,byte[]> files,String path) {
        return JsonParser.parseString(new String(files.get(new ResourceLocation("thaumicbases",path)),StandardCharsets.UTF_8)).getAsJsonObject();
    }
    @Test void configurationChangesRecipeBookAndResearchTogether() {
        for(boolean pearl:new boolean[]{false,true})for(int count:new int[]{1,64}) {
            var files=ConfiguredDataPack.prepare(pearl,count);
            var bright=file(files,"recipes/equipment/brightfocirec.json");
            String center=pearl?"thaumcraft:primordial_pearl":"thaumcraft:primal_charm";
            assertEquals(center,bright.getAsJsonObject("center").getAsJsonObject("ingredient").get("item").getAsString());
            assertEquals(pearl?256:64,bright.getAsJsonObject("aspects").get("auram").getAsInt());
            var research=file(files,"research_catalog/modern.json").getAsJsonArray("categories").get(0).getAsJsonObject().getAsJsonArray("research");
            boolean found=false;
            for(var value:research) {
                var row=value.getAsJsonObject();if(!row.get("key").getAsString().equals("TB.NodeFoci.Bright"))continue;
                found=true;assertEquals(pearl,row.getAsJsonArray("parents").asList().contains(new JsonPrimitive("PRIMPEARL")));
            }
            assertTrue(found);
            var aspectRule=file(files,"item_aspects/configured_brightness.json").getAsJsonArray("entries").get(0).getAsJsonObject();
            assertEquals("thaumicbases:node_focus_brightness",aspectRule.getAsJsonArray("items").get(0).getAsString());
            assertFalse(aspectRule.getAsJsonArray("aspects").isEmpty());
            for(String element:new String[]{"air","fire","water","earth","order","entropy"})
                assertEquals(count,file(files,"recipes/alchemy/shards_"+element+".json").getAsJsonObject("result").get("count").getAsInt());
            var entries=file(files,"research_page_recipes/modern.json").getAsJsonArray("entries");
            int shardDisplays=0;boolean brightDisplay=false;
            for(var entry:entries) {
                var row=entry.getAsJsonObject();String key=row.get("key").getAsString();var data=row.getAsJsonObject("data");
                if(key.startsWith("thaumicbases:alchemy/shards_")) {shardDisplays++;assertEquals(count,data.getAsJsonObject("output").getAsJsonObject("stack").get("count").getAsInt());}
                if(key.equals("thaumicbases:equipment/brightfocirec")) {
                    brightDisplay=true;assertEquals(center,data.getAsJsonObject("input").getAsJsonObject("stack").get("item").getAsString());
                    assertEquals(bright.getAsJsonObject("aspects"),data.getAsJsonObject("aspects"));
                }
            }
            assertEquals(6,shardDisplays);assertTrue(brightDisplay);
        }
    }
    @Test void rejectsOutOfRangeYield() {
        assertThrows(IllegalArgumentException.class,()->ConfiguredDataPack.prepare(true,0));
        assertThrows(IllegalArgumentException.class,()->ConfiguredDataPack.prepare(true,65));
    }
    @Test void cheaperBrightnessRecipeAlsoChangesItsScannableAspects() {
        assertNotEquals(file(ConfiguredDataPack.prepare(true,8),"item_aspects/configured_brightness.json"),
            file(ConfiguredDataPack.prepare(false,8),"item_aspects/configured_brightness.json"));
    }
    @Test void optionalNetherAspectReplacesOnlyTheLegacyCompatibilityContribution() {
        var data=file(ConfiguredDataPack.prepare(true,8,"forbiddenmagic:infernus"),"item_aspects/zz_optional_nether.json");
        assertEquals(3,data.getAsJsonArray("entries").size());
        for(var value:data.getAsJsonArray("entries")) {
            var row=value.getAsJsonObject();var amounts=new java.util.HashMap<String,Integer>();
            row.getAsJsonArray("aspects").forEach(q->amounts.put(q.getAsJsonObject().get("aspect").getAsString(),q.getAsJsonObject().get("amount").getAsInt()));
            assertEquals(1,amounts.get("forbiddenmagic:infernus"));
            if(row.getAsJsonArray("items").get(0).getAsString().endsWith("_log"))assertEquals(1,amounts.get("ignis"));
            else assertFalse(amounts.containsKey("ignis"));
        }
        var fallback=file(ConfiguredDataPack.prepare(true,8),"item_aspects/zz_optional_nether.json");
        assertEquals(3,fallback.getAsJsonArray("entries").size());
    }
    @Test void netherRecipeResearchAndDisplayUseTheSameOptionalAspect() {
        for(String nether:new String[]{"ignis","forbidden_magic:infernus"}) {
            var files=ConfiguredDataPack.prepare(true,8,nether);
            var recipe=file(files,"recipes/equipment/nethersaplingrec.json");
            assertEquals(32,recipe.getAsJsonObject("aspects").get(nether).getAsInt());
            if(!nether.equals("ignis"))assertFalse(recipe.getAsJsonObject("aspects").has("ignis"));
            for(var value:file(files,"research_page_recipes/modern.json").getAsJsonArray("entries")) {
                var row=value.getAsJsonObject();if(row.get("key").getAsString().endsWith("/nethersaplingrec"))
                    assertEquals(recipe.getAsJsonObject("aspects"),row.getAsJsonObject("data").getAsJsonObject("aspects"));
            }
            for(var category:file(files,"research_catalog/modern.json").getAsJsonArray("categories"))for(var value:category.getAsJsonObject().getAsJsonArray("research")) {
                var row=value.getAsJsonObject();if(!row.get("key").getAsString().equals("TB.NetherSapling"))continue;
                assertEquals(nether.equals("ignis")?16:8,row.getAsJsonObject("aspects").get("ignis").getAsInt());
                if(!nether.equals("ignis"))assertEquals(8,row.getAsJsonObject("aspects").get(nether).getAsInt());
            }
        }
    }
    @Test void authorOnlyResearchAndModernTagInputsSurviveDataPreparation() {
        var files=ConfiguredDataPack.prepare(true,8);
        var keys=new java.util.HashSet<String>();
        for(var category:file(files,"research_catalog/modern.json").getAsJsonArray("categories"))for(var row:category.getAsJsonObject().getAsJsonArray("research"))
            assertTrue(keys.add(row.getAsJsonObject().get("key").getAsString()),"Duplicate research key");
        assertTrue(keys.containsAll(java.util.List.of("TB.Enchantments","TB.TaintMinor","TB.HerobrinesScythe")));
        for(String element:new String[]{"air","fire","water","earth","order","entropy"})
            assertEquals("forge:ores/infused_"+element,file(files,"recipes/alchemy/shards_"+element+".json").getAsJsonObject("catalyst").get("tag").getAsString());
    }
}
