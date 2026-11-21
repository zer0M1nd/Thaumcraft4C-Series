package thmodern.focus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import thmodern.Horizons;
public final class IlluminationDyeRecipe extends CustomRecipe {
    public IlluminationDyeRecipe(ResourceLocation id,CraftingBookCategory category){super(id,category);}
    @Override public boolean matches(CraftingContainer grid,Level level){int focuses=0,dyes=0;for(int i=0;i<grid.getContainerSize();i++){var stack=grid.getItem(i);if(stack.isEmpty())continue;if(stack.getItem() instanceof IlluminationFocusItem)focuses++;else if(stack.getItem() instanceof DyeItem)dyes++;else return false;}return focuses==1&&dyes>0;}
    @Override public ItemStack assemble(CraftingContainer grid,net.minecraft.core.RegistryAccess access){ItemStack focus=ItemStack.EMPTY;int shade=-1;for(int i=0;i<grid.getContainerSize();i++){var stack=grid.getItem(i);if(stack.isEmpty())continue;if(stack.getItem() instanceof IlluminationFocusItem){if(!focus.isEmpty())return ItemStack.EMPTY;focus=stack;}else if(stack.getItem() instanceof DyeItem dye)shade=15-dye.getDyeColor().getId();else return ItemStack.EMPTY;}if(focus.isEmpty()||shade<0)return ItemStack.EMPTY;var data=focus.copyWithCount(1).save(new net.minecraft.nbt.CompoundTag());data.putString("id",Horizons.ILLUMINATION_FOCI.get(shade).getId().toString());return ItemStack.of(data);}
    @Override public boolean canCraftInDimensions(int width,int height){return width*height>=2;}
    @Override public RecipeSerializer<?> getSerializer(){return FocusRecipes.DYE.get();}
}
