package heartwork.recipe;

import heartwork.Heartwork;
import heartwork.content.SwissItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public class SwissRepairRecipe extends CustomRecipe {
    public SwissRepairRecipe(ResourceLocation id, CraftingBookCategory category) { super(id, category); }
    @Override public boolean matches(CraftingContainer grid, Level level) {
        if (!canCraftInDimensions(grid.getWidth(), grid.getHeight())) return false;
        ItemStack stone = grid.getItem(1), knife = grid.getItem(grid.getWidth());
        if (!stone.is(Heartwork.item("hone")) || !(knife.getItem() instanceof SwissItem swiss) || swiss.mode().equals("knife") || !knife.isDamaged()) return false;
        for (int i=0; i<grid.getContainerSize(); i++) if (i != 1 && i != grid.getWidth() && !grid.getItem(i).isEmpty()) return false;
        return true;
    }
    @Override public ItemStack assemble(CraftingContainer grid, RegistryAccess registries) {
        ItemStack result = grid.getItem(grid.getWidth()).copyWithCount(1);
        result.setDamageValue(0);
        return result;
    }
    @Override public boolean canCraftInDimensions(int width, int height) { return width >= 2 && height >= 2; }
    @Override public RecipeSerializer<?> getSerializer() { return Heartwork.SWISS_REPAIR.get(); }
}
