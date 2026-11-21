package tc4revival.content;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.TCAspects;

/** Dynamic recipe costs and the captured output item, including TC4's signed-byte hardening. */
public  class RunicAugmentation {
    private RunicAugmentation() {}
    public static boolean eligible(ItemStack stack) { return !stack.isEmpty() && stack.getItem() instanceof RunicGear gear && gear.runicEnabled(stack); }
    private static InfusionIngredient item(String id) {
        var ingredient=new com.google.gson.JsonObject(); ingredient.addProperty("item",id);
        var entry=new com.google.gson.JsonObject(); entry.add("ingredient",ingredient); return new InfusionIngredient(entry);
    }
    public static List<InfusionIngredient> components(ItemStack stack) {
        var result=new ArrayList<InfusionIngredient>(); result.add(item("minecraft:diamond"));
        for (int count=0;count<=Math.max(0,RunicGear.charge(stack));count++) result.add(item("thaumcraft:salis_mundus"));
        return List.copyOf(result);
    }
    public static InfusionRecipe.Plan plan(ItemStack stack) {
        int charge=RunicGear.charge(stack);
        // Java's floating-to-int saturation is also what the original exponential cost used.
        int vis=(int)(32*Math.pow(2,charge)); var aspects=new AspectAmounts();
        if (vis>0) aspects.add(TCAspects.catalog().require("tutamen"),vis/2).add(TCAspects.catalog().require("praecantatio"),vis/2).add(TCAspects.catalog().require("potentia"),vis);
        var result=stack.copy(); result.getOrCreateTag().putByte("RS.HARDEN",(byte)(RunicGear.hardening(stack)+1));
        return new InfusionRecipe.Plan(components(stack),aspects,5+charge/2,0,false,InfusionOutput.item(result));
    }
}
