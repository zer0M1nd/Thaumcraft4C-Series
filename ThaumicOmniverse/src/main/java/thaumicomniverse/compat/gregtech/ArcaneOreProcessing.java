package thaumicomniverse.compat.gregtech;

import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import java.util.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import tc4revival.content.*;

/** Change only requested native ore outputs; preserve chances, power, timing, and all other outputs. */
public final class ArcaneOreProcessing {
    private static final Map<GTRecipe,GTRecipe> ORIGINALS=new WeakHashMap<>();
    public static ShardType previous(ShardType shard) {
        return switch(shard) {case AIR->ShardType.FIRE;case EARTH->ShardType.AIR;case WATER->ShardType.EARTH;case FIRE->ShardType.WATER;case ORDER->ShardType.ENTROPY;case ENTROPY->ShardType.ORDER;case BALANCED->throw new IllegalArgumentException("Balanced shards have no ore");};
    }
    public static ShardType next(ShardType shard) {
        return switch(shard) {case AIR->ShardType.EARTH;case EARTH->ShardType.WATER;case WATER->ShardType.FIRE;case FIRE->ShardType.AIR;case ORDER->ShardType.ENTROPY;case ENTROPY->ShardType.ORDER;case BALANCED->throw new IllegalArgumentException("Balanced shards have no ore");};
    }
    public static void apply(RecipeManager manager) {
        for(var entry:manager.getRecipes()) {
            if(!(entry instanceof GTRecipe current))continue;
            var original=ORIGINALS.getOrDefault(current,current);
            var patched=thaumicomniverse.RevivalFeatures.Group.GREGTECH.enabled()?patch(original):original;
            if(patched==current)continue;
            if(original==current)ORIGINALS.put(current,current.copy());
            // GT's ingredient lookup and category lists retain the registered recipe instance.
            // Only outputs change: preserve that identity so real machines see the same recipe as JEI.
            current.outputs.put(ItemRecipeCapability.CAP,new ArrayList<>(patched.outputs.get(ItemRecipeCapability.CAP)));
        }
    }
    private static GTRecipe patch(GTRecipe original) {
        String path=original.getId().getPath();if(!original.getId().getNamespace().equals("gtceu"))return original;
        for(var entry:ArcaneMaterials.INFUSED.entrySet()) {
            String name=entry.getValue().getName();var shard=entry.getKey();
            if(path.equals("macerator/macerate_"+name+"_refined_ore_to_dust"))
                return replace(original,1,ChemicalHelper.get(TagPrefix.dustSmall,ArcaneMaterials.THAUMIUM,2));
            if(path.equals("macerator/macerate_"+name+"_crushed_ore_to_dust"))
                return replace(original,1,dust(ArcaneMaterials.INFUSED.get(previous(shard))));
            if(path.equals("thermal_centrifuge/centrifuge_"+name+"_purified_ore_to_refined_ore")
                    ||path.equals("thermal_centrifuge/centrifuge_"+name+"_crushed_ore_to_refined_ore"))
                return replace(original,1,dust(ArcaneMaterials.INFUSED.get(next(shard))));
            if(path.equals("sifter/sift_"+name+"_purified_ore_to_gems")) {
                var result=original.copy();var outputs=new ArrayList<>(result.outputs.get(ItemRecipeCapability.CAP));
                // Native infused-gem recipes currently have four outputs. Keep
                // a future larger native recipe intact and respect its six slots.
                for(var neighbor:List.of(previous(shard),next(shard))) {
                    if(outputs.size()>=6)break;
                    outputs.add(new Content(ItemRecipeCapability.CAP.of(dust(ArcaneMaterials.INFUSED.get(neighbor))),2500,10000,0));
                }
                result.outputs.put(ItemRecipeCapability.CAP,outputs);return result;
            }
        }
        if(path.equals("macerator/macerate_amber_refined_ore_to_dust"))return replace(original,1,new ItemStack(TCItems.material(MaterialType.SALIS_MUNDUS)));
        if(path.equals("thermal_centrifuge/centrifuge_amber_purified_ore_to_refined_ore")
                ||path.equals("thermal_centrifuge/centrifuge_amber_crushed_ore_to_refined_ore"))return replace(original,1,dust(GTMaterials.Cinnabar));
        return original;
    }
    private static ItemStack dust(Material material){return ChemicalHelper.get(TagPrefix.dust,material);}
    private static GTRecipe replace(GTRecipe original,int index,ItemStack item) {
        var outputs=original.outputs.get(ItemRecipeCapability.CAP);
        if(outputs==null||outputs.size()<=index||item.isEmpty())return original;
        var result=original.copy();var replaced=new ArrayList<>(result.outputs.get(ItemRecipeCapability.CAP));var previous=outputs.get(index);
        replaced.set(index,new Content(ItemRecipeCapability.CAP.of(item),previous.chance,previous.maxChance,previous.tierChanceBoost));
        result.outputs.put(ItemRecipeCapability.CAP,replaced);return result;
    }
    private ArcaneOreProcessing() {}
}
