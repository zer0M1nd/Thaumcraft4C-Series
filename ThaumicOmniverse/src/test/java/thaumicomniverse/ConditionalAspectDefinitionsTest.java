package thaumicomniverse;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConditionalAspectDefinitionsTest {
    @Test void eitherModernAvaritiaEnablesTerminus() {
        var source=JsonParser.parseString("[{\"tag\":\"thaumicomniverse:terminus\",\"required_any_mods\":[\"avaritia\",\"endless\"]}]").getAsJsonArray();
        assertEquals(0,ConditionalAspectDefinitions.filter(source,id->false).size());
        for(String target:java.util.List.of("avaritia","endless"))
            assertEquals(1,ConditionalAspectDefinitions.filter(source,id->id.equals(target)).size());
        assertEquals(1,ConditionalAspectDefinitions.filter(source,id->true).size());
    }
    @Test void typesFollowTargetModsAndLeaveOtherAddonsUntouched() {
        var source = JsonParser.parseString("""
            [{"tag":"aer"},
             {"tag":"thaumicomniverse:terminus","required_mods":["avaritia"]},
             {"tag":"thaumicomniverse:radio","required_mods":["gtceu"]},
             {"tag":"other:custom","required_mods":["missing"]}]
            """).getAsJsonArray();
        var none = ConditionalAspectDefinitions.filter(source, id -> false);
        assertEquals(2, none.size());
        var avaritia = ConditionalAspectDefinitions.filter(source, id -> id.equals("avaritia"));
        assertEquals(3, avaritia.size());
        assertEquals("thaumicomniverse:terminus", avaritia.get(1).getAsJsonObject().get("tag").getAsString());
        assertEquals(4, source.size());
        assertEquals(4, ConditionalAspectDefinitions.filter(source, id -> true).size());
    }
}
