package tc4revival.aspect.sync;

import com.google.gson.*;
import java.util.*;
import java.util.function.BiPredicate;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeManager;
import tc4revival.aspect.*;
import tc4revival.aspect.inference.*;
import tc4revival.aspect.item.*;
import tc4revival.aspect.entity.*;
import tc4revival.aspect.recipe.*;

/** An independent client calculation. It cannot publish values to server globals or upload recipes. */
public class LocalAspectPreparation {
    public record Prepared(AspectDataSet data,AspectCalculationData calculation,ItemAspectCache cache,int prewarmed) {}
    public static Map<String,String> base(Map<String,String> available,RecipeAspectData crafting){
        var files=new TreeMap<>(available);
        files.put(RuntimeCraftingJson.KEY,RuntimeCraftingJson.encode(crafting).toString());
        files.put("item_aspects/thaumcraft:advanced_inference",ItemAspectJson.encode(List.of()).toString());
        files.put("block_aspects/thaumcraft:advanced_inference",AdvancedAspectInference.emptyTable().toString());
        files.put("entity_aspects/thaumcraft:advanced_inference",EntityAspectJson.encode(List.of()).toString());
        return files;
    }
    /** Production entry point: the client has no authority to add local JEI recipes or candidates. */
    public static Prepared prepareAuthoritative(Map<String,String> available,RecipeManager manager,RegistryAccess registries,RecipeAspectData crafting,
            InferenceSettings settings,BiPredicate<String,String> hasTag){
        return prepare(available,manager,registries,crafting,settings.recipes(),List.of(),settings.advanced(),settings,hasTag);
    }
    public static Prepared prepare(Map<String,String> available,RecipeManager manager,RegistryAccess registries,RecipeAspectData crafting,
            JsonObject jei,Collection<ItemStack> candidates,boolean advanced,InferenceSettings settings,BiPredicate<String,String> hasTag){
        var files=base(available,crafting);
        if(advanced){
            try{
                var prediction=AdvancedAspectInference.calculate(manager,registries,new AspectDataSet(files),jei,
                        new JsonObject(),settings.fallback(),settings.keywords(),candidates,true,hasTag,settings.limits());
                files.putAll(prediction.resources());
                tc4revival.TC4Revival.LOGGER.info("Client advanced aspect inference: {} recipes, {} candidates, {} supplemented entries",prediction.recipes(),prediction.candidates(),prediction.saved().size());
            }catch(RuntimeException|LinkageError failure){
                throw new IllegalStateException("Client advanced aspect inference failed",failure);
            }
        }
        var data=new AspectDataSet(files);var calculation=new AspectCalculationData(data,TCAspects.catalog(),hasTag,settings.limits());var cache=new ItemAspectCache(calculation);
        // Precompute the recipe-derived item list before publishing this generation.
        var outputs=new HashSet<String>();
        for(var recipe:crafting.recipes())if(outputs.add(recipe.output().item()))try{
            var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse(recipe.output().item()));
            if(item!=null&&item!=Items.AIR)cache.resolve(new ItemStack(item));
        }catch(RuntimeException|LinkageError failure){tc4revival.TC4Revival.LOGGER.debug("Skipping invalid recipe output {}: {}",recipe.output().item(),failure.toString());}
        return new Prepared(data,calculation,cache,cache.size());
    }
}
