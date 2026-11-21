package dyesmodern;

import java.util.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.*;

public final class DyesRecipe extends CustomRecipe {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS,ThaumicDyes.ID);
    public static final RegistryObject<RecipeSerializer<DyesRecipe>> TYPE=SERIALIZERS.register("dyeing",()->new SimpleCraftingRecipeSerializer<>(DyesRecipe::new));
    public DyesRecipe(ResourceLocation id,CraftingBookCategory category){super(id,category);}
    @Override public boolean matches(CraftingContainer input,Level level){int armor=0,dyes=0;for(int i=0;i<input.getContainerSize();i++){var stack=input.getItem(i);if(stack.isEmpty())continue;if(stack.getItem() instanceof DyedArmorItem)armor++;else if(stack.getItem() instanceof DyeItem)dyes++;else return false;}return armor==1&&dyes>0;}
    @Override public ItemStack assemble(CraftingContainer input,RegistryAccess registries){
        ItemStack armor=ItemStack.EMPTY;var colors=new ArrayList<Integer>();
        for(int i=0;i<input.getContainerSize();i++){var stack=input.getItem(i);if(stack.isEmpty())continue;if(stack.getItem() instanceof DyedArmorItem){if(!armor.isEmpty())return ItemStack.EMPTY;armor=stack.copyWithCount(1);}else if(stack.getItem() instanceof DyeItem dye)colors.add(dye.getDyeColor().getId());else return ItemStack.EMPTY;}
        if(armor.isEmpty()||colors.isEmpty())return ItemStack.EMPTY;var item=(DyedArmorItem)armor.getItem();item.setColor(armor,LegacyDyeMixing.mix(item.getColor(armor),colors));return armor;
    }
    @Override public boolean canCraftInDimensions(int width,int height){return width*height>=2;}
    @Override public RecipeSerializer<?> getSerializer(){return TYPE.get();}
}
