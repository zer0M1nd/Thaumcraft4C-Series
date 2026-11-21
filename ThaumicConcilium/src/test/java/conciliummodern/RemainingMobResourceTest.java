package conciliummodern;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Source-derived recipe oracles; these tests need no Minecraft registry bootstrap. */
class RemainingMobResourceTest {
    private static JsonObject resource(String path) throws IOException {
        try (var in=RemainingMobResourceTest.class.getResourceAsStream("/"+path)) {
            assertNotNull(in,path);
            return JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
    private static List<String> tokens(JsonArray rows) {
        var result=new ArrayList<String>();
        for(var element:rows){var row=element.getAsJsonObject();result.add(row.get("item").getAsString()+(row.has("metadata")?"@"+row.get("metadata").getAsInt():""));}
        return result;
    }
    private static final List<String> BREW_BASE=List.of("minecraft:nether_wart","witchery:generic@37","witchery:generic@29","minecraft:diamond","minecraft:nether_star","witchery:generic@151","minecraft:glowstone_dust","minecraft:blaze_rod","witchery:generic@11","minecraft:tropical_fish");
    private static List<String> withTail(String... tail){var all=new ArrayList<>(BREW_BASE);all.addAll(List.of(tail));return all;}

    @Test void attackBrewsPreserveOriginalOrderAndHostMetadata() throws Exception {
        var data=resource("conciliummodern/witchery_brews.json");
        assertEquals(withTail("minecraft:fermented_spider_eye","minecraft:glistering_melon_slice","minecraft:glowstone_dust","minecraft:blaze_rod","witchery:generic@114","witchery:generic@35","witchery:generic@24"),tokens(data.getAsJsonArray("attack")));
        assertEquals(withTail("witchery:generic@157","minecraft:glowstone_dust","minecraft:blaze_rod","witchery:generic@114","witchery:generic@35","witchery:generic@24"),tokens(data.getAsJsonArray("attack_werewolf")));
        assertEquals(withTail("witchery:seeds_garlic","minecraft:glowstone_dust","minecraft:blaze_rod","witchery:generic@11","minecraft:tropical_fish","minecraft:fermented_spider_eye","minecraft:glistering_melon_slice","minecraft:glowstone_dust","minecraft:blaze_rod","witchery:generic@114","witchery:generic@35","witchery:generic@24"),tokens(data.getAsJsonArray("attack_vampire")));
    }
    @Test void illFittingKeepsBrambleMetadataAndRepeatedFinalGunpowder() throws Exception {
        var expected=new ArrayList<>(BREW_BASE.subList(0,9));
        expected.addAll(List.of("witchery:bramble@1","minecraft:glowstone_dust","minecraft:blaze_rod","witchery:generic@114","witchery:generic@35","minecraft:glowstone_dust","minecraft:blaze_rod","witchery:glint_weed","witchery:generic@21","minecraft:lapis_lazuli","minecraft:end_stone","minecraft:gunpowder","minecraft:gunpowder"));
        assertEquals(expected,tokens(resource("conciliummodern/witchery_brews.json").getAsJsonArray("ill_fitting")));
    }
    @Test void moonshineUsesHostMandrakeAndWheatOnly() throws Exception {
        assertEquals(List.of("witchery:mandrake_root","minecraft:wheat"),tokens(resource("conciliummodern/witchery_brews.json").getAsJsonArray("moonshine")));
    }
    @Test void tobaccoVatIsConditionallyInjectedAndKeepsOriginalIngredients() throws Exception {
        assertNull(getClass().getResource("/data/thaumicconcilium/vat_infusions/dope_squid.json"),"Absent Thaumic Bases must not expose a recipe with unresolved ingredients");
        var recipe=resource("conciliummodern/optional/dope_squid.json");
        assertEquals("thaumicconcilium:DOPESQUID",recipe.get("research").getAsString());
        assertEquals(4,recipe.get("instability").getAsInt());
        assertEquals("minecraft:squid",recipe.getAsJsonObject("target").get("id").getAsString());
        assertEquals("thaumicconcilium:dope_squid",recipe.getAsJsonObject("output").get("id").getAsString());
        assertEquals(List.of("thaumcraft:air_shard","thaumicbases:tobacco_eldritch","thaumicbases:tobacco_fighting","thaumicbases:tobacco_hunger","thaumicbases:tobacco_knowledge","thaumcraft:fire_shard","thaumicbases:tobacco_mining","thaumicbases:tobacco_sanity","thaumicbases:tobacco_tainted","thaumicbases:tobacco_wispy"),tokens(recipe.getAsJsonArray("ingredients")));
        assertEquals(JsonParser.parseString("{\"herba\":64,\"praecantatio\":64,\"humanus\":32,\"volatus\":64}"),recipe.get("aspects"));
    }
    @Test void witcheryEntitiesResolveTheModernForbiddenMagicAspect() throws Exception {
        for(String mob:List.of("chort","sloppy_alchemist","burned_witch")) {
            var entry=resource("data/thaumicconcilium/entity_aspects/"+mob+".json").getAsJsonArray("entries").get(0).getAsJsonObject();
            assertEquals("thaumicconcilium:"+mob,entry.get("entity").getAsString());
            var aspects=entry.getAsJsonArray("aspects");
            assertEquals(4,aspects.size());
            assertEquals("forbidden_magic:infernus",aspects.get(3).getAsJsonObject().get("aspect").getAsString());
            for(var aspect:aspects)assertEquals(4,aspect.getAsJsonObject().get("amount").getAsInt());
        }
    }
}
