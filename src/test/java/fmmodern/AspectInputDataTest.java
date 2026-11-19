package fmmodern;

import com.google.gson.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AspectInputDataTest {
    private JsonObject input() throws Exception {
        return JsonParser.parseString(Files.readString(Path.of("src/main/resources/optional_packs/aspect_inputs/data/forbidden_magic/recipe_aspect_inputs/forbidden.json"))).getAsJsonObject();
    }
    @Test void calculationTablesOnlyAddAddonRecipes() throws Exception {
        var root=input();assertEquals(0,root.getAsJsonArray("registrations").size());
        assertEquals(54,root.getAsJsonArray("recipes").size());
        for(var entry:root.getAsJsonArray("recipes"))assertTrue(entry.getAsJsonObject().getAsJsonObject("output").get("item").getAsString().startsWith("forbidden_magic:"));
    }
    @Test void conditionalRecipesKeepBothWorldModes() throws Exception {
        int easy=0,normal=0;
        for(var entry:input().getAsJsonArray("recipes")){
            var row=entry.getAsJsonObject();if(!row.getAsJsonObject("output").get("item").getAsString().equals("forbidden_magic:crystalwell"))continue;
            int count=row.getAsJsonArray("ingredients").size();if(count==4)easy++;if(count==6)normal++;
            assertTrue(row.getAsJsonArray("conditions").toString().contains("easy_research"));
        }
        assertEquals(1,easy);assertEquals(1,normal);
    }
    @Test void activatedComponentsPreserveInertInputWithoutExtraLoss() throws Exception {
        int count=0;
        for(var entry:input().getAsJsonArray("recipes")){
            var row=entry.getAsJsonObject();if(!row.get("kind").getAsString().equals("CRUCIBLE")||!row.getAsJsonObject("output").get("item").getAsString().contains(":wand_"))continue;
            count++;assertEquals(1,row.getAsJsonArray("ingredients").size());assertEquals(0,row.getAsJsonArray("cost").size());
            assertTrue(row.getAsJsonArray("ingredients").toString().contains("_inert"));
        }
        assertEquals(4,count);
    }
}
