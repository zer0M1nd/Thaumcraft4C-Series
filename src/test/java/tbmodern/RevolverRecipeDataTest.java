package tbmodern;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RevolverRecipeDataTest {
    @Test void everyObtainableUpgradeHasAllLevelsWithEscalatingCostAndIsolatedPatch() throws IOException {
        Path root=Path.of("src/main/resources");
        var definitions=JsonParser.parseString(Files.readString(root.resolve("tbmodern/revolver_upgrades.json"))).getAsJsonArray();
        for(var row:definitions) {
            var definition=row.getAsJsonObject();String key=definition.get("key").getAsString();String name=key.substring(key.indexOf(':')+1);
            if(name.equals("poisoned"))continue; // Defined by the legacy API but no legacy research/recipe.
            int maximum=definition.get("maximum").getAsInt();JsonObject first=null;
            for(int level=0;level<maximum;level++) {
                var recipe=JsonParser.parseString(Files.readString(root.resolve("data/thaumicbases/recipes/revolver/"+name+"_"+(level+1)+".json"))).getAsJsonObject();
                if(recipe.has("recipes"))recipe=recipe.getAsJsonArray("recipes").get(1).getAsJsonObject().getAsJsonObject("recipe");
                if(first==null)first=recipe;
                var center=recipe.getAsJsonObject("center").getAsJsonObject("ingredient");
                assertEquals(key,center.get("upgrade").getAsString());assertEquals(level,center.get("level").getAsInt());
                var preview=recipe.getAsJsonObject("center").getAsJsonArray("display").get(0).getAsJsonObject();
                assertEquals("thaumicbases:revolver",preview.get("item").getAsString());
                var previewTag=JsonParser.parseString(preview.get("nbt").getAsString()).getAsJsonObject();
                assertEquals(Set.of("tbUpgrade:"+key),previewTag.keySet());
                assertEquals(level,previewTag.get("tbUpgrade:"+key).getAsInt());
                assertEquals(first.get("instability").getAsInt()+definition.get("instability").getAsInt()*level,recipe.get("instability").getAsInt());
                for(var cost:first.getAsJsonObject("aspects").entrySet())assertEquals(cost.getValue().getAsInt()*(1<<level),recipe.getAsJsonObject("aspects").get(cost.getKey()).getAsInt());
                var patch=JsonParser.parseString(recipe.getAsJsonObject("result").get("patch").getAsString()).getAsJsonObject();
                assertEquals(Set.of("tbUpgrade:"+key),patch.keySet());assertEquals(level+1,patch.get("tbUpgrade:"+key).getAsInt());
            }
        }
    }
    @Test void silverFallbackIsMutuallyExclusive() throws IOException {
        var root=JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/thaumicbases/recipes/revolver/silver_1.json"))).getAsJsonObject();
        var alternatives=root.getAsJsonArray("recipes");assertEquals(2,alternatives.size());
        var silver=alternatives.get(0).getAsJsonObject();var fallback=alternatives.get(1).getAsJsonObject();
        var condition=silver.getAsJsonArray("conditions").get(0).getAsJsonObject();assertEquals("forge:not",condition.get("type").getAsString());
        assertEquals(fallback.getAsJsonArray("conditions").get(0),condition.get("value"));
        assertEquals("forge:ingots/silver",silver.getAsJsonObject("recipe").getAsJsonArray("components").get(2).getAsJsonObject().getAsJsonObject("ingredient").get("tag").getAsString());
    }
}
