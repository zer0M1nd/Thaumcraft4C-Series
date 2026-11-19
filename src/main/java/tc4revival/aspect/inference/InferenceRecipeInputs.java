package tc4revival.aspect.inference;

import com.google.gson.*;
import java.util.*;
import tc4revival.aspect.inference.InferenceEngine.*;

/** Lossless dictionary encoding of recipe INPUTS, never calculated aspect amounts. */
public final class InferenceRecipeInputs {
    public static JsonObject compact(JsonObject source) {
        if(source.has("dictionary"))return source.deepCopy();
        var amounts=new JsonArray();var slots=new JsonArray();var recipes=new JsonArray();
        var amountIds=new LinkedHashMap<String,Integer>();var slotIds=new LinkedHashMap<String,Integer>();
        if(source.has("recipes"))for(var element:source.getAsJsonArray("recipes")) {
            try {
            var row=element.getAsJsonObject();
            amount(row.get("output"));
            if(row.getAsJsonArray("inputs").isEmpty())continue;
            for(var slot:row.getAsJsonArray("inputs")){if(slot.getAsJsonArray().isEmpty())throw new IllegalArgumentException("Empty slot");for(var a:slot.getAsJsonArray())amount(a);}
            var encoded=new JsonArray();encoded.add(row.get("id").getAsString());
            encoded.add(intern(row.get("output"),amountIds,amounts));
            for(var input:row.getAsJsonArray("inputs")) {
                var slot=new JsonArray();for(var amount:input.getAsJsonArray())slot.add(intern(amount,amountIds,amounts));
                encoded.add(intern(slot,slotIds,slots));
            }
            recipes.add(encoded);
            }catch(RuntimeException invalid){/* Match the server inference reader: invalid rows are isolated. */}
        }
        var result=new JsonObject();result.addProperty("dictionary",1);result.add("amounts",amounts);result.add("slots",slots);result.add("recipes",recipes);return result;
    }
    private static int intern(JsonElement element,Map<String,Integer> ids,JsonArray values){
        String key=element.toString();var id=ids.get(key);if(id!=null)return id;
        int next=values.size();ids.put(key,next);values.add(element.deepCopy());return next;
    }
    public static List<Recipe> recipes(JsonObject root){
        var result=new ArrayList<Recipe>();if(!root.has("recipes"))return result;
        if(!root.has("dictionary")) {
            for(var element:root.getAsJsonArray("recipes")) {
                try {
                var row=element.getAsJsonObject();var inputs=new ArrayList<List<Amount>>();
                for(var slot:row.getAsJsonArray("inputs")){var alternatives=new ArrayList<Amount>();for(var a:slot.getAsJsonArray())alternatives.add(amount(a));inputs.add(alternatives);}
                result.add(new Recipe(row.get("id").getAsString(),amount(row.get("output")),inputs));
                }catch(RuntimeException invalid){/* Ignore malformed exported rows individually. */}
            }
        }else {
            if(root.get("dictionary").getAsInt()!=1)throw new IllegalArgumentException("Unsupported inference recipe encoding");
            var amounts=new ArrayList<Amount>();for(var a:root.getAsJsonArray("amounts"))amounts.add(amount(a));
            var slots=new ArrayList<List<Amount>>();for(var slot:root.getAsJsonArray("slots")){var list=new ArrayList<Amount>();for(var id:slot.getAsJsonArray())list.add(amounts.get(id.getAsInt()));slots.add(List.copyOf(list));}
            for(var element:root.getAsJsonArray("recipes")){var row=element.getAsJsonArray();var inputs=new ArrayList<List<Amount>>();for(int i=2;i<row.size();i++)inputs.add(slots.get(row.get(i).getAsInt()));result.add(new Recipe(row.get(0).getAsString(),amounts.get(row.get(1).getAsInt()),inputs));}
        }
        return List.copyOf(result);
    }
    private static Amount amount(JsonElement element){var row=element.getAsJsonObject();return new Amount(row.get("key").getAsString(),row.get("count").getAsDouble());}
    private InferenceRecipeInputs(){}
}
