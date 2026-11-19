package com.ilya3point999k.thaumicconcilium.api;

import com.google.gson.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecipeInputMergeTest {
    private static ResourceLocation id(String path){return ResourceLocation.parse("concilium_test:"+path);}
    private static JsonElement json(String type){var value=new JsonObject();value.addProperty("type",type);return value;}
    @Test void riftRemovalIsScopedToBothIdAndRecipeTypeAndNeverMutatesReloadInput(){
        var remove=id("remove_rift");var keep=id("keep_rift");
        var input=Map.of(remove,json("thaumicconcilium:chained_rift"),keep,json("thaumicconcilium:chained_rift"));
        ThaumicConciliumApi.removeChainedRiftRecipe(remove.toString());
        var merged=ThaumicConciliumApi.mergeRecipeInput(input);
        assertFalse(merged.containsKey(remove));assertSame(input.get(keep),merged.get(keep));assertEquals(2,input.size());
        var unrelated=Map.of(remove,json("minecraft:crafting_shapeless"));
        assertEquals(unrelated,ThaumicConciliumApi.mergeRecipeInput(unrelated));
    }
    @Test void malformedResourcesRemainForTheNormalParserToDiagnose(){
        var key=id("malformed");ThaumicConciliumApi.removeChainedRiftRecipe(key.toString());
        for(var malformed:List.<JsonElement>of(JsonNull.INSTANCE,new JsonPrimitive(12),new JsonArray(),new JsonObject())){
            var input=Map.of(key,malformed);assertSame(malformed,ThaumicConciliumApi.mergeRecipeInput(input).get(key));
        }
    }
    @Test void legacyUnqualifiedRecipeIdsAreScopedToConcilium(){
        var input=Map.of(ResourceLocation.parse("thaumicconcilium:unit_legacy_id"),json("thaumicconcilium:chained_rift"),ResourceLocation.parse("minecraft:unit_legacy_id"),json("thaumicconcilium:chained_rift"));
        ThaumicConciliumApi.removeChainedRiftRecipe("unit_legacy_id");var merged=ThaumicConciliumApi.mergeRecipeInput(input);
        assertFalse(merged.containsKey(ResourceLocation.parse("thaumicconcilium:unit_legacy_id")));assertTrue(merged.containsKey(ResourceLocation.parse("minecraft:unit_legacy_id")));
    }
}
