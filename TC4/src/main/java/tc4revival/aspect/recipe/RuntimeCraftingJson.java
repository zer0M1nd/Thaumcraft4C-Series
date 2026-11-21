package tc4revival.aspect.recipe;

import com.google.gson.*;
import java.util.*;
import tc4revival.aspect.recipe.RecipeAspectData.*;

/** Deterministic projection of vanilla-synchronized recipes, with shared ingredient alternatives. */
public class RuntimeCraftingJson {
    public static final String KEY="crafting_aspect_inputs/thaumcraft:runtime";
    public static JsonObject encode(RecipeAspectData data){
        var root=new JsonObject();root.addProperty("format",2);
        var ids=new ArrayList<>(data.items().keySet());Collections.sort(ids);
        var indices=new HashMap<String,Integer>();var items=new JsonArray();
        for(int i=0;i<ids.size();i++){indices.put(ids.get(i),i);items.add(ids.get(i));}
        var groups=new LinkedHashMap<List<Key>,Integer>();var ingredients=new JsonArray();var recipes=new JsonArray();
        for(var recipe:data.recipes()){
            if(recipe.kind()!=Kind.CRAFTING||recipe.output().metadata()!=0||!recipe.cost().isEmpty())throw new IllegalArgumentException("Expected modern crafting recipe");
            var row=new JsonArray();row.add(indices.get(recipe.output().item()));row.add(recipe.outputCount());
            for(var ingredient:recipe.ingredients()){
                if(!ingredient.chooseFirstWithAspects())throw new IllegalArgumentException("Expected crafting ingredient policy");
                Integer group=groups.get(ingredient.alternatives());
                if(group==null){
                    group=groups.size();groups.put(ingredient.alternatives(),group);var values=new JsonArray();
                    for(var key:ingredient.alternatives()){
                        if(key.metadata()!=0)throw new IllegalArgumentException("Unexpected legacy metadata");
                        values.add(indices.get(key.item()));
                    }
                    ingredients.add(values);
                }
                row.add(group);
            }
            recipes.add(row);
        }
        root.add("items",items);root.add("ingredients",ingredients);root.add("recipes",recipes);return root;
    }
    public static RecipeAspectData decode(JsonObject root){
        for(String field:List.of("items","ingredients","recipes"))if(!root.has(field)||!root.get(field).isJsonArray())throw new IllegalArgumentException("Missing compact crafting array: "+field);
        var keys=new ArrayList<Key>();var items=new LinkedHashMap<String,ItemProperties>();
        for(var value:root.getAsJsonArray("items")){
            if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isString())throw new IllegalArgumentException("Expected crafting item ID");
            String id=value.getAsString();keys.add(new Key(id,0));
            if(items.put(id,new ItemProperties(false,true,null))!=null)throw new IllegalArgumentException("Duplicate crafting item");
        }
        var ingredients=new ArrayList<Ingredient>();
        for(var row:root.getAsJsonArray("ingredients")){
            var values=new ArrayList<Key>();for(var index:row.getAsJsonArray())values.add(keys.get(integer(index)));
            if(values.isEmpty())throw new IllegalArgumentException("Empty crafting alternatives");
            ingredients.add(new Ingredient(values,true));
        }
        var recipes=new ArrayList<RecipeAspectData.Recipe>();
        for(var entry:root.getAsJsonArray("recipes")){
            var row=entry.getAsJsonArray();var inputs=new ArrayList<Ingredient>();
            if(row.size()<3||integer(row.get(1))<=0)throw new IllegalArgumentException("Invalid compact crafting recipe");
            for(int i=2;i<row.size();i++)inputs.add(ingredients.get(integer(row.get(i))));
            recipes.add(new RecipeAspectData.Recipe(Kind.CRAFTING,keys.get(integer(row.get(0))),integer(row.get(1)),inputs,Map.of()));
        }
        return new RecipeAspectData(items,List.of(),recipes);
    }
    private static int integer(JsonElement value){
        if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Expected crafting integer");
        return value.getAsBigDecimal().intValueExact();
    }
}
