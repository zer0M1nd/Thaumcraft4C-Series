package tc4revival.client;

import com.google.gson.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TableBodyModelTest {
    @Test void bakedRigidBodiesKeepLegacyDimensionsAndCoverAllStates() throws Exception {
        var dir=Path.of("src/main/resources/assets/thaumcraft/models/block");
        for(String name:new String[]{"single","master"}){
            var model=JsonParser.parseString(Files.readString(dir.resolve("table_"+name+".json"))).getAsJsonObject();
            var elements=model.getAsJsonArray("elements");assertEquals(name.equals("single")?4:6,elements.size());
            var top=elements.get(0).getAsJsonObject();
            assertEquals(name.equals("single")?16:32,top.getAsJsonArray("to").get(0).getAsInt());
            assertEquals(12,top.getAsJsonArray("from").get(1).getAsInt());
            for(var element:elements){var faces=element.getAsJsonObject().getAsJsonObject("faces");assertEquals(6,faces.size());
                for(var face:faces.entrySet()){assertFalse(face.getValue().getAsJsonObject().has("cullface"),"Thin table must never cull against neighbouring walls");
                    for(var uv:face.getValue().getAsJsonObject().getAsJsonArray("uv"))assertTrue(uv.getAsDouble()>=0&&uv.getAsDouble()<=16);}}
        }
        var states=JsonParser.parseString(Files.readString(Path.of("src/generated/resources/assets/thaumcraft/blockstates/table.json"))).getAsJsonObject().getAsJsonObject("variants");
        assertEquals(12,states.size());
        var sources=JsonParser.parseString(Files.readString(Path.of("src/generated/resources/assets/minecraft/atlases/blocks.json"))).getAsJsonObject().getAsJsonArray("sources");
        var sprites=new java.util.HashSet<String>();for(var source:sources)sprites.add(source.getAsJsonObject().get("resource").getAsString());
        assertTrue(sprites.containsAll(java.util.Set.of("thaumcraft:models/table","thaumcraft:models/restable")),"Legacy model sheets must be explicitly stitched for baked bodies");
        for(var entry:states.entrySet()){
            String model=entry.getValue().getAsJsonObject().get("model").getAsString();
            assertEquals("thaumcraft:block/table"+(entry.getKey().contains("part=extension")?"":entry.getKey().contains("part=master")?"_master":"_single"),model);
        }
    }
}
