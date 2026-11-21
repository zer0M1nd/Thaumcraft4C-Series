package conciliummodern;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.entity.EntityAspectJson;
import tc4revival.aspect.item.ItemAspectJson;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises the real core decoders: a singular "item" previously silently lost whole aspect files. */
class InfrastructureResourceTest {
    private static final Path DATA=Path.of("src/main/resources/data/thaumicconcilium");

    @Test void everyExplicitItemAspectRuleUsesTheCoreSchema()throws Exception{
        var entries=new HashMap<String,Map<String,Integer>>();
        try(var files=Files.list(DATA.resolve("item_aspects"))){
            for(var file:files.filter(path->path.toString().endsWith(".json")).toList()){
                var rules=ItemAspectJson.decode(JsonParser.parseString(Files.readString(file)));
                for(var rule:rules){
                    var aspects=new HashMap<String,Integer>();
                    rule.aspects().forEach(quantity->aspects.put(quantity.aspect(),quantity.amount()));
                    for(String item:rule.items())assertNull(entries.put(item,Map.copyOf(aspects)),item);
                }
            }
        }
        // 12 explicit item entries and 7 explicit block-item entries in the original registries.
        assertEquals(19,entries.size());
        assertEquals(Map.of("vitreus",1),entries.get("thaumicconcilium:destabilized_crystal"));
        assertEquals(Map.of("terra",64,"ordo",128,"auram",64),entries.get("thaumicconcilium:hex_of_predictability"));
        assertEquals(Map.of("metallum",64,"sensus",32,"praecantatio",64,"potentia",16,"vacuos",32),entries.get("thaumicconcilium:lithographer"));
        assertEquals(Map.of("cognitio",64,"sensus",64,"exanimis",32,"vacuos",32,"machina",16),entries.get("thaumicconcilium:red_powered_mind"));
        assertEquals(Map.of("cognitio",16,"pannus",16,"instrumentum",16,"vitium",16,"bestia",16),entries.get("thaumicconcilium:warped_drum"));
        assertEquals(Map.of("humanus",4,"vacuos",4,"alienis",4,"perditio",4),entries.get("thaumicconcilium:protobody"));
    }

    @Test void allOriginalEntityScanTagsAreDecodableIncludingTheTwoPreviouslyMissingEntities()throws Exception{
        var entries=new HashMap<String,Map<String,Integer>>();
        try(var files=Files.list(DATA.resolve("entity_aspects"))){
            for(var file:files.filter(path->path.toString().endsWith(".json")).toList()){
                var rules=EntityAspectJson.decode(JsonParser.parseString(Files.readString(file)));
                for(var rule:rules){
                    var aspects=new HashMap<String,Integer>();
                    rule.aspects().forEach(quantity->aspects.put(quantity.aspect(),quantity.amount()));
                    assertNull(entries.put(rule.legacyName(),Map.copyOf(aspects)),rule.legacyName());
                }
            }
        }
        assertEquals(24,entries.size());
        assertEquals(Map.of("alienis",16,"vacuos",16,"praecantatio",16,"corpus",32),entries.get("ThaumicConcilium.MaterialPeeler"));
        assertEquals(Map.of("humanus",4,"victus",4,"metallum",4,"permutatio",4),entries.get("ThaumicConcilium.EnslavedTaintSpider"));
    }

    @Test void livingInfusionsKeepOriginalInstabilityInBothRealVatDataAndBookPages()throws Exception{
        // Original TCRecipeRegistry: BrainLittering/BuffGolem/RedPoweredMind use 10; DopeSquid uses 4.
        var expected=Map.of("brain_littering",10,"buff_golem",10,"red_powered_mind",10,"dope_squid",4);
        var displayed=new HashMap<String,Integer>();
        try(var files=Files.list(DATA.resolve("research_page_recipes"))){
            for(var file:files.filter(path->path.toString().endsWith(".json")).toList()){
                var root=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                for(var entry:root.getAsJsonArray("entries")){
                    var row=entry.getAsJsonObject();String key=row.get("key").getAsString();
                    if(expected.keySet().stream().anyMatch(name->key.equals("thaumicconcilium:"+name)))
                        assertNull(displayed.put(key,row.getAsJsonObject("data").get("instability").getAsInt()),key);
                }
            }
        }
        for(var entry:expected.entrySet()){
            var path=entry.getKey().equals("dope_squid")?Path.of("src/main/resources/conciliummodern/optional/dope_squid.json"):DATA.resolve("vat_infusions/"+entry.getKey()+".json");
            assertEquals(entry.getValue().intValue(),JsonParser.parseString(Files.readString(path)).getAsJsonObject().get("instability").getAsInt(),path.toString());
            assertEquals(entry.getValue(),displayed.get("thaumicconcilium:"+entry.getKey()),"Displayed instability: "+entry.getKey());
        }
    }
}
