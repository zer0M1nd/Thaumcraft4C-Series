package tc4revival.research;

import com.google.gson.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchAmendmentsTest {
    private JsonObject source() { return JsonParser.parseString("""
        {"categories":[{"research":[{"key":"A","pageCount":1}]}],
         "pages":[{"key":"A","pages":[{"id":"A/intro","type":"TEXT","page_break":true,"text":{"translate":"page.a"}}]}],
         "changes":[
          {"id":"addon:blue","research":"A","provider":"addon","provider_name":"Addon","kind":"integration","text":"Blue"},
          {"id":"core:green","research":"A","provider":"core","provider_name":"Core","kind":"continuation","text":"Green","conditions":[{}]},
          {"id":"absent:hidden","research":"A","provider":"absent","provider_name":"Absent","kind":"continuation","text":"Hidden"}],
         "replacements":[{"id":"core:fix","research":"A","provider":"core","provider_name":"Core","kind":"continuation",
           "keys":["page.a"],"translations":{"en_us":{"find":"8","replace":"280"}}}]}
        """).getAsJsonObject(); }
    private JsonArray pages(JsonObject source) {return source.getAsJsonArray("pages").get(0).getAsJsonObject().getAsJsonArray("pages");}
    @Test void orderingAndConditionsAreServerAuthoritativeAndDoNotMutateReloadSource() {
        var raw=source(); var before=raw.deepCopy();
        var on=ResearchAmendments.materialize(raw,m->!m.equals("absent"),e->true);
        assertEquals("change/core:green",pages(on).get(0).getAsJsonObject().get("id").getAsString());
        assertEquals("change/addon:blue",pages(on).get(1).getAsJsonObject().get("id").getAsString());
        assertTrue(pages(on).get(2).getAsJsonObject().has("text_replacements"));
        assertTrue(pages(on).get(2).getAsJsonObject().get("page_break").getAsBoolean(),"Client decides whether a visible prepend joins this page");
        assertEquals(3,on.getAsJsonArray("categories").get(0).getAsJsonObject().getAsJsonArray("research").get(0).getAsJsonObject().get("pageCount").getAsInt());
        assertEquals(before,raw);
        assertEquals(on,ResearchAmendments.materialize(raw,m->!m.equals("absent"),e->true));
        assertEquals(2,pages(ResearchAmendments.materialize(raw,m->!m.equals("absent"),e->false)).size());
        assertEquals(1,pages(ResearchAmendments.materialize(raw,m->false,e->true)).size());
    }
    @Test void tooltipAndClickPayloadsAreNeverReplacementTargets() {
        var keys=new JsonArray();keys.add("page.a");
        assertFalse(ResearchAmendments.hasKey(JsonParser.parseString("""
            {"text":"unchanged","hoverEvent":{"action":"show_text","contents":{"translate":"page.a"}}}
            """),keys));
        assertTrue(ResearchAmendments.hasKey(JsonParser.parseString("""
            {"localized":{"en_us":{"translate":"page.a"}}}
            """),keys));
    }
    @Test void malformedKindsAndDuplicateIdsCannotSilentlyLoad() {
        var raw=source();raw.getAsJsonArray("changes").add(raw.getAsJsonArray("changes").get(0).deepCopy());
        assertThrows(IllegalArgumentException.class,()->ResearchAmendments.materialize(raw,m->true,e->true));
        raw.getAsJsonArray("changes").get(0).getAsJsonObject().addProperty("kind","other");
        assertThrows(IllegalArgumentException.class,()->ResearchAmendments.materialize(raw,m->true,e->true));
    }
    @Test void localizedBranchesShareAnchorsButCannotCollideWithOtherComponents() {
        var node=JsonParser.parseString("""
            {"localized":{"en_us":{"text":"English","id":"shared"},"zh_cn":{"text":"中文","id":"shared"}}}
            """);
        var ids=new HashSet<String>(); BookComponentIds.text(ids,node,"root"); assertTrue(ids.contains("shared"));
        assertThrows(IllegalArgumentException.class,()->BookComponentIds.text(ids,node,"another"));
    }
}
