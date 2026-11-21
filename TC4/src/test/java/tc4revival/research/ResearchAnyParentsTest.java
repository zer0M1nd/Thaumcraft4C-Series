package tc4revival.research;

import java.util.*;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.AspectDefinitions;
import static org.junit.jupiter.api.Assertions.*;

class ResearchAnyParentsTest {
    private ResearchDefinition entry(String key,List<String> parents,List<String> any) {
        return new ResearchDefinition(key,"BASICS",Map.of("aer",1),parents,List.of(),List.of(),Set.of(),1,0,
                new ResearchDefinition.Display(0,0,Optional.empty(),Optional.empty(),0),
                new ResearchDefinition.Triggers(Optional.empty(),Optional.empty(),Optional.empty()),any);
    }
    @Test void oneAlternativeAndEveryMandatoryParentRequiredOnBothSides() {
        var aspects=AspectDefinitions.bundled();
        var catalog=new ResearchCatalog(List.of(new ResearchCatalog.Category("BASICS","icon","background",List.of(
                entry("BASE",List.of(),List.of()),entry("AIR",List.of(),List.of()),entry("FIRE",List.of(),List.of()),
                entry("UPGRADE",List.of("BASE"),List.of("AIR","FIRE"))))),aspects);
        var player=new PlayerKnowledge(aspects);var rules=new ResearchRules(catalog);
        var browser=new ResearchBrowser(catalog,e->true);var actions=new ResearchBookActions(aspects,catalog,e->true);
        player.setPool(aspects.require("aer"),(short)10);
        assertFalse(rules.prerequisites(player,"UPGRADE"));
        player.rememberResearch("AIR");assertFalse(rules.prerequisites(player,"UPGRADE"));
        player.forgetResearch("AIR");player.rememberResearch("BASE");assertFalse(rules.prerequisites(player,"UPGRADE"));
        assertEquals(ResearchBookActions.Rejection.PREREQUISITES,actions.purchase(player,"UPGRADE",-1,false).rejection().orElseThrow());
        player.rememberResearch("FIRE");assertTrue(rules.prerequisites(player,"UPGRADE"));
        assertTrue(browser.view(player,"BASICS",-1).orElseThrow().nodes().stream().filter(n->n.research().key().equals("UPGRADE")).findFirst().orElseThrow().prerequisites());
        assertTrue(actions.purchase(player,"UPGRADE",-1,false).accepted());
        assertFalse(player.researchMarkers().contains("AIR"));
        var creative=actions.completeCreative(new PlayerKnowledge(aspects),"UPGRADE").knowledge();
        assertTrue(rules.prerequisites(creative,"UPGRADE"));
        assertEquals(1,List.of("AIR","FIRE").stream().filter(creative.researchMarkers()::contains).count());
    }
    @Test void jsonDefinitionsCarryAlternativesThroughSynchronizationAndValidateReferences() {
        var json=com.google.gson.JsonParser.parseString("""
            {"categories":[{"key":"BASICS","icon":"icon","background":"background","research":[
            {"key":"BASE","category":"BASICS","displayColumn":0,"displayRow":0,"pageCount":0},
            {"key":"UPGRADE","category":"BASICS","displayColumn":1,"displayRow":0,"pageCount":0,"parentsAny":["BASE"]}]}],"pages":[{"key":"BASE","pages":[]},{"key":"UPGRADE","pages":[]}],"recipes":[]}
            """).getAsJsonObject();
        assertEquals(List.of("BASE"),ResearchDefinitions.decode(json,AspectDefinitions.bundled()).catalog().require("UPGRADE").parentsAny());
        json.getAsJsonArray("categories").get(0).getAsJsonObject().getAsJsonArray("research").get(1).getAsJsonObject().getAsJsonArray("parentsAny").add("MISSING");
        assertThrows(IllegalArgumentException.class,()->ResearchDefinitions.decode(json,AspectDefinitions.bundled()));
    }
}
