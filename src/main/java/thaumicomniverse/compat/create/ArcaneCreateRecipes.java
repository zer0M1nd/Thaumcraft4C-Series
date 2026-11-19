package thaumicomniverse.compat.create;

import java.util.*;
import com.google.gson.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.content.*;

public class ArcaneCreateRecipes {
    private static final String PREFIX="generated_create/";
    public static JsonObject stack(ItemStack stack){var result=new JsonObject();result.addProperty("item",ForgeRegistries.ITEMS.getKey(stack.getItem()).toString());result.addProperty("count",stack.getCount());if(stack.hasTag())result.addProperty("nbt",stack.getTag().toString());return result;}
    public static void apply(RecipeManager manager,RegistryAccess registries){
        var all=new LinkedHashMap<ResourceLocation,Recipe<?>>();for(var recipe:manager.getRecipes())if(!recipe.getId().getNamespace().equals("thaumicomniverse")||!recipe.getId().getPath().startsWith(PREFIX))all.put(recipe.getId(),recipe);
        if(ArcaneContraptions.enabled()){
            for(var recipe:manager.getAllRecipesFor(TCArcane.CRUCIBLE_RECIPE_TYPE.get())){
                if(!thaumcraft.api.research.ResearchCategories.enabled(recipe.research())||recipe.displayCatalyst().isEmpty()||recipe.getResultItem(registries).isEmpty())continue;
                var steps=new JsonArray();var intermediate=new JsonObject();intermediate.addProperty("item","thaumicomniverse:incomplete_essentia_infusion");
                var examples=recipe.displayCatalyst().getItems();
                if(examples.length>0){
                    var visual=new net.minecraft.nbt.CompoundTag();
                    visual.put(SequenceAspects.ORIGINAL,examples[0].copyWithCount(1).save(new net.minecraft.nbt.CompoundTag()));
                    intermediate.addProperty("nbt",visual.toString());
                }
                var costs=recipe.costs().view().entrySet().stream().sorted(Comparator.comparing(e->e.getKey().tag())).toList();
                if(costs.stream().mapToLong(e->(e.getValue()+63L)/64).sum()>4096)continue;
                for(var cost:costs)for(int left=cost.getValue();left>0;left-=64){var step=new JsonObject();step.addProperty("type","thaumicomniverse:essentia_spouting");var in=new JsonArray();in.add(intermediate.deepCopy());step.add("ingredients",in);var out=new JsonArray();out.add(intermediate.deepCopy());step.add("results",out);var aspects=new JsonObject();aspects.addProperty(cost.getKey().tag(),Math.min(64,left));step.add("aspects",aspects);steps.add(step);}
                if(steps.size()>4096)continue;
                for(var step:steps)step.getAsJsonObject().add("selection_output",stack(recipe.getResultItem(registries)));
                var id=ResourceLocation.parse("thaumicomniverse:"+PREFIX+"spouting/"+recipe.getId().getNamespace()+"/"+recipe.getId().getPath());
                var results=new JsonArray();results.add(stack(recipe.getResultItem(registries)));
                if(steps.size()==1){var json=steps.get(0).getAsJsonObject();var input=new JsonArray();input.add(recipe.displayCatalyst().toJson());json.add("ingredients",input);json.add("results",results);all.put(id,ArcaneAssembly.ESSENTIA.serializer.get().fromJson(id,json));}
                else {var json=new JsonObject();json.addProperty("type","create:sequenced_assembly");json.add("ingredient",recipe.displayCatalyst().toJson());json.add("transitionalItem",intermediate);json.add("sequence",steps);json.add("results",results);json.addProperty("loops",1);all.put(id,ForgeRegistries.RECIPE_SERIALIZERS.getValue(ResourceLocation.parse("create:sequenced_assembly")).fromJson(id,json));}
            }
            ClusterMilling.append(all,registries);
        }
        manager.replaceRecipes(all.values());
    }
}
