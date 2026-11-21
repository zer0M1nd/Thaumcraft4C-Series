package conciliummodern;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Checks the external extraction contract against the pinned original archive, without copying art. */
class OriginalResourceContractTest {
    @Test void everyDeclaredOriginalPathExistsAndModernPathsAreValid()throws Exception{
        var file=Path.of("src/main/resources/data/thaumicconcilium/thaumcraft/original_resources.json");
        var data=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        try(var zip=new ZipFile(Path.of("../../../References/Projects/ThaumicConcilium/ThaumicConcilium-1.1.1.jar").toFile())){
            for(var entry:data.getAsJsonObject("mapping").entrySet()){
                assertTrue(entry.getKey().matches("[a-z0-9_./-]+"),entry.getKey());
                assertNotNull(zip.getEntry(entry.getValue().getAsString()),entry.getKey()+" -> "+entry.getValue());
            }
        }
    }
    @Test void recipesKeepTheOriginalPontifexChainAndPageCount()throws Exception{
        var hammer=read("recipes/pontifex_hammer.json");assertEquals(7,hammer.get("instability").getAsInt());assertEquals(10,hammer.getAsJsonArray("components").size());
        assertEquals("thaumcraft:crimson_sword",hammer.getAsJsonObject("center").getAsJsonObject("ingredient").get("item").getAsString());
        long charms=hammer.getAsJsonArray("components").asList().stream().filter(e->e.getAsJsonObject().getAsJsonObject("ingredient").get("item").getAsString().equals("thaumcraft:primal_charm")).count();assertEquals(2,charms,"Original itemResource metadata 15 is the primal charm");
        for(String part:List.of("helmet","chestplate","leggings","boots")){
            var robe=read("recipes/pontifex_"+part+".json");assertEquals("thaumicconcilium:chained_rift",robe.get("type").getAsString());assertEquals("thaumicconcilium:PONTIFEXROBE",robe.get("research").getAsString());
            var costs=robe.getAsJsonObject("aspects");assertEquals(128,costs.get("victus").getAsInt());assertEquals(128,costs.get("vacuos").getAsInt());assertEquals(256,costs.get("tutamen").getAsInt());assertEquals(64,costs.get("permutatio").getAsInt());assertEquals(256,costs.get("corpus").getAsInt());
        }
    }
    private static JsonObject read(String path)throws Exception{return JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/thaumicconcilium/"+path))).getAsJsonObject();}
}
