package ttmodern;

import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public final class SpellClothRecipe extends CustomRecipe {
    public static final net.minecraft.tags.TagKey<Item> IMMUNE=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,ResourceLocation.parse(Tinkerer.ID+":spell_cloth_immune"));
    public SpellClothRecipe(ResourceLocation id,CraftingBookCategory category){super(id,category);}
    @Override public boolean matches(CraftingContainer grid,Level level){int cloth=0,enchanted=0;for(int i=0;i<grid.getContainerSize();i++){var stack=grid.getItem(i);if(stack.isEmpty())continue;if(stack.is(Tinkerer.SPELL_CLOTH.get())&&stack.getDamageValue()<35)cloth++;else if(stack.isEnchanted()&&!stack.is(IMMUNE)&&stack.getCount()==1)enchanted++;else return false;}return cloth==1&&enchanted==1;}
    @Override public ItemStack assemble(CraftingContainer grid,RegistryAccess registries){for(int i=0;i<grid.getContainerSize();i++){var stack=grid.getItem(i);if(!stack.is(Tinkerer.SPELL_CLOTH.get())&&stack.isEnchanted()&&!stack.is(IMMUNE)){var result=stack.copy();result.setCount(1);result.removeTagKey("Enchantments");return result;}}return ItemStack.EMPTY;}
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingContainer grid){var result=NonNullList.withSize(grid.getContainerSize(),ItemStack.EMPTY);for(int i=0;i<grid.getContainerSize();i++){var stack=grid.getItem(i);if(stack.is(Tinkerer.SPELL_CLOTH.get())&&stack.getDamageValue()+1<35){var cloth=stack.copy();cloth.setCount(1);cloth.setDamageValue(cloth.getDamageValue()+1);result.set(i,cloth);}}return result;}
    @Override public boolean canCraftInDimensions(int width,int height){return width*height>=2;}
    @Override public RecipeSerializer<?> getSerializer(){return Tinkerer.SPELL_CLOTH_RECIPE.get();}
}
