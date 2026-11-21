package conciliummodern;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GemResourceContractTest {
    private static JsonObject read(String path)throws Exception{return JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/thaumicconcilium/"+path+".json"))).getAsJsonObject();}
    @Test void originalSixteenComponentInfusionsAndAwakeningsRemainGatedTogether()throws Exception{
        for(String gem:List.of("rift_gem","terra_cast_gem")){
            var inert=read("recipes/"+gem+"_inert");assertEquals(10,inert.get("instability").getAsInt());assertEquals(16,inert.getAsJsonArray("components").size());
            for(String path:List.of(gem,gem+"_inert"))assertTrue(read("recipes/"+path).getAsJsonArray("conditions").asList().stream().anyMatch(e->e.getAsJsonObject().get("modid").getAsString().equals("gadomancy")));
            var active=read("recipes/"+gem);assertEquals("thaumicconcilium:"+gem+"_inert",active.getAsJsonObject("catalyst").get("item").getAsString());assertEquals("thaumicconcilium:"+gem,active.getAsJsonObject("result").get("item").getAsString());
            var costs=active.getAsJsonObject("aspects");assertEquals(5,costs.size());assertEquals(64,costs.get("auram").getAsInt());
            for(var entry:costs.entrySet())if(!entry.getKey().equals("auram"))assertEquals(128,entry.getValue().getAsInt());
        }
    }
    @Test void causalBouillonUsesOriginalAirFloorAndNoGeneratedStructures()throws Exception{
        var settings=read("dimension/causal_bouillon").getAsJsonObject("generator").getAsJsonObject("settings");var layers=settings.getAsJsonArray("layers");
        assertEquals(2,layers.size());assertEquals("minecraft:air",layers.get(0).getAsJsonObject().get("block").getAsString());assertEquals(1,layers.get(0).getAsJsonObject().get("height").getAsInt());
        assertEquals("thaumicconcilium:void_reality",layers.get(1).getAsJsonObject().get("block").getAsString());assertEquals(3,layers.get(1).getAsJsonObject().get("height").getAsInt());
        assertTrue(settings.getAsJsonArray("structure_overrides").isEmpty());assertFalse(settings.get("features").getAsBoolean());assertFalse(settings.get("lakes").getAsBoolean());
        var dimension=read("dimension_type/causal_bouillon");assertEquals(0,dimension.get("min_y").getAsInt());assertEquals(1,dimension.get("coordinate_scale").getAsInt());assertEquals(6000,dimension.get("fixed_time").getAsInt());assertFalse(dimension.get("bed_works").getAsBoolean());
    }
}
