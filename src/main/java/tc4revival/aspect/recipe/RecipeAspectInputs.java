package tc4revival.aspect.recipe;

import java.util.*;
/** Deterministic composition. Existing semantic properties may be repeated identically, never silently replaced. */
public class RecipeAspectInputs {
    public static RecipeAspectData merge(List<RecipeAspectData> inputs) {
        var items=new LinkedHashMap<String,RecipeAspectData.ItemProperties>();
        var registrations=new ArrayList<RecipeAspectData.Registration>();
        var recipes=new ArrayList<RecipeAspectData.Recipe>();
        for(var input:inputs) {
            input.items().forEach((id,value)->{
                var old=items.putIfAbsent(id,value);
                if(old!=null&&!old.equals(value))throw new IllegalArgumentException("Conflicting semantic item properties: "+id);
            });
            registrations.addAll(input.registrations());recipes.addAll(input.recipes());
        }
        return new RecipeAspectData(items,registrations,recipes);
    }
}
