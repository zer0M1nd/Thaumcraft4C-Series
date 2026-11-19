package thaumicfutures.recipe;
import thaumicfutures.FutureRecipes;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

/** Keep enchantments, damage, ancient wills and phantom ink when upgrading a helmet. */
public final class RevealingHelmetRecipe extends ShapelessRecipe {
    public RevealingHelmetRecipe(ResourceLocation id, String group, CraftingBookCategory category,
                                 ItemStack output, NonNullList<Ingredient> ingredients) {
        super(id, group, category, output, ingredients);
    }
    @Override public ItemStack assemble(CraftingContainer grid, RegistryAccess access) {
        ItemStack result = super.assemble(grid, access);
        for (int slot = 0; slot < grid.getContainerSize(); slot++) {
            ItemStack input = grid.getItem(slot);
            if (input.getItem() instanceof ArmorItem && input.getItem() != tc4revival.content.TCItems.GOGGLES.get()) {
                if (input.hasTag()) result.setTag(input.getTag().copy());
                break;
            }
        }
        return result;
    }
    @Override public RecipeSerializer<?> getSerializer() { return FutureRecipes.REVEALING_HELMET.get(); }
    public static final class Serializer implements RecipeSerializer<RevealingHelmetRecipe> {
        private final ShapelessRecipe.Serializer delegate = new ShapelessRecipe.Serializer();
        private RevealingHelmetRecipe wrap(ShapelessRecipe recipe) {
            return new RevealingHelmetRecipe(recipe.getId(), recipe.getGroup(), recipe.category(),
                    recipe.getResultItem(RegistryAccess.EMPTY), recipe.getIngredients());
        }
        @Override public RevealingHelmetRecipe fromJson(ResourceLocation id, com.google.gson.JsonObject json) { return wrap(delegate.fromJson(id, json)); }
        @Override public RevealingHelmetRecipe fromNetwork(ResourceLocation id, net.minecraft.network.FriendlyByteBuf buf) { return wrap(delegate.fromNetwork(id, buf)); }
        @Override public void toNetwork(net.minecraft.network.FriendlyByteBuf buf, RevealingHelmetRecipe recipe) { delegate.toNetwork(buf, recipe); }
    }
}
