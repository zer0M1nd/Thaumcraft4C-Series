package tc4revival.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

final class SilverRecipeConditions {
    static JsonArray present() {
        return JsonParser.parseString("""
            [{"type":"forge:or","values":[
              {"type":"forge:not","value":{"type":"forge:tag_empty","tag":"forge:ingots/silver"}},
              {"type":"forge:not","value":{"type":"forge:tag_empty","tag":"forge:nuggets/silver"}}
            ]}]
            """).getAsJsonArray();
    }
    private SilverRecipeConditions() {}
}
