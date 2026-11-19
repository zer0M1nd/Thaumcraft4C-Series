package tbmodern;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AspectDataTest {
    private static final Path DATA=Path.of("src/main/resources/data/thaumicbases");
    private static JsonObject read(String path) throws Exception {
        return JsonParser.parseString(Files.readString(DATA.resolve(path))).getAsJsonObject();
    }
    private static Map<String,JsonArray> rules(String... paths) throws Exception {
        Map<String,JsonArray> result=new LinkedHashMap<>();
        for(String path:paths)for(var value:read(path).getAsJsonArray("entries")) {
            var row=value.getAsJsonObject();
            for(var item:row.getAsJsonArray("items"))assertNull(result.put(item.getAsString(),row.getAsJsonArray("aspects")),"Duplicate aspect registration");
        }
        return result;
    }
    @Test void placedBlocksAndCropSeedsShareRegisteredAspectIdentity() throws Exception {
        var items=rules("item_aspects/explicit.json","item_aspects/derived.json");
        var bright=JsonParser.parseString(Files.readString(Path.of("src/main/resources/tbmodern/bright-aspects.json"))).getAsJsonObject();
        items.put("thaumicbases:node_focus_brightness",bright.getAsJsonObject("pearl").getAsJsonArray("aspects"));
        var configured=tbmodern.resources.ConfiguredDataPack.prepare(true,8);
        var nether=JsonParser.parseString(new String(configured.get(new net.minecraft.resources.ResourceLocation("thaumicbases","item_aspects/zz_optional_nether.json")),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        for(var value:nether.getAsJsonArray("entries")) {
            var row=value.getAsJsonObject();for(var item:row.getAsJsonArray("items"))items.put(item.getAsString(),row.getAsJsonArray("aspects"));
        }
        var scan=read("block_scan_targets/modern.json").getAsJsonArray("rules");
        Map<String,String> mapping=new HashMap<>();
        for(var value:scan) {
            var row=value.getAsJsonObject();String item=row.get("item").getAsString();
            assertTrue(items.containsKey(item)||item.equals("minecraft:crying_obsidian"),"Scan target has no direct aspects: "+item);
            assertNull(mapping.put(row.get("block").getAsString(),item));
        }
        assertEquals("thaumicbases:knose_seed",mapping.get("thaumicbases:knose"));
        assertEquals("thaumicbases:plax_seeds",mapping.get("thaumicbases:plax"));
        assertEquals("thaumicbases:crystal_air_slab",mapping.get("thaumicbases:crystal_air_slab"));
        assertFalse(mapping.containsKey("thaumicbases:pyrofluid"),"Non-item fluid must use the scan callback");
    }
    @Test void oldRoundingAndEquipmentBonusesAreNotInventedOrAddedTwice() throws Exception {
        var items=rules("item_aspects/explicit.json","item_aspects/derived.json");
        assertTrue(items.get("thaumicbases:ender_planks").isEmpty(),"3 arbor * .75 / 4 truncates to zero");
        assertTrue(items.get("thaumicbases:bloody_cloth").isEmpty());
        var bonuses=rules("item_bonus_aspects/equipment.json");
        assertTrue(bonuses.isEmpty(),"All equipment minima are now part of the complete base definition");
        assertEquals(3,amount(items.get("thaumicbases:void_shears"),"meto"));
        assertEquals(4,amount(items.get("thaumicbases:thauminite_shears"),"meto"));
        assertEquals(8,amount(items.get("thaumicbases:thauminite_chestplate"),"tutamen"));
    }
    private static int amount(JsonArray values,String tag){
        return values.asList().stream().map(JsonElement::getAsJsonObject).filter(q->q.get("aspect").getAsString().equals(tag)).mapToInt(q->q.get("amount").getAsInt()).findFirst().orElse(0);
    }
}
