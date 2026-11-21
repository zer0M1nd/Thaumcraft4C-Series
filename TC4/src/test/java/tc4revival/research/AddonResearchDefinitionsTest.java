package tc4revival.research;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.AspectDefinitions;
import static org.junit.jupiter.api.Assertions.*;

class AddonResearchDefinitionsTest {
    private JsonObject definition() {
        return JsonParser.parseString("""
            {"categories":[{"key":"example:category","icon":"example:icon.png","background":"example:book.png",
              "research":[{"key":"example:start","category":"example:category","displayColumn":0,"displayRow":0,
              "pageCount":1,"isAutoUnlock":true}]}],
              "pages":[{"key":"example:start","pages":[{"id":"example:start/intro","type":"TEXT","text":{"translate":"example.page.start"}}]}],"recipes":[]}
            """).getAsJsonObject();
    }
    @Test void addonResearchUsesSparseJsonAndNamespacedKeys() {
        var snapshot=ResearchDefinitions.decode(definition(),AspectDefinitions.bundled());
        assertTrue(snapshot.catalog().require("example:start").has(ResearchDefinition.Flag.AUTO_UNLOCK));
        assertEquals("example.page.start",snapshot.pages().require("example:start").get(0).text().orElseThrow());
    }
    @Test void rejectPageMismatchAndUnknownParentsBeforePublishing() {
        var json=definition();json.getAsJsonArray("pages").get(0).getAsJsonObject().getAsJsonArray("pages").add(new JsonObject());
        assertThrows(RuntimeException.class,()->ResearchDefinitions.decode(json,AspectDefinitions.bundled()));
        var invalid=definition();var parents=new JsonArray();parents.add("missing:research");
        invalid.getAsJsonArray("categories").get(0).getAsJsonObject().getAsJsonArray("research").get(0).getAsJsonObject().add("parents",parents);
        assertThrows(IllegalArgumentException.class,()->ResearchDefinitions.decode(invalid,AspectDefinitions.bundled()));
    }
    @Test void mergeCategoryContributionsButRejectConflictingArtworkAndDuplicateResearch() {
        var json=definition();var extension=json.getAsJsonArray("categories").get(0).getAsJsonObject().deepCopy();
        extension.add("research",new JsonArray());json.getAsJsonArray("categories").add(extension);
        assertEquals(1,ResearchDefinitions.decode(json,AspectDefinitions.bundled()).catalog().categories().size());
        extension.addProperty("icon","example:other.png");
        assertThrows(IllegalArgumentException.class,()->ResearchDefinitions.decode(json,AspectDefinitions.bundled()));
        var duplicate=definition();duplicate.getAsJsonArray("categories").add(duplicate.getAsJsonArray("categories").get(0).deepCopy());
        assertThrows(IllegalArgumentException.class,()->ResearchDefinitions.decode(duplicate,AspectDefinitions.bundled()));
    }
}
