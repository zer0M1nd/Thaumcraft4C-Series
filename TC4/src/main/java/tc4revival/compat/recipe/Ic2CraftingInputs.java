package tc4revival.compat.recipe;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Inspect IC2's native predicates without expanding every registered fluid container. */
public final class Ic2CraftingInputs {
    public static boolean hasFluidInput(Object recipe) {
        if (!isType(recipe, "ic2.core.recipe.AdvRecipe")
                && !isType(recipe, "ic2.core.recipe.AdvShapelessRecipe")) return false;
        try {
            Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            for (Object input : (Object[]) recipe.getClass().getField("input").get(recipe)) {
                if (fluidInput(input, seen)) return true;
            }
            return false;
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot inspect IC2 crafting input predicates", error);
        }
    }

    private static boolean fluidInput(Object input, Set<Object> seen) throws ReflectiveOperationException {
        if (input == null || !seen.add(input)) return false;
        if (isType(input, "ic2.core.recipe.input.RecipeInputFluidContainer")) return true;
        if (isType(input, "ic2.core.recipe.input.RecipeInputMultiple")) {
            for (Object alternative : (Object[]) input.getClass().getField("inputs").get(input)) {
                if (fluidInput(alternative, seen)) return true;
            }
        }
        return false;
    }

    private static boolean isType(Object value, String name) {
        if (value == null) return false;
        for (Class<?> type = value.getClass(); type != null; type = type.getSuperclass()) {
            if (type.getName().equals(name)) return true;
        }
        return false;
    }

    private Ic2CraftingInputs() {}
}
