package tmmodern;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
public class VoidBloodRecipe extends CustomRecipe {
    public VoidBloodRecipe(ResourceLocation id,CraftingBookCategory category){super(id,category);}
    public boolean matches(CraftingContainer grid,Level level){int armor=0,blood=0;for(int n=0;n<grid.getContainerSize();n++){var s=grid.getItem(n);if(s.isEmpty())continue;if(s.is(TaintedMagic.item("void_blood")))blood++;else if(s.getItem() instanceof ArmorItem&&(!s.hasTag()||!s.getTag().getBoolean("voidtouched")))armor++;else return false;}return armor==1&&blood==1;}
    public ItemStack assemble(CraftingContainer grid,RegistryAccess access){for(int n=0;n<grid.getContainerSize();n++)if(grid.getItem(n).getItem() instanceof ArmorItem){var out=grid.getItem(n).copy();out.setCount(1);out.getOrCreateTag().putBoolean("voidtouched",true);return out;}return ItemStack.EMPTY;}
    public boolean canCraftInDimensions(int width,int height){return width*height>=2;}
    public RecipeSerializer<?> getSerializer(){return TaintedMagic.VOID_RECIPE.get();}
}
