package dyesmodern;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import tc4revival.research.*;
import static org.junit.jupiter.api.Assertions.*;

class DyesDataContractTest {
    private static final Path ROOT=Path.of("src/main/resources/data/thaumicdyes");
    private static JsonObject read(Path path)throws Exception{return JsonParser.parseString(Files.readString(path)).getAsJsonObject();}
    private static Map<String,JsonObject> recipes()throws Exception{
        var result=new LinkedHashMap<String,JsonObject>();try(var files=Files.list(ROOT.resolve("recipes"))){for(var path:files.filter(p->p.toString().endsWith(".json")).sorted().toList())result.put(path.getFileName().toString().replace(".json",""),read(path));}return result;
    }
    private static ResearchCatalog catalog()throws Exception{
        var categories=read(Path.of("../../TC4/src/generated/resources/data/thaumcraft/research_catalog/legacy.json")).getAsJsonArray("categories").deepCopy();
        categories.addAll(read(ROOT.resolve("research_catalog/main.json")).getAsJsonArray("categories"));
        var aspects=new tc4revival.aspect.AspectCatalog(Arrays.stream(tc4revival.aspect.LegacyAspect.values()).map(tc4revival.aspect.LegacyAspect::create).toList());
        return ResearchCatalog.decode(categories,aspects);
    }
    @Test void realCoreSchemaDecodesEveryPageAndMirrorWithoutDanglingPrerequisites()throws Exception{
        var catalog=catalog();var decoded=ResearchPages.decode(read(ROOT.resolve("research_pages/main.json")).getAsJsonArray("entries"));
        var owned=catalog.all().stream().filter(r->r.key().startsWith("thaumicdyes:")).toList();assertEquals(37,owned.size());assertEquals(5,owned.stream().filter(ResearchDefinition::mirror).count());assertEquals(32,decoded.entries().size());assertEquals(134,decoded.entries().values().stream().mapToInt(List::size).sum());
        for(var row:owned){
            if(row.mirror()){assertFalse(catalog.original(row.key()).mirror());assertFalse(decoded.entries().containsKey(row.key()));}
            else assertEquals(row.display().pageCount(),decoded.require(row.key()).size(),row.key());
            for(var parent:row.parents())assertTrue(catalog.find(parent).isPresent(),row.key()+" -> "+parent);
            for(var parent:row.hiddenParents())assertTrue(catalog.find(parent).isPresent(),row.key()+" -> "+parent);
        }
        var recipes=recipes();for(var pages:decoded.entries().values())for(var page:pages)if(page.recipe() instanceof ResearchPage.Reference ref)for(String id:ref.keys())assertTrue(recipes.containsKey(id.substring(id.indexOf(':')+1)),id);
    }
    @Test void finalOriginalResearchSetWarpAndDiscoveryTriggersAreRetained()throws Exception{
        var source=Files.readString(Path.of("../../../References/Projects/ThaumicDyes/decompiled/thaumicdyes/common/ConfigResearch.java"));var matcher=Pattern.compile("new (?:Fake)?ResearchItem\\(\"([^\"]+)\"").matcher(source);var expected=new HashSet<String>();while(matcher.find())expected.add("thaumicdyes:"+matcher.group(1));
        var catalog=catalog();var actual=new HashSet<String>();catalog.all().stream().filter(r->r.key().startsWith("thaumicdyes:")).forEach(r->actual.add(r.key()));assertEquals(expected,actual);
        assertEquals(1,catalog.require("thaumicdyes:TD.CRIMSON").warp());assertEquals(2,catalog.require("thaumicdyes:TD.INHABITEDPLATE").warp());assertEquals(4,catalog.require("thaumicdyes:TD.PRIMALRUNICARMOR").warp());
        assertEquals(List.of("thaumcraft:cultist_cleric"),catalog.require("thaumicdyes:TD.CRIMSONCLERIC").triggers().entities().orElseThrow());assertEquals(List.of("thaumcraft:cultist_knight"),catalog.require("thaumicdyes:TD.CRIMSONKNIGHT").triggers().entities().orElseThrow());
        assertEquals(List.of("thaumicdyes:TD.CRIMSON"),catalog.require("thaumicdyes:TD.CRIMSONBANNER").parents());
    }
    @Test void everyRegisteredItemHasAFixedSurvivalOutputAndAllTenUpgradesCoverTheirOriginalSlots()throws Exception{
        var rows=recipes();assertEquals(209,rows.size());var counts=new HashMap<String,Integer>();var outputs=new HashSet<String>();var upgrades=new HashMap<String,Integer>();
        for(var row:rows.values()){
            counts.merge(row.get("type").getAsString(),1,Integer::sum);
            if(!row.has("result"))continue;var out=row.getAsJsonObject("result");if(out.has("item"))outputs.add(out.get("item").getAsString());
            if(out.has("patch")&&out.get("patch").getAsString().startsWith("{upgrade:"))upgrades.merge(out.get("patch").getAsString(),1,Integer::sum);
        }
        assertEquals(Map.of("thaumcraft:arcane",19,"thaumcraft:infusion",186,"thaumcraft:crucible",3,"thaumicdyes:dyeing",1),counts);
        var registered=Pattern.compile("ITEMS\\.register\\(\"([^\"]+)\"").matcher(Files.readString(Path.of("src/main/java/dyesmodern/DyesItems.java")));int itemCount=0;
        while(registered.find()){itemCount++;assertTrue(outputs.contains("thaumicdyes:"+registered.group(1)),registered.group(1));}assertEquals(57,itemCount);
        assertEquals(10,upgrades.size());for(int i=1;i<=10;i++)assertEquals(i==9?4:14,upgrades.get("{upgrade:"+i+"b}"));
    }
    @Test void flattenedPotionIngredientsRemainSpecificAndBannerRetainsItsCrimsonPattern()throws Exception{
        var potions=new HashSet<String>();for(var row:recipes().values())if(row.has("components"))for(var component:row.getAsJsonArray("components")){
            var ingredient=component.getAsJsonObject().getAsJsonObject("ingredient");if(ingredient.has("item")&&ingredient.get("item").getAsString().endsWith("potion")){
                assertEquals("forge:partial_nbt",ingredient.get("type").getAsString());potions.add(ingredient.get("item").getAsString()+" "+ingredient.get("nbt").getAsString());
            }
        }
        assertEquals(Set.of("minecraft:splash_potion {Potion:\"minecraft:strong_harming\"}","minecraft:potion {Potion:\"minecraft:strong_strength\"}","minecraft:potion {Potion:\"minecraft:long_regeneration\"}"),potions);
        var banner=recipes().get("crimson_banner");assertEquals("minecraft:red_banner",banner.getAsJsonObject("result").get("item").getAsString());assertTrue(banner.getAsJsonObject("result").get("nbt").getAsString().contains("Pattern:\"tc_crimson\",Color:4"));
        assertEquals(5,banner.getAsJsonObject("vis").get("ordo").getAsInt());
    }
    @Test void explicitOriginalItemAndCultistLeaderAspectsUseTheCoreDataSchema()throws Exception{
        var rules=tc4revival.aspect.item.ItemAspectJson.decode(read(ROOT.resolve("item_aspects/original.json")));assertEquals(34,rules.size());
        var items=new HashSet<String>();for(var rule:rules){assertEquals(1,rule.items().size());assertTrue(items.add(rule.items().get(0)));assertFalse(rule.generated());}
        var leader=tc4revival.aspect.entity.EntityAspectJson.decode(read(ROOT.resolve("entity_aspects/cultist_leader.json")));assertEquals(1,leader.size());assertEquals("thaumcraft:cultist_leader",leader.get(0).entity());assertEquals("Thaumcraft.CultistLeader",leader.get(0).legacyName());
        var actual=new HashMap<String,Integer>();leader.get(0).aspects().forEach(a->actual.put(a.aspect(),a.amount()));assertEquals(Map.of("alienis",2,"humanus",2,"perditio",1,"praecantatio",2),actual);
    }
}
