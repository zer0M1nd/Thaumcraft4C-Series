package thaumicomniverse.compat.gregtech;

import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.integration.xei.entry.item.*;
import com.gregtechceu.gtceu.integration.xei.entry.fluid.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

/** GT's diagram normally synthesizes outputs from OreProperty, bypassing recipe changes. */
public final class ArcaneOreDiagram {
    public static boolean apply(Material material,RecipeManager manager,List<ItemEntryList> inputs,List<ItemStack> outputs,
                                List<FluidEntryList> fluids,Int2ObjectMap<Content> chances) {
        if(!thaumicomniverse.RevivalFeatures.Group.GREGTECH.enabled()
                ||material!=ArcaneMaterials.AMBER&&!ArcaneMaterials.INFUSED.containsValue(material))return false;
        String name=material.getName();
        copy(manager,"gtceu:macerator/macerate_"+name+"_refined_ore_to_dust",11,2,outputs,chances,inputs.size());
        copy(manager,"gtceu:macerator/macerate_"+name+"_crushed_ore_to_dust",13,2,outputs,chances,inputs.size());
        copy(manager,"gtceu:thermal_centrifuge/centrifuge_"+name+"_purified_ore_to_refined_ore",9,2,outputs,chances,inputs.size());
        if(ArcaneMaterials.INFUSED.containsValue(material))
            copy(manager,"gtceu:sifter/sift_"+name+"_purified_ore_to_gems",28,6,outputs,chances,inputs.size());
        String bath="thaumicomniverse:chemical_bath/gt/bathe_"+name+(material==ArcaneMaterials.AMBER?"_liquid_death":"_purifying_water");
        var recipe=recipe(manager,bath);
        if(recipe==null)return false;
        copy(manager,bath,23,2,outputs,chances,inputs.size());
        inputs.set(13,ItemStackList.of(GTMachines.CHEMICAL_BATH[1].asStack()));
        var content=recipe.getInputContents(FluidRecipeCapability.CAP);
        if(!content.isEmpty())fluids.set(1,FluidStackList.of(List.of(FluidRecipeCapability.CAP.of(content.get(0).content).getStacks())));
        return true;
    }
    private static GTRecipe recipe(RecipeManager manager,String id) {
        return manager.byKey(ResourceLocation.parse(id)).filter(GTRecipe.class::isInstance).map(GTRecipe.class::cast).orElse(null);
    }
    private static void copy(RecipeManager manager,String id,int start,int count,List<ItemStack> outputs,Int2ObjectMap<Content> chances,int offset) {
        var recipe=recipe(manager,id);if(recipe==null)return;
        var contents=recipe.getOutputContents(ItemRecipeCapability.CAP);
        for(int i=0;i<count;i++) {
            int slot=start+i;chances.remove(offset+slot);outputs.set(slot,ItemStack.EMPTY);
            if(i>=contents.size())continue;
            var content=contents.get(i);var stacks=ItemRecipeCapability.CAP.of(content.content).getItems();
            if(stacks.length>0)outputs.set(slot,stacks[0].copy());
            if(content.chance<content.maxChance)chances.put(offset+slot,content);
        }
    }
    private ArcaneOreDiagram() {}
}
