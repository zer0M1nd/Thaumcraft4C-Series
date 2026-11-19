package tc4revival.research;

import java.util.*;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.*;
import static org.junit.jupiter.api.Assertions.*;

class ResearchPreferencesTest {
    private final AspectCatalog catalog=new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    @Test void ordersUseDeepestParentAndPreferencesRoundTrip() {
        var cache=new HashMap<String,Integer>();
        for(var aspect:catalog.all()) {
            int expected=aspect.isPrimal()?0:1+aspect.components().stream().map(catalog::require).mapToInt(parent->ResearchPreferences.order(parent,catalog,cache)).max().orElseThrow();
            assertEquals(expected,ResearchPreferences.order(aspect,catalog,cache));
        }
        var knowledge=new PlayerKnowledge(catalog);knowledge.setPool(catalog.require("cognitio"),(short)37);knowledge.setPool(catalog.require("aer"),(short)19);
        var prefs=new ResearchPreferences(ResearchPreferences.Sort.QUANTITY,true,true);knowledge.researchPreferences(prefs);
        assertEquals("cognitio",prefs.sort(knowledge,catalog).get(0).tag());
        assertEquals(prefs,ResearchPreferences.unpack(prefs.packed()));assertEquals(prefs,knowledge.copy().researchPreferences());
        assertEquals(prefs,KnowledgeNbt.read(KnowledgeNbt.write(knowledge),catalog).knowledge().researchPreferences());
        var sorted=new ResearchPreferences(ResearchPreferences.Sort.ORDER,false,false).sort(knowledge,catalog);
        assertTrue(sorted.subList(0,6).stream().allMatch(Aspect::isPrimal));
    }
}
