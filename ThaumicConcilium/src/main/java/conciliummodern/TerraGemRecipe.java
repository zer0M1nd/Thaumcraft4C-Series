package conciliummodern;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Exactly one charged earth sample and one active rift gem; inputs remain untouched by preview. */
public final class TerraGemRecipe extends CustomRecipe {
    public TerraGemRecipe(ResourceLocation id,CraftingBookCategory category){super(id,category);}
    private record Inputs(ItemStack rift,GemCoordinates.Sample sample){}
    private static Inputs inputs(CraftingContainer grid){
        ItemStack rift=ItemStack.EMPTY,terra=ItemStack.EMPTY;int rifts=0,earth=0,others=0;
        for(int i=0;i<grid.getContainerSize();i++){
            var stack=grid.getItem(i);if(stack.isEmpty())continue;
            if(stack.is(ConciliumGems.RIFT.get())){rift=stack;rifts++;}
            else if(stack.is(ConciliumGems.TERRA.get())){terra=stack;earth++;}
            else others++;
        }
        var sample=GemCoordinates.captured(terra);
        return GemRules.canBind(rifts,earth,others,GemCoordinates.samples(rift).size(),sample!=null)?new Inputs(rift,sample):null;
    }
    @Override public boolean matches(CraftingContainer grid,Level level){return ConciliumGems.available()&&inputs(grid)!=null;}
    @Override public ItemStack assemble(CraftingContainer grid,RegistryAccess access){var inputs=inputs(grid);if(inputs==null)return ItemStack.EMPTY;var result=inputs.rift.copyWithCount(1);GemCoordinates.append(result,inputs.sample);return result;}
    @Override public boolean canCraftInDimensions(int width,int height){return width*height>=2;}
    @Override public RecipeSerializer<?> getSerializer(){return ConciliumGems.COMBINE.get();}
}
