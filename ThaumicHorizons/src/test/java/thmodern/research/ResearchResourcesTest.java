package thmodern.research;

import com.google.gson.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Exercise the pinned base codec, not a second hand-written approximation of its schema. */
class ResearchResourcesTest {
    private JsonArray entries(String directory,String field)throws Exception{
        var entries=new JsonArray();
        try(var files=Files.list(Path.of("src/main/resources/data/thaumichorizons",directory))){
            for(var path:files.filter(p->p.toString().endsWith(".json")).sorted().toList()){
                try(var reader=Files.newBufferedReader(path)){entries.addAll(JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray(field));}
            }
        }
        return entries;
    }
    @Test void allNamedRecipesDecodeAndEveryPageReferenceMatchesItsType()throws Exception{
        var pages=tc4revival.research.ResearchPages.decode(entries("research_pages","entries"));
        var recipes=tc4revival.research.ResearchPageRecipes.decode(entries("research_page_recipes","entries"));
        assertDoesNotThrow(()->recipes.validate(pages));
    }
    @Test void researchUsesSupportedDifficultyAndNonnegativeWarp()throws Exception{
        for(var category:entries("research_catalog","categories"))for(var element:category.getAsJsonObject().getAsJsonArray("research")){
            var row=element.getAsJsonObject();int complexity=row.has("complexity")?row.get("complexity").getAsInt():1;
            assertTrue(complexity>=0&&complexity<=3,row.get("key")+" difficulty");
            assertTrue(!row.has("warp")||row.get("warp").getAsInt()>=0,row.get("key")+" warp");
        }
    }
}
