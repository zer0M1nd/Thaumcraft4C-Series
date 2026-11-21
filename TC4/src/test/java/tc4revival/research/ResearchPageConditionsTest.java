package tc4revival.research;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchPageConditionsTest {
    @Test void serverConditionsPreserveUnconditionalFailuresAndRestoreOnReload() {
        var raw=JsonParser.parseString("""
                {"categories":[{"research":[{"key":"test:storage","pageCount":3}]}],
                 "pages":[{"key":"test:storage","pages":[
                   {"type":"TEXT","text":"intro"},
                   {"type":"ARCANE_CRAFTING","conditions":[{"type":"test:enabled"}]},
                   {"type":"ARCANE_CRAFTING","recipe":{"kind":"reference","keys":["missing"]}}
                 ]}]}
                """).getAsJsonObject();
        var off=ResearchPageConditions.materialize(raw,page->false);
        var offPages=off.getAsJsonArray("pages").get(0).getAsJsonObject().getAsJsonArray("pages");
        assertEquals(2,offPages.size());
        assertTrue(offPages.get(1).getAsJsonObject().has("recipe"),"An unresolved unconditional recipe stays visible for diagnosis");
        assertEquals(2,off.getAsJsonArray("categories").get(0).getAsJsonObject().getAsJsonArray("research").get(0).getAsJsonObject().get("pageCount").getAsInt());
        var on=ResearchPageConditions.materialize(raw,page->true);
        var onPages=on.getAsJsonArray("pages").get(0).getAsJsonObject().getAsJsonArray("pages");
        assertEquals(3,onPages.size());
        assertFalse(onPages.get(1).getAsJsonObject().has("conditions"),"Client uses the server decision");
        assertTrue(raw.getAsJsonArray("pages").get(0).getAsJsonObject().getAsJsonArray("pages").get(1).getAsJsonObject().has("conditions"),"Raw data survives for a later config reload");
    }
}
