package ttmodern;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** A runtime config gate, so server config changes never leave a craftable disabled recipe. */
public final class ShareBookRecipe extends ShapedRecipe {
    public ShareBookRecipe(ResourceLocation id,CraftingBookCategory category) {
        super(id,"",category,3,3,displayIngredients(),new ItemStack(Tinkerer.SHARE_BOOK.get()));
    }
    private static net.minecraft.core.NonNullList<Ingredient> displayIngredients() {
        var ingredients=net.minecraft.core.NonNullList.withSize(9,Ingredient.EMPTY);
        ingredients.set(1,Ingredient.of(tc4revival.content.TCItems.SCRIBING_TOOLS.get()));
        for(int slot:new int[]{3,5,7})ingredients.set(slot,Ingredient.of(Items.PAPER));
        ingredients.set(4,Ingredient.of(tc4revival.content.TCItems.THAUMONOMICON.get()));
        return ingredients;
    }
    @Override public boolean isSpecial() {return !TinkererConfig.SHARE_TOME.get();}
    @Override public boolean matches(CraftingContainer grid,Level level) {
        if(!TinkererConfig.SHARE_TOME.get()||grid.getWidth()!=3||grid.getHeight()!=3)return false;
        for(int i=0;i<9;i++) {
            var stack=grid.getItem(i);
            boolean valid=switch(i){case 1->stack.is(tc4revival.content.TCItems.SCRIBING_TOOLS.get());case 3,5,7->stack.is(Items.PAPER);case 4->stack.is(tc4revival.content.TCItems.THAUMONOMICON.get());default->stack.isEmpty();};
            if(!valid)return false;
        }
        return true;
    }
    @Override public ItemStack assemble(CraftingContainer grid,RegistryAccess access) { return new ItemStack(Tinkerer.SHARE_BOOK.get()); }
    @Override public boolean canCraftInDimensions(int width,int height) { return width>=3&&height>=3; }
    @Override public RecipeSerializer<?> getSerializer() { return Tinkerer.SHARE_BOOK_RECIPE.get(); }
}
