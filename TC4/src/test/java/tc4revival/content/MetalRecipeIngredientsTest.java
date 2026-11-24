package tc4revival.content;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MetalRecipeIngredientsTest {
    private final MetalRecipeIngredients tags = new MetalRecipeIngredients(
            Map.of("example:ingot", "forge:ingots/example", "example:block", "forge:storage_blocks/example"));

    @Test void recognizesMetalFormsAtTheirOwnCompressionTier() {
        var metals = java.util.Set.of("iron", "alchemical_brass", "void");
        for (var path : java.util.List.of("storage_blocks/void_3x", "storage_blocks/alchemical_brass_4x",
                "storage_blocks/raw_iron", "raw_materials/iron", "quicksilver")) {
            assertTrue(MetalRecipeIngredients.materialForm(path, metals), path);
        }
        for (var path : java.util.List.of("storage_blocks/cobblestone_3x", "nuggets/chicken", "ingots",
                "storage_blocks/void_0x", "plates/void_3x")) {
            assertFalse(MetalRecipeIngredients.materialForm(path, metals), path);
        }
    }

    @Test void normalizesOnlyRecipeInputsAndKeepsOriginalDatapack() {
        JsonObject source = JsonParser.parseString("""
                {"type":"forge:conditional","recipes":[{"conditions":[{"item":"example:block"}],
                "recipe":{"type":"thaumcraft:infusion","center":{"ingredient":{"item":"example:block"}},
                "components":[{"ingredient":[{"item":"example:ingot"},{"type":"forge:nbt","item":"example:ingot","nbt":"{x:1}"}]}],
                "result":{"item":"example:ingot","count":2}}}]}
                """).getAsJsonObject();
        var original = source.deepCopy();
        var branch = tags.normalized(source).getAsJsonArray("recipes").get(0).getAsJsonObject();
        var recipe = branch.getAsJsonObject("recipe");
        assertEquals("forge:storage_blocks/example", recipe.getAsJsonObject("center").getAsJsonObject("ingredient").get("tag").getAsString());
        var choices = recipe.getAsJsonArray("components").get(0).getAsJsonObject().getAsJsonArray("ingredient");
        assertEquals("forge:ingots/example", choices.get(0).getAsJsonObject().get("tag").getAsString());
        assertEquals("example:ingot", choices.get(1).getAsJsonObject().get("item").getAsString());
        assertEquals("example:block", branch.getAsJsonArray("conditions").get(0).getAsJsonObject().get("item").getAsString());
        assertEquals("example:ingot", recipe.getAsJsonObject("result").get("item").getAsString());
        assertEquals(2, recipe.getAsJsonObject("result").get("count").getAsInt());
        assertEquals(original, source);
    }

    @Test void preservesExistingTagsAndConcreteOutputs() {
        var source = JsonParser.parseString("""
                {"type":"minecraft:crafting_shapeless","ingredients":[{"tag":"pack:custom_alloy"}],
                "result":{"item":"example:ingot"}}
                """).getAsJsonObject();
        assertSame(source, tags.normalized(source));
    }
}
