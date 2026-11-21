package tc4revival.content;

import java.util.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Requested, deterministic recipe hints; never realizes a random output or changes the altar. */
public class InfusionPreview {
    public enum Mark { PRESENT, MISSING, EXTRA }
    public record Item(ItemStack stack, Mark mark) {
        public Item { stack=stack.copyWithCount(1); }
    }
    public record Match(InfusionRecipe recipe, InfusionRecipe.Plan plan, List<Item> items, int distance) {
        public Match { items=List.copyOf(items); }
    }
    public static Optional<Match> find(Level level, ItemStack center, List<ItemStack> actual, boolean ordered) {
        if (center.isEmpty() && actual.isEmpty()) return Optional.empty();
        Match best=null;
        // Same priority as crafting for distance-zero ties, stable for every other tie as well.
        var recipes=level.getRecipeManager().getAllRecipesFor(TCArcane.INFUSION_RECIPE_TYPE.get()).stream()
                .filter(InfusionRecipe::enabled).sorted(InfusionRecipe.priority()).toList();
        for (var recipe : recipes) {
            boolean centerMatches=recipe.acceptsCenter(center);
            if (!centerMatches && recipe.dynamicCenter()) continue;
            var sample=centerMatches?center:recipe.centerSamples().stream().filter(recipe::acceptsCenter).findFirst().orElse(ItemStack.EMPTY);
            if (sample.isEmpty()) continue;
            InfusionRecipe.Plan plan;
            try {
                try { plan=recipe.plan(new InfusionRecipe.Input(sample,actual,key->true,ordered)); }
                catch (IllegalArgumentException|ArithmeticException incomplete) { plan=recipe.plan(sample); }
            } catch (IllegalArgumentException|ArithmeticException invalid) { continue; }
            int centerDistance=centerMatches?0:1, limit=best==null?Integer.MAX_VALUE:best.distance()-1;
            if (centerDistance+Math.abs(plan.components().size()-actual.size())>limit) continue;
            var matches=InfusionDistance.matches(plan.components(),actual,InfusionIngredient::matches);
            int distance=centerDistance+(ordered?InfusionDistance.ordered(matches,actual.size(),limit-centerDistance)
                    :InfusionDistance.unordered(matches,actual.size()));
            if (distance>limit) continue;
            best=new Match(recipe,plan,items(sample,center,actual,plan.components(),matches,centerMatches),distance);
            if (distance==0) break;
        }
        return Optional.ofNullable(best);
    }
    public static List<Item> checklist(InfusionRecipe recipe, InfusionRecipe.Plan plan, ItemStack originalCenter,
            ItemStack center, List<ItemStack> actual) {
        return items(originalCenter,center,actual,plan.components(),
                InfusionDistance.matches(plan.components(),actual,InfusionIngredient::matches),recipe.acceptsCenter(center));
    }
    private static List<Item> items(ItemStack sample,ItemStack center,List<ItemStack> actual,
            List<InfusionIngredient> expected,boolean[][] matches,boolean centerMatches) {
        var result=new ArrayList<Item>();
        result.add(new Item(sample,centerMatches?Mark.PRESENT:Mark.MISSING));
        int[] assignment=InfusionDistance.assignment(matches,actual.size());
        boolean[] used=new boolean[actual.size()];
        for (int i=0;i<expected.size();i++) {
            int j=assignment[i];
            if (j>=0) { used[j]=true; result.add(new Item(actual.get(j),Mark.PRESENT)); }
            else result.add(new Item(expected.get(i).displayStacks().stream().findFirst().orElse(ItemStack.EMPTY),Mark.MISSING));
        }
        if (!centerMatches && !center.isEmpty()) result.add(new Item(center,Mark.EXTRA));
        for (int j=0;j<actual.size();j++) if (!used[j]) result.add(new Item(actual.get(j),Mark.EXTRA));
        return List.copyOf(result);
    }
    private InfusionPreview() {}
}
