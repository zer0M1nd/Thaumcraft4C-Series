package thaumcraft.api.aspects;

import java.util.Objects;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import tc4revival.aspect.AspectAmounts;
import thaumcraft.api.util.ExtensionRegistry;

/** Pure production planning. Does not change scanning, storage or transport queries. */
public class AspectProductionRules {
    public record Context(String producer, ItemStack item, FluidStack fluid) {
        public Context { Objects.requireNonNull(producer);item=item.copy();fluid=fluid.copy(); }
        @Override public ItemStack item(){return item.copy();}
        @Override public FluidStack fluid(){return fluid.copy();}
    }
    @FunctionalInterface public interface Rule { AspectAmounts apply(Context context, AspectAmounts proposed); }
    private static final ExtensionRegistry<Rule> RULES=new ExtensionRegistry<>();
    public static void register(String id,int priority,Rule rule){RULES.register(id,priority,rule);}
    public static AspectAmounts plan(Context context,AspectAmounts proposed) {
        var result=proposed.copy();
        for(var rule:RULES.entries())result=Objects.requireNonNull(rule.value().apply(context,result.copy())).copy();
        return result;
    }
    public static AspectAmounts item(String producer,ItemStack input,AspectAmounts proposed) {
        return plan(new Context(producer,input,FluidStack.EMPTY),proposed);
    }
    public static AspectAmounts fluid(String producer,FluidStack input,AspectAmounts proposed) {
        return plan(new Context(producer,ItemStack.EMPTY,input),proposed);
    }
    private AspectProductionRules(){}
}
