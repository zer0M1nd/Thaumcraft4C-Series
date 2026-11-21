package tc4revival.research;
import java.util.*;
import net.minecraft.world.item.ItemStack;
import tc4revival.aspect.AspectAmounts;

/** Side-independent presentation only. No JEI or client classes in the common data model. */
public record BrowserRecipe(String kind,String id,String research,List<List<ItemStack>> inputs,ItemStack output,
        Map<String,Integer> aspectInputs,Map<String,Integer> aspectOutputs,int instability,int xp,boolean dynamic) {
    public BrowserRecipe {
        if(!Set.of("arcane","crucible","infusion","infusion_enchantment","aspect_combine","item_aspects").contains(kind))throw new IllegalArgumentException("Unknown browser category");
        inputs=inputs.stream().map(group->group.stream().map(ItemStack::copy).toList()).toList();output=output.copy();
        aspectInputs=Map.copyOf(aspectInputs);aspectOutputs=Map.copyOf(aspectOutputs);
    }
    public static Map<String,Integer> amounts(AspectAmounts amounts){var out=new LinkedHashMap<String,Integer>();for(var a:amounts.alphabetical())out.put(a.tag(),amounts.amount(a));return out;}
}
