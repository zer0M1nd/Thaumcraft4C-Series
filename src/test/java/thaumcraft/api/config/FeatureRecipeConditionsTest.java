package thaumcraft.api.config;

import com.google.gson.*;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class FeatureRecipeConditionsTest {
    private static JsonObject json(String value) { return JsonParser.parseString(value).getAsJsonObject(); }
    @BeforeAll static void setup() {
        FeatureRecipes.deferCondition(ResourceLocation.parse("configtest:option"));
        try { CraftingHelper.getCondition(json("{\"type\":\"forge:true\"}")); }
        catch (JsonSyntaxException e) {
            CraftingHelper.register(TrueCondition.Serializer.INSTANCE);
            CraftingHelper.register(FalseCondition.Serializer.INSTANCE);
        }
    }
    @Test void configurationUnderNegationRemainsUnknown() {
        assertEquals(FeatureRecipes.Truth.UNKNOWN, FeatureRecipes.partial(
                json("{\"type\":\"forge:not\",\"value\":{\"type\":\"configtest:option\"}}"), ICondition.IContext.EMPTY));
    }
    @Test void unavailableIndependentPrerequisiteStillRejectsRecipe() {
        assertEquals(FeatureRecipes.Truth.FALSE, FeatureRecipes.partial(json("""
                {"type":"forge:and","values":[{"type":"configtest:option"},{"type":"forge:false"}]}
                """), ICondition.IContext.EMPTY));
        assertEquals(FeatureRecipes.Truth.UNKNOWN, FeatureRecipes.partial(json("""
                {"type":"forge:or","values":[{"type":"configtest:option"},{"type":"forge:false"}]}
                """), ICondition.IContext.EMPTY));
    }
    @Test void preparationDoesNotMutateDatapackInput() {
        var id = ResourceLocation.parse("configtest:recipe");
        var original = json("{\"type\":\"test:recipe\",\"conditions\":[{\"type\":\"configtest:option\"}]}");
        var input = Map.<ResourceLocation, JsonElement>of(id, original);
        var prepared = FeatureRecipes.prepare(null, input, ICondition.IContext.EMPTY);
        assertEquals(1, original.getAsJsonArray("conditions").size());
        assertEquals(0, prepared.get(id).getAsJsonObject().getAsJsonArray("conditions").size());
    }
}
