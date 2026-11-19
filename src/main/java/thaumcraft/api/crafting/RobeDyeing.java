package thaumcraft.api.crafting;

import java.util.*;
import java.util.function.Predicate;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.util.ExtensionRegistry;

/** Pure dye plans built from copied stacks. Empty Optional means vanilla may handle the grid. */
public final class RobeDyeing {
    public record CraftingGridSnapshot(List<ItemStack> items) {
        public CraftingGridSnapshot { items=items.stream().map(ItemStack::copy).toList(); }
        @Override public List<ItemStack> items(){return items.stream().map(ItemStack::copy).toList();}
        public static CraftingGridSnapshot of(CraftingContainer grid){
            var items=new ArrayList<ItemStack>();for(int i=0;i<grid.getContainerSize();i++)items.add(grid.getItem(i));
            return new CraftingGridSnapshot(items);
        }
    }
    public record DyePlan(ItemStack output) {
        public DyePlan {output=output.copy();}
        @Override public ItemStack output(){return output.copy();}
        public boolean matches(){return !output.isEmpty();}
    }
    public interface RobeDyeAdapter {
        int color(ItemStack robe);
        /** Palette RGB, or null if this stack is not an accepted dye. */
        Integer dyeColor(ItemStack stack);
        int blend(List<Integer> colors);
        /** Called on a count-one copy; must preserve other NBT. */
        ItemStack withColor(ItemStack robe,int color);
    }
    private record Registration(Predicate<ItemStack> selector,RobeDyeAdapter adapter) {}
    private static final ExtensionRegistry<Registration> ROBES=new ExtensionRegistry<>();
    private RobeDyeing() {}
    public static void registerRobe(String id,int priority,Predicate<ItemStack> selector,RobeDyeAdapter adapter){
        ROBES.register(id,priority,new Registration(Objects.requireNonNull(selector),Objects.requireNonNull(adapter)));
    }
    public static Optional<DyePlan> planDye(CraftingGridSnapshot grid){
        var items=grid.items();Registration selected=null;ItemStack robe=ItemStack.EMPTY;
        for(var stack:items)if(!stack.isEmpty())for(var entry:ROBES.entries())if(entry.value().selector().test(stack.copy())){
            if(selected!=null)return Optional.of(new DyePlan(ItemStack.EMPTY));
            selected=entry.value();robe=stack;break;
        }
        if(selected==null)return Optional.empty();
        var colors=new ArrayList<Integer>();colors.add(selected.adapter().color(robe.copy()));
        for(var stack:items)if(!stack.isEmpty()&&stack!=robe){
            var color=selected.adapter().dyeColor(stack.copy());
            if(color==null)return Optional.of(new DyePlan(ItemStack.EMPTY));
            colors.add(color);
        }
        if(colors.size()<2)return Optional.of(new DyePlan(ItemStack.EMPTY));
        return Optional.of(new DyePlan(selected.adapter().withColor(robe.copyWithCount(1),selected.adapter().blend(List.copyOf(colors)))));
    }
}
