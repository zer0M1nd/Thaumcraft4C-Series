package tc4revival.aspect.inference;

import com.google.gson.*;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.TC4Revival;
import tc4revival.aspect.*;
import tc4revival.aspect.item.*;
import tc4revival.aspect.entity.*;
import tc4revival.content.*;
import tc4revival.aspect.inference.InferenceEngine.*;

/** Server-authoritative supplementation based solely on public behavior and modern APIs. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public class AdvancedAspectInference {
    private static boolean ready,scanning;
    private static tc4revival.aspect.sync.InferenceSettings activeInputs;
    public static tc4revival.aspect.sync.InferenceSettings inputs(){if(activeInputs==null)throw new IllegalStateException("Inference inputs not ready");return activeInputs;}
    public static boolean enabled(){return tc4revival.config.TCConfig.SERVER.advancedAspectInference.get()&&net.minecraftforge.fml.ModList.get().isLoaded("jei");}
    @SubscribeEvent(priority=EventPriority.LOW) public static void started(ServerStartedEvent e){rebuild(e.getServer());}
    @SubscribeEvent(priority=EventPriority.HIGH) public static void sync(OnDatapackSyncEvent e){if(e.getPlayer()==null||!ready)rebuild(e.getPlayerList().getServer());}
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){ready=false;scanning=false;activeInputs=null;}
    public static String key(ItemStack stack){
        var tag=stack.getTag()==null?new CompoundTag():stack.getTag().copy();
        if(stack.isDamageableItem()&&tag.getInt("Damage")==0)tag.remove("Damage");
        // Forge may add neutral share-tag fields only on the client wire representation.
        // Compare only against a fresh otherwise-empty stack, preserving real NBT variants.
        var plain=new ItemStack(stack.getItem());
        var original=plain.getTag()==null?new CompoundTag():plain.getTag().copy();
        if(plain.isDamageableItem()&&original.getInt("Damage")==0)original.remove("Damage");
        if(original.isEmpty()&&!tag.isEmpty()) {
            var shared=plain.getShareTag();
            if(shared!=null){shared=shared.copy();if(plain.isDamageableItem()&&shared.getInt("Damage")==0)shared.remove("Damage");if(tag.equals(shared))tag=new CompoundTag();}
        }
        return "item:"+ForgeRegistries.ITEMS.getKey(stack.getItem())+(tag.isEmpty()?"":"#"+tag);
    }
    public static ItemStack stack(String key){
        if(!key.startsWith("item:"))return ItemStack.EMPTY;int split=key.indexOf('#');
        var id=ResourceLocation.tryParse(key.substring(5,split<0?key.length():split));
        if(id==null||!ForgeRegistries.ITEMS.containsKey(id))return ItemStack.EMPTY;
        var stack=new ItemStack(ForgeRegistries.ITEMS.getValue(id));
        if(split>=0)try{stack.setTag(TagParser.parseTag(key.substring(split+1)));}catch(Exception e){return ItemStack.EMPTY;}
        return stack;
    }
    private static Map<String,Integer> values(AspectAmounts amounts){var result=new TreeMap<String,Integer>();amounts.view().forEach((a,n)->{if(n>0)result.put(a.tag(),n);});return result;}
    private static Map<String,Integer> values(JsonElement json){
        var result=new TreeMap<String,Integer>();if(json==null||!json.isJsonObject())return result;
        json.getAsJsonObject().entrySet().forEach(e->{if(TCAspects.catalog().find(e.getKey()).isPresent()){int n=e.getValue().getAsBigDecimal().intValueExact();if(n<0||n>10000)throw new IllegalArgumentException("Invalid inferred aspect amount");if(n>0)result.put(e.getKey(),n);}});return result;
    }
    private static JsonObject json(Map<String,Integer> values){var result=new JsonObject();values.forEach(result::addProperty);return result;}
    public static JsonObject emptyTable(){var root=new JsonObject();root.addProperty("format",1);root.add("entries",new JsonArray());return root;}
    public static void rebuild(MinecraftServer server){
        if(scanning)return;scanning=true;
        try{
            activeInputs=null;
            ItemAspects.inference(List.of());BlockAspects.inference(emptyTable());EntityAspects.inference(List.of());ready=true;
            InferenceFiles.defaults();
            activeInputs=tc4revival.aspect.sync.InferenceSettings.server();
            if(!activeInputs.advanced())return;
            var result=calculate(server.getRecipeManager(),server.registryAccess(),tc4revival.aspect.sync.ServerAspectData.snapshot(),
                    activeInputs.recipes(),new JsonObject(),activeInputs.fallback(),activeInputs.keywords(),
                    List.of(),true,AspectRegistryTargets.itemTags(),activeInputs.limits());
            var resources=result.resources();
            ItemAspects.inference(ItemAspectJson.decodeKnown(JsonParser.parseString(resources.get("item_aspects/thaumcraft:advanced_inference")),TCAspects.catalog(),AspectRegistryTargets::item));
            BlockAspects.inference(JsonParser.parseString(resources.get("block_aspects/thaumcraft:advanced_inference")));
            EntityAspects.inference(EntityAspectJson.decode(JsonParser.parseString(resources.get("entity_aspects/thaumcraft:advanced_inference"))));
            InferenceFiles.write("aspect-cache.json",result.saved());
            var report=new JsonObject();for(var e:InferenceFiles.read("verify.json").entrySet()){
                var row=new JsonObject();row.add("expected",e.getValue());row.add("actual",json(result.known().getOrDefault(e.getKey(),Map.of())));row.addProperty("matches",values(e.getValue()).equals(result.known().getOrDefault(e.getKey(),Map.of())));report.add(e.getKey(),row);
            }
            InferenceFiles.write("verification-results.json",report);
            TC4Revival.LOGGER.info("Advanced aspect inference: {} recipes, {} candidates, {} supplemented entries",result.recipes(),result.candidates(),result.saved().size());
        }catch(Exception e){activeInputs=new tc4revival.aspect.sync.InferenceSettings(false,new JsonObject(),new JsonObject(),tc4revival.aspect.recipe.InferenceLimits.server());TC4Revival.LOGGER.error("Advanced aspect inference failed; check config/thaumcraft/advanced-inference",e);}
        finally{scanning=false;}
    }
    public record Prediction(Map<String,String> resources,Map<String,Map<String,Integer>> known,JsonObject saved,int recipes,int candidates) {}
    /** Read-only prediction shared by both sides. Never mutates server registries or the integrated server. */
    public static Prediction calculate(RecipeManager manager,net.minecraft.core.RegistryAccess access,tc4revival.aspect.sync.AspectDataSet files,
            JsonObject jei,JsonObject cache,JsonObject fallback,JsonObject keyword,Collection<ItemStack> localItems,boolean defaultItems,
            java.util.function.BiPredicate<String,String> hasTag){
        return calculate(manager,access,files,jei,cache,fallback,keyword,localItems,defaultItems,hasTag,tc4revival.aspect.recipe.InferenceLimits.server());
    }
    public static Prediction calculate(RecipeManager manager,net.minecraft.core.RegistryAccess access,tc4revival.aspect.sync.AspectDataSet files,
            JsonObject jei,JsonObject cache,JsonObject fallback,JsonObject keyword,Collection<ItemStack> localItems,boolean defaultItems,
            java.util.function.BiPredicate<String,String> hasTag,tc4revival.aspect.recipe.InferenceLimits limits){
        var base=new tc4revival.aspect.sync.AspectCalculationData(files,TCAspects.catalog(),hasTag,limits);
        var authoredEntities=new HashSet<String>();files.directory("entity_aspects").values().forEach(j->EntityAspectJson.decode(j).forEach(r->authoredEntities.add(r.entity())));
            var candidates=new TreeMap<String,String>();var seeds=new TreeMap<String,Map<String,Integer>>();var protectedKeys=new HashSet<String>();
            if(defaultItems)for(var item:ForgeRegistries.ITEMS)if(item!=Items.AIR){
                try{var stack=new ItemStack(item);candidates.put(key(stack),item.getDescriptionId());}
                catch(RuntimeException|LinkageError ignored){/* A default stack is not valid for every third-party item. */}
            }
            for(var original:localItems)try{var stack=original.copy();if(!stack.isEmpty())candidates.put(key(stack),stack.getDescriptionId());}catch(RuntimeException|LinkageError ignored){}
            var recipes=new ArrayList<InferenceEngine.Recipe>();
            for(var recipe:manager.getRecipes().stream().sorted(Comparator.comparing(r->r.getId().toString())).toList()){
                try{collect(recipe,access,recipes,candidates);}catch(RuntimeException|LinkageError failure){TC4Revival.LOGGER.debug("Cannot project inference recipe {}: {}",recipe.getId(),failure.toString());}
            }
            readJei(jei,recipes,candidates);
            cache.keySet().forEach(k->{if(exists(k))candidates.putIfAbsent(k,k);});fallback.keySet().forEach(k->{if(exists(k))candidates.putIfAbsent(k,k);});
            var authoredBlocks=new HashSet<String>();base.blockAspects().rules().forEach(rule->authoredBlocks.add(rule.block()));
            for(var block:ForgeRegistries.BLOCKS){String id=ForgeRegistries.BLOCKS.getKey(block).toString();String key="block:"+id;candidates.put(key,block.getDescriptionId());
                if(authoredBlocks.contains(id)
                        ||base.editor().resolve("block",id,t->block.defaultBlockState().is(net.minecraft.tags.BlockTags.create(ResourceLocation.parse(t)))).isPresent()
                        ||base.blocks().resolve(id,BlockScanTargets.properties(block.defaultBlockState())).isPresent())protectedKeys.add(key);
            }
            for(var entity:ForgeRegistries.ENTITY_TYPES){String id=ForgeRegistries.ENTITY_TYPES.getKey(entity).toString(),key="entity:"+id;candidates.put(key,entity.getDescriptionId());
                if(authoredEntities.contains(id)||base.editor().get("entity:"+id).isPresent())protectedKeys.add(key);
            }
            for(var fluid:ForgeRegistries.FLUIDS)if(fluid!=net.minecraft.world.level.material.Fluids.EMPTY)candidates.put("fluid:"+ForgeRegistries.FLUIDS.getKey(fluid),fluid.getFluidType().getDescriptionId());
            var invalidItems=new HashSet<String>();
            for(String key:candidates.keySet())if(key.startsWith("item:"))try{
                var stack=stack(key);if(stack.isEmpty())continue;
                var nativeValues=ItemStackAspects.resolve(stack,base).orElseGet(AspectAmounts::new);
                var id=ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
                if(!nativeValues.isEmpty())seeds.put(key,values(nativeValues));
                if(base.direct().contains(id)||!nativeValues.isEmpty()||base.editor().resolve("item",id,t->stack.is(net.minecraft.tags.ItemTags.create(ResourceLocation.parse(t)))).isPresent())protectedKeys.add(key);
            }catch(RuntimeException|LinkageError failure){invalidItems.add(key);TC4Revival.LOGGER.debug("Skipping invalid inference candidate {}: {}",key,failure.toString());}
            invalidItems.forEach(key->{candidates.remove(key);seeds.remove(key);protectedKeys.remove(key);});
            // A block already scanned through its item is not missing aspects.
            for(var block:ForgeRegistries.BLOCKS)if(block.asItem()!=Items.AIR&&protectedKeys.contains(key(new ItemStack(block))))protectedKeys.add("block:"+ForgeRegistries.BLOCKS.getKey(block));
            // Existing values always outrank distributed seeds; unknown aspect names are omitted individually.
            for(var e:cache.entrySet())if(candidates.containsKey(e.getKey())&&!protectedKeys.contains(e.getKey())){var v=values(e.getValue());if(!v.isEmpty())seeds.putIfAbsent(e.getKey(),v);}
            var known=new TreeMap<>(InferenceEngine.infer(seeds,recipes,6,.9,limits,v->limits.apply(v,TCAspects.catalog())));
            // Fluid values are per 1000 mB, derived from a full bucket with its empty container removed.
            for(var fluid:ForgeRegistries.FLUIDS){String key="fluid:"+ForgeRegistries.FLUIDS.getKey(fluid);if(known.containsKey(key)||fluid.getBucket()==Items.AIR)continue;
                var full=known.get(key(new ItemStack(fluid.getBucket())));var empty=known.get(key(new ItemStack(Items.BUCKET)));
                if(full!=null){var v=new TreeMap<String,Integer>();full.forEach((a,n)->{int left=limits.scaling()?limits.compress(limits.expand(n)-limits.expand(empty==null?0:empty.getOrDefault(a,0))):n-(empty==null?0:empty.getOrDefault(a,0));if(left>0)v.put(a,left);});if(!v.isEmpty())known.put(key,limits.apply(v,TCAspects.catalog()));}
            }
            known=new TreeMap<>(InferenceEngine.infer(known,recipes,6,.9,limits,v->limits.apply(v,TCAspects.catalog())));
            var patterns=new HashMap<String,java.util.regex.Pattern>();
            for(var e:candidates.entrySet())if(!known.containsKey(e.getKey())&&!protectedKeys.contains(e.getKey())){
                var v=values(fallback.get(e.getKey()));
                if(v.isEmpty()&&keyword.has("rules"))for(var rule:keyword.getAsJsonArray("rules")){
                    var row=rule.getAsJsonObject();String text=e.getKey()+" "+e.getValue();
                    String kind=e.getKey().substring(0,e.getKey().indexOf(':'));
                    if(row.has("kinds")?!row.getAsJsonArray("kinds").asList().contains(new JsonPrimitive(kind)):!kind.equals("item")&&!kind.equals("block"))continue;
                    if(patterns.computeIfAbsent(row.get("pattern").getAsString(),pattern->java.util.regex.Pattern.compile(pattern,java.util.regex.Pattern.CASE_INSENSITIVE|java.util.regex.Pattern.UNICODE_CASE)).matcher(text).find()){v=values(row.get("aspects"));if(!v.isEmpty())break;}
                }
                if(!v.isEmpty())known.put(e.getKey(),limits.apply(v,TCAspects.catalog()));
            }
            // Missing technical blocks inherit their item or fluid only after the recipe stage.
            for(var block:ForgeRegistries.BLOCKS){String key="block:"+ForgeRegistries.BLOCKS.getKey(block);if(protectedKeys.contains(key)||known.containsKey(key))continue;
                var v=block.asItem()!=Items.AIR?known.get(key(new ItemStack(block))):null;
                if(v==null&&!block.defaultBlockState().getFluidState().isEmpty())v=known.get("fluid:"+ForgeRegistries.FLUIDS.getKey(block.defaultBlockState().getFluidState().getType()));
                if(v!=null)known.put(key,limits.apply(v,TCAspects.catalog()));
            }
        // A failed third-party query must not become an empty or keyword-derived cache entry.
        known.keySet().removeAll(invalidItems);
        known.replaceAll((k,v)->protectedKeys.contains(k)?v:limits.apply(v,TCAspects.catalog()));
        var saved=new JsonObject();known.forEach((k,v)->{if(!protectedKeys.contains(k)&&!v.isEmpty())saved.add(k,json(v));});
        return new Prediction(supplemental(known,protectedKeys),known,saved,recipes.size(),candidates.size());
    }
    private static boolean exists(String key){
        try{
            if(key.startsWith("item:"))return !stack(key).isEmpty();int colon=key.indexOf(':');if(colon<0)return false;var id=ResourceLocation.tryParse(key.substring(colon+1));if(id==null)return false;
            return switch(key.substring(0,colon)){case "fluid"->ForgeRegistries.FLUIDS.containsKey(id);case "block"->ForgeRegistries.BLOCKS.containsKey(id);case "entity"->ForgeRegistries.ENTITY_TYPES.containsKey(id);default->false;};
        }catch(RuntimeException|LinkageError failure){return false;}
    }
    private static List<Amount> ingredient(Ingredient ingredient,Map<String,String> candidates){
        // Some name/tooltip implementations lazily write NBT (for example EnderChests owner).
        // Ingredient stacks belong to RecipeManager and must never be passed to those hooks.
        var result=new ArrayList<Amount>();for(var original:ingredient.getItems()){var stack=original.copy();if(!stack.isEmpty()){String key=key(stack);candidates.putIfAbsent(key,stack.getDescriptionId());result.add(new Amount(key,Math.max(1,stack.getCount())));}}return result;
    }
    private static void collect(net.minecraft.world.item.crafting.Recipe<?> recipe,net.minecraft.core.RegistryAccess access,List<InferenceEngine.Recipe> recipes,Map<String,String> candidates){
        var output=recipe.getResultItem(access).copy();if(output.isEmpty())return;
        // IC2's native fluid predicates enumerate every item when flattened to ingredients.
        // Preserve them in the recipe manager; let explicit fluid/JEI rows describe those inputs.
        if(tc4revival.compat.recipe.Ic2CraftingInputs.hasFluidInput(recipe))return;
        var inputs=new ArrayList<List<Amount>>();
        if(recipe instanceof CrucibleRecipe crucible)inputs.add(ingredient(crucible.displayCatalyst(),candidates));
        else if(recipe instanceof ArcaneRecipe arcane){
            var json=arcane.displayDefinition();if(!json.has("result"))return;
            var parsed=json.has("pattern")?RecipeSerializer.SHAPED_RECIPE.fromJson(recipe.getId(),json):RecipeSerializer.SHAPELESS_RECIPE.fromJson(recipe.getId(),json);
            for(var input:parsed.getIngredients())if(input!=Ingredient.EMPTY)inputs.add(ingredient(input,candidates));
        }else if(recipe instanceof InfusionRecipe infusion){
            var json=infusion.displayDefinition();if(!json.has("center")||!json.getAsJsonObject("center").has("ingredient"))return;
            var all=new JsonArray();all.add(json.get("center"));all.addAll(json.getAsJsonArray("components"));
            for(var input:all){var row=input.getAsJsonObject();if(row.has("nbt")||!row.has("ingredient"))return;inputs.add(ingredient(Ingredient.fromJson(row.get("ingredient")),candidates));}
        }else for(var input:recipe.getIngredients())if(input!=Ingredient.EMPTY)inputs.add(ingredient(input,candidates));
        if(inputs.isEmpty()||inputs.stream().anyMatch(List::isEmpty))return;
        String key=key(output);candidates.putIfAbsent(key,output.getDescriptionId());
        recipes.add(new InferenceEngine.Recipe(recipe.getId().toString(),new Amount(key,output.getCount()),inputs));
    }
    public static JsonObject recipeJson(InferenceEngine.Recipe recipe){
        var json=new JsonObject();json.addProperty("id",recipe.id());json.add("output",amountJson(recipe.output()));var slots=new JsonArray();
        for(var slot:recipe.inputs()){var list=new JsonArray();slot.forEach(a->list.add(amountJson(a)));slots.add(list);}json.add("inputs",slots);return json;
    }
    private static JsonObject amountJson(Amount amount){var json=new JsonObject();json.addProperty("key",amount.key());json.addProperty("count",amount.count());return json;}
    private static Amount amount(JsonElement element){var row=element.getAsJsonObject();return new Amount(row.get("key").getAsString(),row.get("count").getAsDouble());}
    private static void readJei(JsonObject root,List<InferenceEngine.Recipe> recipes,Map<String,String> candidates){
        readJei(root,recipes,candidates,AdvancedAspectInference::exists);
    }
    static void readJei(JsonObject root,List<InferenceEngine.Recipe> recipes,Map<String,String> candidates,java.util.function.Predicate<String> exists){
        // Thousands of recipes share the same alternatives. Validate each identity once per snapshot,
        // including negative results; do not retain registry or stack state across reloads.
        var present=new HashMap<String,Boolean>();
        for(var row:InferenceRecipeInputs.recipes(root))try{
            var output=row.output();var inputs=new ArrayList<List<Amount>>();boolean valid=present.computeIfAbsent(output.key(),exists::test);
            for(var slot:row.inputs()){var alternatives=slot.stream().filter(a->present.computeIfAbsent(a.key(),exists::test)).toList();if(alternatives.isEmpty())valid=false;inputs.add(alternatives);}
            if(!valid)continue;recipes.add(new InferenceEngine.Recipe("jei:"+row.id(),output,inputs));
            candidates.putIfAbsent(output.key(),output.key());inputs.forEach(slot->slot.forEach(a->candidates.putIfAbsent(a.key(),a.key())));
        }catch(RuntimeException e){TC4Revival.LOGGER.warn("Skipping invalid JEI inference row: {}",e.toString());}
    }
    private static Map<String,String> supplemental(Map<String,Map<String,Integer>> known,Set<String> protectedKeys){
        var items=new ArrayList<ItemAspectRule>();var blocks=emptyTable();var entities=new ArrayList<EntityAspectRule>();
        var variantFields=new HashMap<String,Set<String>>();known.keySet().stream().filter(k->k.startsWith("item:")&&k.contains("#")).forEach(k->{var s=stack(k);if(s.hasTag())variantFields.computeIfAbsent(k.substring(0,k.indexOf('#')),id->new TreeSet<>()).addAll(s.getTag().getAllKeys());});
        for(var entry:known.entrySet()){
            String key=entry.getKey();if(protectedKeys.contains(key)||entry.getValue().isEmpty())continue;
            if(key.startsWith("item:")){
                var s=stack(key);if(s.isEmpty())continue;var quantities=entry.getValue().entrySet().stream().map(e->new ItemAspectRule.Quantity(e.getKey(),e.getValue())).toList();
                var selector=s.hasTag()?NbtOps.INSTANCE.convertTo(com.mojang.serialization.JsonOps.INSTANCE,s.getTag()).getAsJsonObject():new JsonObject();
                // Preserve the plain cached value, but do not apply it to observed NBT variants.
                String baseKey=key.contains("#")?key.substring(0,key.indexOf('#')):key;
                for(String field:variantFields.getOrDefault(baseKey,Set.of()))if(!selector.has(field)){var absent=new JsonObject();absent.addProperty("$exists",false);selector.add(field,absent);}
                var nbt=new ItemNbtPredicate(selector);
                items.add(new ItemAspectRule(List.of(ForgeRegistries.ITEMS.getKey(s.getItem()).toString()),null,quantities,true,nbt));
            }else if(key.startsWith("block:")){
                var row=new JsonObject();row.addProperty("block",key.substring(6));row.add("aspects",json(entry.getValue()));blocks.getAsJsonArray("entries").add(row);
            }else if(key.startsWith("entity:"))entities.add(new EntityAspectRule(key.substring(7),"advanced:"+key,EntityAspectRule.Variant.ANY,"",entry.getValue().entrySet().stream().map(e->new EntityAspectRule.Quantity(e.getKey(),e.getValue())).toList()));
        }
        return Map.of("item_aspects/thaumcraft:advanced_inference",ItemAspectJson.encode(items).toString(),
                "block_aspects/thaumcraft:advanced_inference",blocks.toString(),"entity_aspects/thaumcraft:advanced_inference",EntityAspectJson.encode(entities).toString());
    }
    private AdvancedAspectInference(){}
}
