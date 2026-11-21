package thmodern.golem;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GolemDefinitionsTest {
    private JsonArray read(String name)throws IOException{
        try(var input=getClass().getResourceAsStream("/data/thaumichorizons/"+name+".json")){
            assertNotNull(input);return JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonArray();
        }
    }
    @Test void allLegacyProfilesHaveStableNamesAndCorrectCharacteristicValues()throws IOException{
        var profiles=new LinkedHashMap<String,JsonObject>();for(var entry:read("golem_profiles")){var value=entry.getAsJsonObject();assertNull(profiles.put(value.get("id").getAsString(),value));}
        assertEquals(Set.of("grass","dirt","wood","plant","rock","metal","cloth","sand","redstone","tnt","ice","cactus","clay","cake","web","void"),profiles.keySet());
        assertEquals(1250,profiles.get("metal").get("vis_cost").getAsInt());assertEquals(35,profiles.get("metal").get("health").getAsInt());
        assertEquals(25,profiles.get("cake").get("regeneration_delay").getAsInt());assertTrue(profiles.get("rock").get("fire_resistant").getAsBoolean());
        assertEquals(0,profiles.get("void").get("vis_cost").getAsInt());
    }
    @Test void upgradesUseNamedKeysAndAspectListsInsteadOfReservedIds()throws IOException{
        var all=new LinkedHashMap<String,JsonObject>();
        for(var entry:read("thaumcraft/focus_upgrades")){var row=entry.getAsJsonObject();assertFalse(row.has("id"));assertNull(all.put(row.get("key").getAsString(),row));}
        assertTrue(all.keySet().containsAll(Set.of("thaumichorizons:berserk","thaumichorizons:detonation")));
        for(var entry:Map.of("berserk",3,"detonation",5).entrySet()){
            var row=all.get("thaumichorizons:"+entry.getKey());
            assertEquals(List.of("telum"),row.getAsJsonArray("aspects").asList().stream().map(JsonElement::getAsString).toList());
            assertEquals(List.of(entry.getValue()),row.getAsJsonArray("ranks").asList().stream().map(JsonElement::getAsInt).toList());
            assertEquals(List.of("thaumichorizons:focus_animation"),row.getAsJsonArray("foci").asList().stream().map(JsonElement::getAsString).toList());
        }
    }
}
