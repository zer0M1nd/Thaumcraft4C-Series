package tc4revival.research;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchRecipeLinksTest {
    private ResearchRecipeLinks links(String firstRecipe, String firstType, String firstOutput, String recipes) {
        String output = "{\"item\":\"minecraft:iron_ingot\",\"metadata\":0,\"count\":1,\"damageable\":false}";
        String source = "[{\"key\":\"FIRST\",\"pages\":[{\"type\":\"" + firstType
                + "\",\"aspects\":[],\"recipe\":" + firstRecipe + ",\"recipeOutput\":" + firstOutput
                + "}]},{\"key\":\"SECOND\",\"pages\":[{\"type\":\"NORMAL_CRAFTING\",\"aspects\":[],\"recipeOutput\":"
                + output + "}]}]";
        return new ResearchRecipeLinks(BookTestData.pages(com.google.gson.JsonParser.parseString(source).getAsJsonArray()),
                ResearchPageRecipes.decode(com.google.gson.JsonParser.parseString(recipes).getAsJsonArray()));
    }
    @Test void firstOrdinaryMatchBlocksLaterRecipesWhenIncomplete() {
        String output = "{\"item\":\"minecraft:iron_ingot\",\"metadata\":0,\"count\":1,\"damageable\":false}";
        var links = links("null", "NORMAL_CRAFTING", output, "[]");
        assertTrue(links.find(stack -> true, key -> key.equals("SECOND")).isEmpty());
        assertEquals(new ResearchRecipeLinks.Target("FIRST", 0), links.find(stack -> true, key -> true).orElseThrow());
        assertTrue(links.find(stack -> false, key -> true).isEmpty());
    }
    @Test void crucibleArraysContinuePastAnIncompleteMatch() {
        String recipe = "{\"kind\":\"sequence\",\"elements\":[{\"kind\":\"reference\",\"keys\":[\"IRON\"],\"class\":\"Crucible\"}]}";
        String definitions = """
                [{"key":"IRON","class":"Crucible","data":{"kind":"crucible","research":"FIRST",
                "aspects":[],"catalyst":null,"output":{"kind":"stack","stack":{
                "item":"minecraft:iron_ingot","metadata":0,"count":1,"damageable":false}}}}]
                """;
        var links = links(recipe, "CRUCIBLE_CRAFTING", "null", definitions);
        assertEquals(new ResearchRecipeLinks.Target("SECOND", 0), links.find(stack -> true, key -> key.equals("SECOND")).orElseThrow());
        assertEquals(new ResearchRecipeLinks.Target("FIRST", 0), links.find(stack -> true, key -> true).orElseThrow());
    }
}
