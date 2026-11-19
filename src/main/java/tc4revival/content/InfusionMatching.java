package tc4revival.content;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.RecipeMatcher;

/** Forge's complete assignment avoids consuming a narrow ingredient for a broad tag. */
public class InfusionMatching {
    private InfusionMatching() {}

    public static int[] assignment(List<ItemStack> inputs,List<InfusionIngredient> required,boolean allowExtras) {
        if(inputs.size()<required.size() || !allowExtras && inputs.size()!=required.size())return null;
        var predicates=new ArrayList<Predicate<ItemStack>>();
        for(var ingredient:required)predicates.add(ingredient::matches);
        while(predicates.size()<inputs.size())predicates.add(stack->true);
        return RecipeMatcher.findMatches(inputs,predicates);
    }
}
