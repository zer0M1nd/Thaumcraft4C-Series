package thmodern.research;

import com.google.gson.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApiCompletionResourcesTest {
    private JsonElement read(String path)throws Exception{return JsonParser.parseString(Files.readString(Path.of("src/main/resources",path)));}
    @Test void crystalPartsDecodeAs250VisNonRechargeableAndTenPercentDiscount()throws Exception{
        var rod=tc4revival.content.WandParts.Rod.decode(read("data/thaumichorizons/thaumcraft/wand_rods.json").getAsJsonArray().get(0).getAsJsonObject());
        var cap=tc4revival.content.WandParts.Cap.decode(read("data/thaumichorizons/thaumcraft/wand_caps.json").getAsJsonArray().get(0).getAsJsonObject());
        assertEquals(250,rod.capacity);assertFalse(rod.rechargeable);assertTrue(rod.glowing());assertEquals(.9f,cap.cost);
    }
    @Test void vatCustomPageDecodesAlongsideTextAndRecipes()throws Exception{
        var entries=read("data/thaumichorizons/research_pages/vat.json").getAsJsonObject().getAsJsonArray("entries");boolean found=false;
        for(var entry:entries)for(var data:entry.getAsJsonObject().getAsJsonArray("pages")){
            var page=tc4revival.research.ResearchPage.decode(data.getAsJsonObject());
            if(page.type()==tc4revival.research.ResearchPage.Type.CUSTOM){var custom=(tc4revival.research.ResearchPage.Custom)page.recipe();assertEquals("thaumichorizons:vat_structure",custom.id().toString());found=true;}
        }
        assertTrue(found,"The healing vat has an actual custom page rather than only descriptive text");
    }
}
