package automagy.content;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
/** Ordinary shapeless recipe whose output keeps the entire portable golem payload. */
public final class AdvancedGolemRecipe extends CustomRecipe {
 public AdvancedGolemRecipe(ResourceLocation id,CraftingBookCategory category){super(id,category);}
 private static ItemStack golem(CraftingContainer grid){ItemStack found=ItemStack.EMPTY;boolean cap=false;for(int i=0;i<grid.getContainerSize();i++){var stack=grid.getItem(i);if(stack.isEmpty())continue;if(stack.is(Content.THINKING_CAP.get())&&!cap){cap=true;continue;}if(stack.getItem() instanceof tc4revival.content.GolemPlacerItem&&found.isEmpty()){found=stack;continue;}return ItemStack.EMPTY;}return cap?found:ItemStack.EMPTY;}
 @Override public boolean matches(CraftingContainer grid,Level level){return automagy.AutomagyConfig.THINKING_CAP_RECIPE.get()&&!golem(grid).isEmpty();}
 @Override public ItemStack assemble(CraftingContainer grid,RegistryAccess registries){var found=golem(grid);if(found.isEmpty())return ItemStack.EMPTY;var result=found.copyWithCount(1);result.getOrCreateTag().putByte("advanced",(byte)1);return result;}
 @Override public boolean canCraftInDimensions(int width,int height){return width*height>=2;}
 @Override public RecipeSerializer<?> getSerializer(){return Content.ADVANCED_GOLEM_RECIPE.get();}
}
