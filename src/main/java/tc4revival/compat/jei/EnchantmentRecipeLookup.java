package tc4revival.compat.jei;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import tc4revival.research.BrowserRecipe;

/** Level-independent book focus scoped to infusion enchantments; other JEI categories keep their normal identity. */
final class EnchantmentRecipeLookup implements ISimpleRecipeManagerPlugin<BrowserRecipe> {
    private final Supplier<List<BrowserRecipe>> recipes;
    EnchantmentRecipeLookup(Supplier<List<BrowserRecipe>> recipes){this.recipes=recipes;}
    private static Set<Enchantment> enchantments(ITypedIngredient<?> ingredient){
        return ingredient.getItemStack().filter(stack->stack.is(Items.ENCHANTED_BOOK))
            .map(stack->EnchantmentHelper.getEnchantments(stack).keySet()).orElse(Set.of());
    }
    @Override public boolean isHandledInput(ITypedIngredient<?> ingredient){return false;}
    @Override public boolean isHandledOutput(ITypedIngredient<?> ingredient){return !getRecipesForOutput(ingredient).isEmpty();}
    @Override public List<BrowserRecipe> getRecipesForInput(ITypedIngredient<?> ingredient){return List.of();}
    @Override public List<BrowserRecipe> getRecipesForOutput(ITypedIngredient<?> ingredient){
        var enchantments=enchantments(ingredient);if(enchantments.isEmpty())return List.of();
        return recipes.get().stream().filter(recipe->recipe.kind().equals("infusion_enchantment"))
            .filter(recipe->EnchantmentHelper.getEnchantments(recipe.output()).keySet().stream().anyMatch(enchantments::contains)).toList();
    }
    @Override public List<BrowserRecipe> getAllRecipes(){return List.of();}
}
