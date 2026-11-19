package tc4revival.research;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import tc4revival.config.TCConfig;
import thaumcraft.api.config.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class RevivalResearchGraphTest {
    private JsonObject data() throws Exception {
        var data = new JsonObject();
        for (String[] group : new String[][]{{"categories","research_catalog","categories"},{"pages","research_pages","entries"},{"recipes","research_page_recipes","entries"}}) {
            var entries = new JsonArray();
            for (String file : new String[]{"legacy", "revival"}) {
                var stream = getClass().getResourceAsStream("/data/thaumcraft/"+group[1]+"/"+file+".json");
                if(stream == null) { assertEquals("research_page_recipes", group[1]); continue; }
                try(var reader = new java.io.InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    entries.addAll(JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray(group[2]));
                }
            }
            data.add(group[0], entries);
        }
        return data;
    }
    @Test void everyStageAndGateCombinationHasValidReachablePrerequisites() throws Exception {
        TCConfig.SERVER_SPEC.acceptConfig(CommentedConfig.inMemory());
        var f = TCConfig.SERVER.revival;
        ResearchTableAccess.register();
        try {
            var raw = data();
            for(int mask=0;mask<64;mask++) {
                f.global.set(RevivalProfile.VANILLA);
                f.aspectSearch.set((mask&1)!=0); f.autoRefill.set(false); f.randomResearch.set(false);
                f.relatedSearch.set((mask&2)!=0); f.autoConnect.set((mask&4)!=0);
                f.firstResearch.set((mask&8)!=0); f.secondResearch.set((mask&16)!=0); f.thirdResearch.set((mask&32)!=0);
                var aspects = tc4revival.aspect.AspectDefinitions.bundled();
                var resolved = thaumcraft.api.research.ResearchPrerequisites.materialize(raw, aspects);
                var snapshot = ResearchDefinitions.decode(resolved, aspects);
                var disabled = resolved.getAsJsonArray("disabledResearch").asList().stream().map(JsonElement::getAsString).toList();
                String[] keys = {ResearchTableAccess.FIRST, ResearchTableAccess.SECOND, ResearchTableAccess.THIRD};
                for(int stage=1;stage<=3;stage++) assertEquals(!f.stageEnabled(stage), disabled.contains(keys[stage-1]), "mask="+mask);
                var second = snapshot.catalog().require(keys[1]);
                var third = snapshot.catalog().require(keys[2]);
                assertEquals(f.stageEnabled(1), second.parents().contains(keys[0]));
                assertEquals(f.stageEnabled(2), third.parents().contains(keys[1]));
                assertTrue(second.parents().contains("JARBRAIN"));
                assertTrue(third.parents().contains("PRIMPEARL"));
                for(String key:keys) if(!disabled.contains(key)) for(String parent:snapshot.catalog().require(key).parents())
                    assertFalse(disabled.contains(parent), key+" has disabled parent "+parent+" mask="+mask);
            }
        } finally { TCConfig.SERVER_SPEC.acceptConfig(null); }
    }
}
