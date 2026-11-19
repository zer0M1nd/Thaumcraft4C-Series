package tc4revival.aspect.editor;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import tc4revival.aspect.*;
import tc4revival.aspect.item.*;
import tc4revival.aspect.sync.*;
class AdvancedAspectSourcesTest {
    private final AspectCatalog catalog=new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    private String item(String id,boolean generated) {return ItemAspectJson.encode(List.of(new ItemAspectRule(List.of(id),null,List.of(new ItemAspectRule.Quantity("aer",3)),generated))).toString();}
    @Test void provenanceUsesResourceIdentityAndAuthoredRulesHavePriority(){
        var files=new HashMap<String,String>();
        files.put("item_aspects/thaumcraft:advanced_inference",item("test:advanced",false));
        files.put("item_aspects/test:recipe",item("test:recipe",true));
        var source=new AdvancedAspectSources(new AspectDataSet(files),catalog);
        assertTrue(source.item("test:advanced",tag->false,null));
        assertFalse(source.item("test:recipe",tag->false,null));
        assertFalse(source.item("test:unknown",tag->false,null));
        files.put("item_aspects/test:authored",item("test:advanced",false));
        assertFalse(new AdvancedAspectSources(new AspectDataSet(files),catalog).item("test:advanced",tag->false,null));
    }
    @Test void explicitEmptyAndTaggedDefinitionsAreStillAuthored(){
        var direct=new ItemAspectRule(List.of("test:advanced"),null,List.of());
        var tagged=new ItemAspectRule(List.of("test:advanced"),"test:items",List.of());
        for(var rule:List.of(direct,tagged)){
            var source=new AdvancedAspectSources(new AspectDataSet(Map.of("item_aspects/thaumcraft:advanced_inference",item("test:advanced",false),"item_aspects/test:authored",ItemAspectJson.encode(List.of(rule)).toString())),catalog);
            assertFalse(source.item("test:advanced",tag->true,null));
        }
    }
    @Test void blockOriginHonorsStateSelectorsAndReload(){
        String inferred="{\"format\":1,\"entries\":[{\"block\":\"test:block\",\"properties\":{\"lit\":\"true\"},\"aspects\":{\"aer\":3}}]}";
        var source=new AdvancedAspectSources(new AspectDataSet(Map.of("block_aspects/thaumcraft:advanced_inference",inferred)),catalog);
        assertTrue(source.block("test:block",Map.of("lit","true")));
        assertFalse(source.block("test:block",Map.of("lit","false")));
        assertFalse(new AdvancedAspectSources(new AspectDataSet(Map.of()),catalog).block("test:block",Map.of("lit","true")));
    }
    @Test void entityOriginAndSolidifiedItemDefinitionsSurviveSaving(){
        String entities="{\"format\":1,\"entries\":[{\"entity\":\"test:mob\",\"legacy_name\":\"test:mob\",\"variant\":\"any\",\"legacy_qualifier\":\"\",\"aspects\":[{\"aspect\":\"aer\",\"amount\":3}]}]}";
        var source=new AdvancedAspectSources(new AspectDataSet(Map.of("entity_aspects/thaumcraft:advanced_inference",entities)),catalog);
        assertTrue(source.entity("test:mob"));assertFalse(source.entity("test:other"));
        assertFalse(new AdvancedAspectSources(new AspectDataSet(Map.of("entity_aspects/thaumcraft:advanced_inference",entities,"entity_aspects/test:authored",entities)),catalog).entity("test:mob"));
        var saved=EditorDefinitions.merge(List.of(new EditorDefinitions(Map.of("item:test:advanced",Map.of("aer",3)),12).encode()));
        assertEquals(Map.of("aer",3),saved.get("item:test:advanced").orElseThrow());
        var resolver=new EditorItemResolver(new ItemAspectResolver(List.of(),catalog),saved,catalog,(id,tag)->false);
        assertEquals(3,resolver.resolve("test:advanced",tag->false).orElseThrow().amount(catalog.require("aer")));
    }
}
