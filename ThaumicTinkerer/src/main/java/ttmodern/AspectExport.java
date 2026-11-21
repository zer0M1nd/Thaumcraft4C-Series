package ttmodern;

import com.google.gson.*;
import java.util.*;
import java.nio.file.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.item.crafting.Ingredient;
import tc4revival.aspect.item.ItemStackAspects;

/** Development-only exporter of the original TC4 recipe formula into ordinary addon JSON. */
public final class AspectExport {
    private final Map<String,JsonObject> recipes=new LinkedHashMap<>();
    private final Map<String,Map<String,Integer>> cache=new LinkedHashMap<>();
    private final Set<String> active=new HashSet<>();
    public AspectExport() throws Exception {
        var directory=net.minecraftforge.fml.ModList.get().getModFileById(Tinkerer.ID).getFile().findResource("data",Tinkerer.ID,"recipes");
        try(var files=Files.walk(directory)) {
            for(var path:files.filter(p->p.toString().endsWith(".json")).sorted().toList())try(var reader=Files.newBufferedReader(path)) {
                var recipe=JsonParser.parseReader(reader).getAsJsonObject();
                if(!recipe.has("result")||!recipe.get("result").isJsonObject())continue;
                recipes.putIfAbsent(recipe.getAsJsonObject("result").get("item").getAsString(),recipe);
            }
        }
    }
    private Map<String,Integer> value(String id) {
        if(cache.containsKey(id))return cache.get(id);
        if(!active.add(id))throw new IllegalStateException("Aspect recipe cycle: "+id);
        try {
            // Recompute our own recipes; previously exported JSON must not mask recipe changes.
            if(!ForgeRegistries.ITEMS.containsKey(ResourceLocation.parse(id)))throw new IllegalStateException("Unknown recipe item: "+id);
            var stack=new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(id)));
            var existing=ItemStackAspects.resolve(stack);
            if(existing.isPresent() && !recipes.containsKey(id)) {
                var result=new TreeMap<String,Integer>();existing.get().view().forEach((a,n)->result.put(a.tag(),n));cache.put(id,result);return result;
            }
            // Legacy crafting ignores untagged inputs (e.g. dimension shards) instead of rejecting the recipe.
            var recipe=recipes.get(id);if(recipe==null){cache.put(id,Map.of());return Map.of();}
            var totals=new TreeMap<String,Integer>();String type=recipe.get("type").getAsString();
            if(type.equals("thaumcraft:crucible"))add(totals,ingredient(recipe.get("catalyst")));
            else {
                if(type.equals("thaumcraft:infusion")) {
                    add(totals,ingredient(recipe.getAsJsonObject("center").get("ingredient")));
                    recipe.getAsJsonArray("components").forEach(c->add(totals,ingredient(c.getAsJsonObject().get("ingredient"))));
                }else if(type.equals("minecraft:crafting_shapeless")) {
                    recipe.getAsJsonArray("ingredients").forEach(part->add(totals,ingredient(part)));
                }else {
                    var key=recipe.getAsJsonObject("key");
                    recipe.getAsJsonArray("pattern").forEach(row->{for(char symbol:row.getAsString().toCharArray())if(symbol!=' ')add(totals,ingredient(key.get(String.valueOf(symbol))));});
                }
                int count=recipe.getAsJsonObject("result").has("count")?recipe.getAsJsonObject("result").get("count").getAsInt():1;
                totals.replaceAll((a,n)->(int)(n*.75f/count));
            }
            var costs=recipe.has("aspects")?recipe.getAsJsonObject("aspects"):recipe.has("vis")?recipe.getAsJsonObject("vis"):new JsonObject();
            int count=recipe.getAsJsonObject("result").has("count")?recipe.getAsJsonObject("result").get("count").getAsInt():1;
            costs.entrySet().forEach(e->totals.merge(e.getKey(),(int)(Math.sqrt(e.getValue().getAsInt())/count),Integer::sum));
            totals.entrySet().removeIf(e->e.getValue()<=0);totals.replaceAll((a,n)->Math.min(64,n));cache.put(id,totals);return totals;
        }finally{active.remove(id);}
    }
    private Map<String,Integer> ingredient(JsonElement json) {
        var options=Ingredient.fromJson(json).getItems();
        if(options.length==0)throw new IllegalStateException("Empty ingredient: "+json);
        return value(ForgeRegistries.ITEMS.getKey(options[0].getItem()).toString());
    }
    private static void add(Map<String,Integer> total,Map<String,Integer> part){part.forEach((a,n)->total.merge(a,n,Integer::sum));}
    public JsonObject export() {
        var entries=new JsonArray();
        for(String id:recipes.keySet()) {
            var row=new JsonObject();var items=new JsonArray();items.add(id);row.add("items",items);var aspects=new JsonArray();
            value(id).forEach((tag,amount)->{var a=new JsonObject();a.addProperty("aspect",tag);a.addProperty("amount",amount);aspects.add(a);});row.add("aspects",aspects);entries.add(row);
        }
        var result=new JsonObject();result.addProperty("format",1);result.add("entries",entries);return result;
    }
}
