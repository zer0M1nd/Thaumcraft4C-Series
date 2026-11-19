package tc4revival.aspect.recipe;

import java.util.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.sync.ServerAspectData;
import tc4revival.aspect.recipe.RecipeAspectData.*;

/** Server-authoritative projection of actual crafting-table recipes, independent of JEI. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public class RuntimeCraftingAspects {
    private static RecipeAspectData current;
    public static void invalidate(){current=null;}
    public static RecipeAspectData data(){
        if(current==null){
            var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if(server!=null)capture(server);
        }
        return current;
    }
    @SubscribeEvent public static void started(ServerStartedEvent event){if(current==null)capture(event.getServer());}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void sync(OnDatapackSyncEvent event){
        if(event.getPlayer()==null||current==null)capture(event.getPlayerList().getServer());
    }
    private static void capture(net.minecraft.server.MinecraftServer server){
        current=extract(server.getRecipeManager(),server.registryAccess());
        ServerAspectData.capture("crafting_aspect_inputs",Map.of(
                net.minecraft.resources.ResourceLocation.parse("thaumcraft:runtime"),RuntimeCraftingJson.encode(current)));
        tc4revival.TC4Revival.LOGGER.info("Captured {} actual crafting-table recipes for aspect calculation",current.recipes().size());
    }
    public static RecipeAspectData extract(RecipeManager manager,RegistryAccess access){
        var items=new TreeMap<String,ItemProperties>();
        var recipes=new ArrayList<RecipeAspectData.Recipe>();
        var transportDefaults=new IdentityHashMap<net.minecraft.world.item.Item,net.minecraft.nbt.CompoundTag>();
        int fluidPredicates=0;
        for(var recipe:thaumcraft.api.config.FeatureRecipes.definitions(manager).stream().filter(r->r.getType()==RecipeType.CRAFTING).sorted(Comparator.comparing(r->r.getId().toString())).toList()){
            try{
                var output=recipe.getResultItem(access).copy();
                // Dynamic special recipes without a concrete output cannot be statically inferred.
                if(output.isEmpty()||hasVariantTag(output,transportDefaults)||output.getCount()<=0)continue;
                // Fluid contents/capacity cannot be represented by this item-ID-only projection.
                // IC2 expands such predicates by scanning every item and draining its capability;
                // inspect its native inputs first, before getIngredients triggers that global scan.
                if(tc4revival.compat.recipe.Ic2CraftingInputs.hasFluidInput(recipe)){fluidPredicates++;continue;}
                var ingredients=new ArrayList<RecipeAspectData.Ingredient>();boolean supported=true;
                for(var ingredient:recipe.getIngredients()){
                    if(ingredient==net.minecraft.world.item.crafting.Ingredient.EMPTY)continue;
                    var alternatives=new LinkedHashSet<Key>();
                    for(var original:ingredient.getItems()){
                        var stack=original.copy();
                        // Do not silently treat NBT-constrained variants as an unrestricted item.
                        if(hasVariantTag(stack,transportDefaults)){supported=false;break;}
                        if(!stack.isEmpty())alternatives.add(key(stack,items));
                    }
                    if(!supported||alternatives.isEmpty()){supported=false;break;}
                    ingredients.add(new RecipeAspectData.Ingredient(List.copyOf(alternatives),true));
                }
                if(supported&&!ingredients.isEmpty())recipes.add(new RecipeAspectData.Recipe(
                        Kind.CRAFTING,key(output,items),output.getCount(),ingredients,Map.of()));
            }catch(RuntimeException|LinkageError error){
                tc4revival.TC4Revival.LOGGER.warn("Skipping unsupported crafting aspect recipe {}",recipe.getId(),error);
            }
        }
        if(fluidPredicates>0)tc4revival.TC4Revival.LOGGER.info("Excluded {} IC2 fluid-predicate recipes from item-only aspect inference",fluidPredicates);
        return new RecipeAspectData(items,List.of(),recipes);
    }
    private static Key key(net.minecraft.world.item.ItemStack stack,Map<String,ItemProperties> items){
        var id=ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        // Modern registry IDs have no legacy metadata; NBT recipes are excluded above.
        var remainder=stack.hasCraftingRemainingItem()?stack.getCraftingRemainingItem():net.minecraft.world.item.ItemStack.EMPTY;
        String container=remainder.isEmpty()?null:ForgeRegistries.ITEMS.getKey(remainder.getItem()).toString();
        items.put(id,new ItemProperties(false,true,container));
        // Include returned buckets/tools in the seed set even when they are absent
        // from every recipe input and output in this snapshot.
        if(container!=null)items.putIfAbsent(container,new ItemProperties(false,true,null));
        return new Key(id,0);
    }
    private static boolean hasVariantTag(net.minecraft.world.item.ItemStack stack,Map<net.minecraft.world.item.Item,net.minecraft.nbt.CompoundTag> transportDefaults){
        if(!stack.hasTag())return false;
        var tag=stack.getTag().copy();
        // ItemStack initializes damageable items with Damage:0; this is the ordinary fresh item.
        if(stack.isDamageableItem()&&tag.contains("Damage",net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)&&tag.getInt("Damage")==0)tag.remove("Damage");
        if(tag.isEmpty())return false;
        // Some mods materialize empty capability state into NBT only when an item crosses
        // the network (e.g. IE's empty bullets or Apotheosis's empty entity_data). Compare
        // against a fresh plain item's share tag; never strip arbitrary empty NBT or variants.
        var transport=transportDefaults.computeIfAbsent(stack.getItem(),item->{
            var fresh=new net.minecraft.world.item.ItemStack(item);
            var original=fresh.getTag()==null?new net.minecraft.nbt.CompoundTag():fresh.getTag().copy();
            if(fresh.isDamageableItem()&&original.getInt("Damage")==0)original.remove("Damage");
            if(!original.isEmpty())return new net.minecraft.nbt.CompoundTag();
            var shared=fresh.getShareTag();var normalized=shared==null?new net.minecraft.nbt.CompoundTag():shared.copy();
            if(fresh.isDamageableItem()&&normalized.getInt("Damage")==0)normalized.remove("Damage");
            return normalized;
        });
        return !tag.equals(transport);
    }
    public static RecipeAspectData withoutPresetCrafting(RecipeAspectData data){
        return new RecipeAspectData(data.items(),data.registrations(),data.recipes().stream().filter(r->r.kind()!=Kind.CRAFTING).toList());
    }
}
