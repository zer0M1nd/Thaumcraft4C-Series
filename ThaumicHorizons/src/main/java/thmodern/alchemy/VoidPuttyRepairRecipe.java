package thmodern.alchemy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import thmodern.Horizons;
public final class VoidPuttyRepairRecipe extends CustomRecipe {
 public VoidPuttyRepairRecipe(ResourceLocation id,CraftingBookCategory category){super(id,category);}
 private ItemStack target(CraftingContainer grid){ItemStack target=ItemStack.EMPTY;int putty=0;for(int i=0;i<grid.getContainerSize();i++){var stack=grid.getItem(i);if(stack.isEmpty())continue;if(stack.is(Horizons.VOID_PUTTY.get())){putty++;continue;}if(!target.isEmpty()||stack.getCount()!=1||!stack.isDamageableItem()||!stack.isDamaged()||!stack.getItem().isRepairable(stack))return ItemStack.EMPTY;target=stack;}return putty>0?target:ItemStack.EMPTY;}
 @Override public boolean matches(CraftingContainer grid,Level level){return !target(grid).isEmpty();}
 @Override public ItemStack assemble(CraftingContainer grid,net.minecraft.core.RegistryAccess access){var target=target(grid);if(target.isEmpty())return ItemStack.EMPTY;var result=target.copy();result.setDamageValue(0);return result;}
 @Override public boolean canCraftInDimensions(int width,int height){return width*height>=2;}
 @Override public RecipeSerializer<?> getSerializer(){return thmodern.focus.FocusRecipes.VOID_REPAIR.get();}
}
