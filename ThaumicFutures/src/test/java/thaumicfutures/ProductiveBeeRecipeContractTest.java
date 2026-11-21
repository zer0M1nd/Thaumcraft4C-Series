package thaumicfutures;

import com.google.gson.JsonParser;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProductiveBeeRecipeContractTest {
    @Test void productiveBeeRecipesCannotReferenceUnregisteredFutureBeeTypes()throws Exception{
        var root=Path.of("src/main/resources/data");
        try(var files=Files.walk(root.resolve("thaumicfutures/recipes"))){
            for(var path:files.filter(p->p.toString().endsWith(".json")).toList()){
                var recipe=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                if(!recipe.has("type")||!recipe.get("type").getAsString().startsWith("productivebees:"))continue;
                for(var key:new String[]{"source","result","bee"}){
                    var value=recipe.get(key);if(value==null||!value.isJsonPrimitive())continue;
                    String id=value.getAsString();if(!id.startsWith("thaumicfutures:"))continue;
                    String name=id.substring(id.indexOf(':')+1);
                    try(var definitions=Files.walk(root.resolve("thaumicfutures"))){
                        assertTrue(definitions.anyMatch(p->p.toString().replace('\\','/').contains("/productivebees/")&&p.getFileName().toString().equals(name+".json")),path+" references undefined bee "+id);
                    }
                }
            }
        }
    }
}
