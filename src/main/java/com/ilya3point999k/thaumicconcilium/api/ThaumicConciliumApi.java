package com.ilya3point999k.thaumicconcilium.api;

import com.google.gson.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.AspectList;

/** Programmatic additions participate in the next normal recipe reload and vanilla recipe synchronization. */
public final class ThaumicConciliumApi {
    private static final Map<ResourceLocation,ChainedRiftRecipe> RIFTS=new LinkedHashMap<>();
    private static final Set<ResourceLocation> REMOVED_RIFTS=new HashSet<>();
    private static final Map<ResourceLocation,Polishment> POLISHMENTS=new LinkedHashMap<>();
    private static final java.util.List<Polishment> REMOVED_POLISHMENTS=new ArrayList<>();
    private record Polishment(ItemStack item,AspectList aspects){
        Polishment {item=item.copyWithCount(1);aspects=aspects.copy();if(item.isEmpty()||aspects.size()!=1||aspects.getAmount(aspects.getAspects()[0])<=0)throw new IllegalArgumentException("Polishment requires exactly one positive crystal aspect");}
        JsonObject json(){var json=new JsonObject();json.addProperty("type","thaumicconcilium:polishment");json.add("result",ChainedRiftRecipe.ingredient(item).toJson());var aspect=aspects.getAspects()[0];json.addProperty("aspect",aspect.getTag());json.addProperty("amount",aspects.getAmount(aspect));return json;}
        boolean same(Polishment other){return ItemStack.isSameItemSameTags(item,other.item)&&aspects.unwrap().view().equals(other.aspects.unwrap().view());}
        boolean matches(JsonObject json){
            try {
            if(!"thaumicconcilium:polishment".equals(string(json,"type"))||!json.has("result"))return false;
            var aspect=aspects.getAspects()[0];return aspect.getTag().equals(string(json,"aspect"))&&json.has("amount")&&json.get("amount").getAsInt()==aspects.getAmount(aspect)
                    &&net.minecraft.world.item.crafting.Ingredient.fromJson(json.get("result")).test(item);
            }catch(RuntimeException invalidSource){return false;} // Let the ordinary recipe parser report malformed data.
        }
    }
    public static synchronized ChainedRiftRecipe addChainedRiftRecipe(String research,ItemStack result,Object catalyst,AspectList aspects){
        var recipe=new ChainedRiftRecipe(research,result,catalyst,aspects);var id=ResourceLocation.fromNamespaceAndPath("thaumicconcilium","api/rift/"+digest(recipe.toJson().toString()));put(id,recipe);return recipe;
    }
    public static synchronized void addChainedRecipe(String id,String research,ItemStack result,Object catalyst,AspectList aspects){put(id(id),new ChainedRiftRecipe(research,result,catalyst,aspects));}
    public static synchronized void addChainedRecipe(ResourceLocation id,ChainedRiftRecipe recipe){put(id,Objects.requireNonNull(recipe));}
    private static void put(ResourceLocation id,ChainedRiftRecipe recipe){RIFTS.put(Objects.requireNonNull(id),recipe);REMOVED_RIFTS.remove(id);}
    public static synchronized void removeChainedRiftRecipe(String id){var key=id(id);RIFTS.remove(key);REMOVED_RIFTS.add(key);}
    public static synchronized void addPolishmentRecipe(ItemStack item,AspectList tags){var value=new Polishment(item,tags);var id=ResourceLocation.fromNamespaceAndPath("thaumicconcilium","api/polishment/"+digest(value.json().toString()));POLISHMENTS.put(id,value);REMOVED_POLISHMENTS.removeIf(value::same);}
    /** Original spelling retained for callers. */
    public static synchronized void removePolismentRecipe(ItemStack item,AspectList tags){if(tags==null)return;var value=new Polishment(item,tags);POLISHMENTS.values().removeIf(value::same);REMOVED_POLISHMENTS.add(value);}
    public static void removePolishmentRecipe(ItemStack item,AspectList tags){removePolismentRecipe(item,tags);}
    public static ChainedRiftRecipe getRiftRecipe(ItemStack catalyst,AspectList aspects){return recipes(currentLevel()).stream().filter(recipe->recipe.matches(aspects,catalyst)).findFirst().orElse(null);}
    public static ChainedRiftRecipe findMatchingRiftRecipe(ServerPlayer player,AspectList aspects,ItemStack catalyst){return recipes(player.serverLevel()).stream().filter(recipe->recipe.matches(aspects,catalyst)&&(recipe.key.isEmpty()||thaumcraft.api.ThaumcraftApiHelper.isResearchComplete(player,recipe.key))).max(Comparator.comparingInt(recipe->recipe.aspects.size())).orElse(null);}
    public static ChainedRiftRecipe findMatchingRiftRecipe(String username,AspectList aspects,ItemStack catalyst){
        var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();if(server==null)return null;
        var player=server.getPlayerList().getPlayerByName(username);if(player!=null)return findMatchingRiftRecipe(player,aspects,catalyst);
        var cache=server.getProfileCache();return cache==null?null:cache.get(username).map(profile->findMatchingRiftRecipe(profile.getId(),aspects,catalyst)).orElse(null);
    }
    /** Reads existing server-owned knowledge; querying an unknown UUID never creates a player record. */
    public static ChainedRiftRecipe findMatchingRiftRecipe(UUID player,AspectList aspects,ItemStack catalyst){
        var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();if(server==null)return null;
        var known=tc4revival.research.KnowledgeSavedData.get(server).snapshot(player);
        return recipes(server.overworld()).stream().filter(recipe->recipe.matches(aspects,catalyst)&&(recipe.key.isEmpty()||known.map(k->tc4revival.research.ResearchCatalogs.rules().completed(k,recipe.key)).orElse(false))).max(Comparator.comparingInt(recipe->recipe.aspects.size())).orElse(null);
    }
    public static AspectList parseAspects(String source){
        var costs=AspectCostParser.parse(source,tag->tc4revival.aspect.TCAspects.catalog().find(tag).isPresent());var result=new AspectList();
        costs.forEach((tag,amount)->result.add(thaumcraft.api.aspects.Aspect.getAspect(tag),amount));return result;
    }
    public static AspectList getPolishmentRecipe(ItemStack item){
        var level=currentLevel();if(level!=null)return conciliummodern.PolishmentRecipe.find(level,item).map(recipe->new AspectList().add(thaumcraft.api.aspects.Aspect.getAspect(recipe.aspect()),recipe.amount())).orElse(null);
        synchronized(ThaumicConciliumApi.class){return POLISHMENTS.values().stream().filter(value->ChainedRiftRecipe.ingredient(value.item).test(item)).map(value->value.aspects.copy()).findFirst().orElse(null);}
    }
    private static java.util.List<ChainedRiftRecipe> recipes(ServerLevel level){
        if(level==null){synchronized(ThaumicConciliumApi.class){return List.copyOf(RIFTS.values());}}
        return level.getRecipeManager().getAllRecipesFor(conciliummodern.ChainedRiftRecipe.TYPE.get()).stream().sorted(Comparator.comparing(conciliummodern.ChainedRiftRecipe::getId)).map(recipe->{var aspects=new AspectList();recipe.aspectCosts().forEach((tag,amount)->aspects.add(thaumcraft.api.aspects.Aspect.getAspect(tag),amount));return new ChainedRiftRecipe(recipe.research(),recipe.getResultItem(level.registryAccess()),recipe.getIngredients().get(0),aspects,recipe.catalystCount());}).toList();
    }
    /** Called only by the recipe-input mixin before vanilla parses a newly prepared resource map. */
    public static synchronized Map<ResourceLocation,JsonElement> mergeRecipeInput(Map<ResourceLocation,JsonElement> input){
        if(RIFTS.isEmpty()&&REMOVED_RIFTS.isEmpty()&&POLISHMENTS.isEmpty()&&REMOVED_POLISHMENTS.isEmpty())return input;
        var result=new LinkedHashMap<>(input);
        result.entrySet().removeIf(entry->entry.getValue().isJsonObject()&&((REMOVED_RIFTS.contains(entry.getKey())&&"thaumicconcilium:chained_rift".equals(string(entry.getValue().getAsJsonObject(),"type")))||REMOVED_POLISHMENTS.stream().anyMatch(value->value.matches(entry.getValue().getAsJsonObject()))));
        RIFTS.forEach((id,recipe)->result.put(id,recipe.toJson()));POLISHMENTS.forEach((id,recipe)->result.put(id,recipe.json()));return result;
    }
    private static String string(JsonObject object,String key){return object.has(key)&&object.get(key).isJsonPrimitive()?object.get(key).getAsString():"";}
    private static ResourceLocation id(String text){return ResourceLocation.parse(text.indexOf(':')<0?"thaumicconcilium:"+text:text);}
    private static ServerLevel currentLevel(){var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();return server==null?null:server.overworld();}
    private static String digest(String value){try{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))).substring(0,24);}catch(java.security.NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}}
    private ThaumicConciliumApi(){}
}
