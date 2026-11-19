package thaumicomniverse.compat.create;

import java.util.*;
import com.google.gson.*;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.common.crafting.StrictNBTIngredient;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.content.CrucibleRecipe;
import thaumicomniverse.content.UniversalClusterItem;

public class ClusterMilling {
    private record Cluster(String material,Ingredient ingredient){}
    private static Ingredient tag(String path){return Ingredient.of(TagKey.create(Registries.ITEM,ResourceLocation.parse("forge:"+path)));}
    private static boolean accepts(ProcessingRecipe<?> recipe,Ingredient input){return !recipe.getIngredients().isEmpty()&&Arrays.stream(input.getItems()).anyMatch(recipe.getIngredients().get(0)::test);}
    private static String type(Recipe<?> recipe){return ForgeRegistries.RECIPE_TYPES.getKey(recipe.getType()).toString();}
    public static void append(Map<ResourceLocation,Recipe<?>> all,RegistryAccess registries){
        var clusters=new TreeMap<String,Cluster>();
        for(var key:ForgeRegistries.ITEMS.tags().getTagNames().toList()){
            var id=key.location();if(!id.getNamespace().equals("forge")||!id.getPath().startsWith("clusters/"))continue;
            String material=id.getPath().substring(9);clusters.put(material,new Cluster(material,Ingredient.of(key)));
        }
        for(var recipe:all.values())if(recipe instanceof CrucibleRecipe crucible){var output=crucible.getResultItem(registries);
            if(output.getItem() instanceof UniversalClusterItem&&output.hasTag()){
                String material=output.getTag().getString("Material");if(!material.isBlank()){
                    var own=StrictNBTIngredient.of(output.copyWithCount(1));var previous=clusters.get(material);
                    clusters.put(material,new Cluster(material,previous==null?own:net.minecraftforge.common.crafting.CompoundIngredient.of(previous.ingredient(),own)));
                }
            }
        }
        var processing=all.values().stream().filter(r->r instanceof ProcessingRecipe<?>).map(r->(ProcessingRecipe<?>)r)
            .filter(r->type(r).equals("create:crushing")||type(r).equals("create:milling")).sorted(Comparator.comparing(r->r.getId().toString())).toList();
        for(var cluster:clusters.values()){
            var ore=tag("ores/"+cluster.material());var raw=tag("raw_materials/"+cluster.material());
            var existing=processing.stream().filter(r->accepts(r,ore)&&r.getRollableResults().stream().anyMatch(o->ForgeRegistries.ITEMS.getKey(o.getStack().getItem()).getPath().startsWith("crushed_raw_"))).findFirst().orElse(null);
            if(existing==null)continue;
            for(String machine:List.of("crushing","milling")){
                ProcessingRecipe<?> baseline=processing.stream().filter(r->type(r).equals("create:"+machine)&&accepts(r,raw)).findFirst().orElse(null);
                if(baseline==null)baseline=processing.stream().filter(r->accepts(r,raw)).findFirst().orElse(null);
                boolean inferred=baseline==null;if(inferred)baseline=existing;
                var json=new JsonObject();json.addProperty("type","create:"+machine);var inputs=new JsonArray();inputs.add(cluster.ingredient().toJson());json.add("ingredients",inputs);
                var results=new JsonArray();for(var output:baseline.getRollableResults()){
                    var stack=output.getStack();if(stack.isEmpty()||inferred&&!ForgeRegistries.ITEMS.getKey(stack.getItem()).getPath().startsWith("crushed_raw_"))continue;
                    // Without a raw-input recipe, one raw ore corresponds to one crushed raw ore;
                    // the ore recipe's stone byproducts and bonus rolls do not become cluster loot.
                    int count=inferred?2:stack.getCount()*2;
                    while(count>0){int part=Math.min(stack.getMaxStackSize(),count);var result=ArcaneCreateRecipes.stack(stack.copyWithCount(part));result.addProperty("chance",inferred?1:output.getChance());results.add(result);count-=part;}
                }
                if(results.isEmpty())continue;json.add("results",results);json.addProperty("processingTime",Math.max(100,baseline.getProcessingDuration()));
                var id=ResourceLocation.parse("thaumicomniverse:generated_create/"+machine+"/"+cluster.material().replace(':','/'));
                all.put(id,ForgeRegistries.RECIPE_SERIALIZERS.getValue(ResourceLocation.parse("create:"+machine)).fromJson(id,json));
            }
        }
    }
}
