package thaumicomniverse;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.*;
import tc4revival.aspect.item.*;
import static org.junit.jupiter.api.Assertions.*;
class AvaritiaDataContractTest {
    private static JsonElement json(Path path) throws Exception { return JsonParser.parseString(Files.readString(path)); }
    private static AspectCatalog catalog(boolean loaded) throws Exception {
        var base=json(Path.of("../../TC4/src/main/resources/data/thaumcraft/aspect_registry.json")).getAsJsonArray();
        var extra=json(Path.of("src/main/resources/data/thaumicomniverse/thaumcraft/aspects.json")).getAsJsonArray();
        base.addAll(ConditionalAspectDefinitions.filter(extra,id->loaded&&id.equals("avaritia")));
        return AspectDefinitions.decode(base);
    }
    @Test void avaritiaGainsTerminusOnlyWithItsTargetMod() throws Exception {
        var table = json(Path.of("src/main/resources/data/thaumicomniverse/item_aspects/avaritia.json"));
        for (boolean loaded : List.of(false, true)) {
            var catalog = catalog(loaded);
            var rules = ItemAspectJson.decodeKnown(table, catalog, id -> id.equals("avaritia:infinity_ingot"), mod -> mod.equals("avaritia"));
            var result = new ItemAspectResolver(rules, catalog).resolve("avaritia:infinity_ingot", tag -> false).orElseThrow();
            assertEquals(64, result.amount(catalog.require("alienis")));
            assertEquals(loaded, catalog.find("thaumicomniverse:terminus").isPresent());
            if (loaded) assertEquals(16, result.amount(catalog.require("thaumicomniverse:terminus")));
        }
    }
    @Test void shippedNativeNbtFamiliesRemainDistinct() throws Exception {
        var catalog=catalog(true);
        for(String id:List.of("avaritia:singularity","endless:singularity")) {
            String file="avaritia";
            var rules=ItemAspectJson.decodeKnown(json(Path.of("src/main/resources/data/thaumicomniverse/item_aspects/"+file+".json")),catalog,itemId->true,mod->true);
            var resolver=new ItemAspectResolver(rules,catalog);
            var variants=rules.stream().filter(r->r.items().contains(id)).toList();
            assertTrue(variants.size()>1);
            assertTrue(variants.stream().allMatch(r->!r.nbt().isEmpty()),id);
            var values=new HashSet<Map<?,?>>();
            for(var rule:variants) {
                var tag=rule.nbt().example();var value=resolver.resolve(id,t->false,tag).orElseThrow();
                assertEquals(rule.amounts(catalog).view(),value.view(),rule.nbt().canonical());
                values.add(value.view());
            }
            if(file.equals("avaritia")) assertTrue(values.size()>1,"Distinct legacy singularity values must remain distinct");
            assertTrue(resolver.resolve(id,t->false).isEmpty());
        }
    }
}
