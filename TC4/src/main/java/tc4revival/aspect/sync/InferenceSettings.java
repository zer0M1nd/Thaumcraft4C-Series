package tc4revival.aspect.sync;

import com.google.gson.*;
import tc4revival.aspect.inference.*;

/** Editable inputs only. A previous run's generated cache is never an input or network payload. */
public record InferenceSettings(boolean advanced,JsonObject fallback,JsonObject keywords,tc4revival.aspect.recipe.InferenceLimits limits,JsonObject recipes) {
    public InferenceSettings(boolean advanced,JsonObject fallback,JsonObject keywords,tc4revival.aspect.recipe.InferenceLimits limits){this(advanced,fallback,keywords,limits,new JsonObject());}
    public InferenceSettings(boolean advanced,JsonObject fallback,JsonObject keywords){this(advanced,fallback,keywords,tc4revival.aspect.recipe.InferenceLimits.server());}
    public static InferenceSettings server(){
        try{return new InferenceSettings(AdvancedAspectInference.enabled(),InferenceFiles.read("item-fallback.json"),InferenceFiles.read("keyword-fallback.json"),tc4revival.aspect.recipe.InferenceLimits.server(),AdvancedAspectInference.enabled()?InferenceRecipeInputs.compact(InferenceFiles.read("jei-recipes.json")):new JsonObject());}
        catch(java.io.IOException failure){throw new IllegalStateException("Cannot read inference settings",failure);}
    }
    public JsonObject encode(){
        var json=new JsonObject();json.addProperty("advanced",advanced);
        if(!limits.equals(tc4revival.aspect.recipe.InferenceLimits.DEFAULT)){json.addProperty("amount_limit",limits.amount());json.addProperty("type_limit",limits.types());json.addProperty("scaling",limits.scaling());
            json.addProperty("soft_limit",limits.softLimit());json.addProperty("hard_limit",limits.hardLimit());json.addProperty("hard_limit_before_scaling",limits.hardLimitBeforeScaling());}
        if(advanced){
            if(recipes.has("recipes")&&recipes.getAsJsonArray("recipes").size()>0)json.add("recipe_inputs",recipes);
            if(fallback.size()>0)json.add("fallback",fallback);
            if(!keywords.equals(InferenceFiles.defaultKeywords()))json.add("keywords",keywords);
        }
        return json;
    }
    public static InferenceSettings decode(String value){
        var json=JsonParser.parseString(value).getAsJsonObject();
        return new InferenceSettings(json.get("advanced").getAsBoolean(),json.has("fallback")?json.getAsJsonObject("fallback"):new JsonObject(),
                json.has("keywords")?json.getAsJsonObject("keywords"):InferenceFiles.defaultKeywords(),new tc4revival.aspect.recipe.InferenceLimits(json.has("amount_limit")?json.get("amount_limit").getAsInt():64,json.has("type_limit")?json.get("type_limit").getAsInt():10,
                !json.has("scaling")||json.get("scaling").getAsBoolean(),json.has("soft_limit")?json.get("soft_limit").getAsInt():32,
                json.has("hard_limit")?json.get("hard_limit").getAsInt():256,json.has("hard_limit_before_scaling")?json.get("hard_limit_before_scaling").getAsInt():1048576),json.has("recipe_inputs")?json.getAsJsonObject("recipe_inputs"):new JsonObject());
    }
}
