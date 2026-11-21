package thaumicomniverse.alignment;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.AspectDefinitions;
import tc4revival.research.ResearchDefinitions;
import static org.junit.jupiter.api.Assertions.*;
class AlignmentResearchDataTest {
    private JsonObject read(String path) throws Exception {
        try(var in=getClass().getResourceAsStream(path)) { assertNotNull(in,path);return JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject(); }
    }
    @Test void mergedResearchPassesTheActualServerDecoder() throws Exception {
        var combined=new JsonObject();
        for(var section:new String[][]{{"research_catalog","categories","categories"},{"research_pages","entries","pages"},{"research_page_recipes","entries","recipes"}}) {
            var entries=read("/data/thaumcraft/"+section[0]+"/legacy.json").getAsJsonArray(section[1]).deepCopy();
            // Addons now reference the core tutorial catalog, alongside its legacy research.
            try(var paths=Files.list(Path.of("../../TC4/src/main/resources/data/thaumcraft",section[0]))) {
                for(var path:paths.filter(p->p.toString().endsWith(".json")).sorted().toList())
                    entries.addAll(JsonParser.parseString(Files.readString(path)).getAsJsonObject().getAsJsonArray(section[1]));
            }
            try(var paths=Files.list(Path.of("src/main/resources/data/thaumicomniverse",section[0]))) {
                for(var path:paths.filter(p->p.toString().endsWith(".json")).sorted().toList()) {
                    entries.addAll(JsonParser.parseString(Files.readString(path)).getAsJsonObject().getAsJsonArray(section[1]));
                }
            }
            try(var paths=Files.list(Path.of("../ThaumicFutures/src/main/resources/data/thaumicfutures",section[0]))) {
                for(var path:paths.filter(p->p.toString().endsWith(".json")).sorted().toList())
                    entries.addAll(JsonParser.parseString(Files.readString(path)).getAsJsonObject().getAsJsonArray(section[1]));
            }
            combined.add(section[2],entries);
        }
        JsonArray aspects;
        try(var in=getClass().getResourceAsStream("/data/thaumcraft/aspect_registry.json")) {
            aspects=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonArray();
        }
        aspects.addAll(JsonParser.parseString(Files.readString(Path.of("../ThaumicFutures/src/main/resources/data/thaumicfutures/thaumcraft/aspects.json"))).getAsJsonArray());
        aspects.addAll(JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/thaumicomniverse/thaumcraft/aspects.json"))).getAsJsonArray());
        tc4revival.research.ResearchPageAdditions.recount(combined);
        var snapshot=ResearchDefinitions.decode(combined,AspectDefinitions.decode(aspects));
        assertEquals(8,snapshot.pages().require("thaumicomniverse:alignment").size());
        assertNotNull(snapshot.catalog().require("thaumicomniverse:alignment"));
    }
}
