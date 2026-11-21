package tc4revival.content;

import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import tc4revival.research.ResearchPage;
import tc4revival.aspect.TCAspects;
import static org.junit.jupiter.api.Assertions.*;

class RemainingAddonDefinitionsTest {
    @Test void customResearchPageKeepsNamespacedTypeAndDefensivePayload() {
        var source = JsonParser.parseString("{\"type\":\"horizons:vat\",\"data\":{\"layers\":3}}").getAsJsonObject();
        source.addProperty("id","test:vat");
        var page = ResearchPage.decode(source);
        assertEquals(ResearchPage.Type.CUSTOM, page.type());
        var custom = (ResearchPage.Custom)page.recipe();
        assertEquals("horizons:vat", custom.id().toString());
        source.getAsJsonObject("data").addProperty("layers", 9);
        custom.data().addProperty("layers", 5);
        assertEquals(3, custom.data().get("layers").getAsInt());
    }
    @Test void literalTextAndCustomDataMayBeEmpty() {
        assertEquals(ResearchPage.Type.TEXT, ResearchPage.decode(JsonParser.parseString("{\"id\":\"test:text\",\"type\":\"TEXT\",\"text\":\"a\"}").getAsJsonObject()).type());
        var page=ResearchPage.decode(JsonParser.parseString("{\"id\":\"test:vat\",\"type\":\"horizons:vat\"}").getAsJsonObject());
        assertTrue(((ResearchPage.Custom)page.recipe()).data().entrySet().isEmpty());
    }
    @Test void disposableRodJsonDoesNotRegenerateAndDefaultsRemainRechargeable() {
        var json = JsonParser.parseString("{\"tag\":\"test:crystal\",\"texture\":\"test:rod.png\",\"capacity\":250,\"rechargeable\":false,\"regenerateAll\":true}").getAsJsonObject();
        var rod = WandParts.Rod.decode(json);
        assertFalse(rod.rechargeable);
        var tag = new CompoundTag();
        assertTrue(WandRegeneration.tick(tag, 200, new tc4revival.aspect.AspectCatalog(java.util.Arrays.stream(tc4revival.aspect.LegacyAspect.values()).map(tc4revival.aspect.LegacyAspect::create).toList()), bound -> 0, rod, 25000).isEmpty());
        assertTrue(tag.isEmpty());
        json.remove("rechargeable"); assertTrue(WandParts.Rod.decode(json).rechargeable);
    }
}
