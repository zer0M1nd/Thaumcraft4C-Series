package tc4revival.aspect.item;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MaterializedDefinitionContractTest {
    @Test void lustContributionsRemainConditionalRatherThanLeakingIntoTheDefaultTable() throws Exception {
        Path root=Path.of("../Addons/ForbiddenMagic/src/main/resources");
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.isDirectory(root));
        var base=ItemAspectJson.decode(JsonParser.parseString(Files.readString(root.resolve("data/forbidden_magic/item_aspects/explicit.json"))));
        assertTrue(base.stream().flatMap(r->r.aspects().stream()).noneMatch(q->q.aspect().equals("forbidden_magic:luxuria")));
        var conditional=ItemAspectJson.decode(JsonParser.parseString(Files.readString(root.resolve("optional_packs/lust/data/forbidden_magic/item_aspects/zzzz_lust.json"))));
        assertEquals(Set.of("forbidden_magic:lust_shard","forbidden_magic:riding_crop","minecraft:lead","minecraft:saddle"),conditional.stream().flatMap(r->r.items().stream()).collect(java.util.stream.Collectors.toSet()));
        assertTrue(conditional.stream().allMatch(r->r.aspects().size()==1&&r.aspects().get(0).aspect().equals("forbidden_magic:luxuria")));
    }
    @Test void bundledDataNeverRestoresGeneratedOrBonusDefinitions() throws Exception {
        var projects=new ArrayList<Path>();projects.add(Path.of("."));
        for(String addon:List.of("ThaumicTinkerer","ForbiddenMagic","ThaumicEnergistics","ThaumicHorizons","ThaumicBases","Automagy")) {
            Path p=Path.of("../Addons",addon);if(Files.isDirectory(p))projects.add(p);
        }
        int tables=0,entries=0;
        for(var project:projects)for(String source:List.of("main","generated")){
            Path root=project.resolve("src/"+source+"/resources");if(!Files.isDirectory(root))continue;
            try(var files=Files.walk(root)){
                for(var file:files.filter(p->p.toString().endsWith(".json")&&List.of("item_aspects","item_bonus_aspects").contains(p.getParent().getFileName().toString())).toList()){
                    var json=JsonParser.parseString(Files.readString(file)).getAsJsonObject();tables++;
                    if(file.getParent().getFileName().toString().equals("item_bonus_aspects"))assertTrue(json.getAsJsonArray("entries").isEmpty(),file.toString());
                    assertFalse(json.has("generated")&&json.get("generated").getAsBoolean(),file.toString());
                    for(var rule:ItemAspectJson.decode(json)){assertFalse(rule.generated(),file.toString());entries++;}
                }
            }
        }
        assertTrue(tables>4);assertTrue(entries>1500);
    }
}
