package thaumcraft.api.data;

import com.google.gson.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StartupDefinitionsTest {
    @Test void conditionsAliasesAndRawMetadataComposeBeforeRegistration(){
        var rows=JsonParser.parseString("""
            [{"tag":"old:time","components":[],"color":1},
             {"tag":"new:time","components":[],"color":2},
             {"tag":"new:cycle","components":["old:time","new:cycle"],"required_any_mods":["present","absent"],"color":3},
             {"tag":"new:disabled","components":[],"required_mods":["absent"]}]
            """).getAsJsonArray();
        var result=StartupDefinitions.normalize("aspects",rows,"present"::equals,
            new StartupDefinitions.Rules(true,Map.of("old:time","new:time"),Map.of()));
        assertEquals(2,result.size());
        assertEquals("new:time",result.get(1).getAsJsonObject().getAsJsonArray("components").get(0).getAsString());
        assertEquals(3,result.get(1).getAsJsonObject().get("color").getAsInt());
        assertEquals(4,rows.size());
    }
    @Test void aliasCyclesAndMissingTargetsAreRejected(){
        assertThrows(IllegalArgumentException.class,()->new StartupDefinitions.Rules(true,Map.of("a","b","b","a"),Map.of()));
        assertThrows(IllegalArgumentException.class,()->StartupDefinitions.normalize("aspects",new JsonArray(),id->true,
            new StartupDefinitions.Rules(true,Map.of("a","b"),Map.of())));
    }
}
