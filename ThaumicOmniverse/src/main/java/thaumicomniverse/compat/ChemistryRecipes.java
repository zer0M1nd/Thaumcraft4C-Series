package thaumicomniverse.compat;

import com.smashingmods.alchemistry.common.recipe.dissolver.DissolverRecipe;
import com.smashingmods.alchemylib.api.item.IngredientStack;
import java.util.ArrayList;
import java.util.Arrays;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.registries.ForgeRegistries;

/** Keep the upstream ore recipe for other cinnabar while preserving the fictional TC formula. */
public final class ChemistryRecipes {
    private static final ResourceLocation GENERIC=ResourceLocation.parse("alchemistry:dissolver/ores/cinnabar");
    private static final ResourceLocation FICTIONAL=ResourceLocation.parse("thaumicomniverse:expansion/chemistry/cinnabar_dissolving");
    public static void apply(RecipeManager manager) {
        if(!thaumicomniverse.RevivalFeatures.Group.CHEMISTRY.enabled())return;
        var recipe=manager.byKey(GENERIC).orElse(null);
        if(!(recipe instanceof DissolverRecipe original)||manager.byKey(FICTIONAL).isEmpty())return;
        var cinnabar=ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("thaumcraft:cinnabar_ore"));
        if(cinnabar==null||!original.matches(new ItemStack(cinnabar)))return;
        var alternatives=Arrays.stream(original.getInput().getIngredient().getItems()).filter(stack->!stack.is(cinnabar)).toArray(ItemStack[]::new);
        var recipes=new ArrayList<Recipe<?>>();
        for(var entry:manager.getRecipes())if(!entry.getId().equals(GENERIC))recipes.add(entry);
        // AlchemyLib requires ordinary Ingredient values; custom difference ingredients have none.
        if(alternatives.length>0)recipes.add(new DissolverRecipe(GENERIC,original.getGroup(),new IngredientStack(Ingredient.of(alternatives),original.getInput().getCount()),original.getOutput().copy()));
        manager.replaceRecipes(recipes);
    }
    private ChemistryRecipes(){}
}
