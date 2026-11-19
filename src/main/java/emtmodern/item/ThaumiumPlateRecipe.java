package emtmodern.item;

import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

/** Modern IC2's hammer no longer has a vanilla crafting remainder. Preserve the old one-use wear. */
public class ThaumiumPlateRecipe extends ShapedRecipe {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS,"emt");
    public static final RegistryObject<RecipeSerializer<ThaumiumPlateRecipe>> SERIALIZER=SERIALIZERS.register("thaumium_plate",Serializer::new);
    public static void register(IEventBus bus){SERIALIZERS.register(bus);}
    public ThaumiumPlateRecipe(ShapedRecipe recipe){super(recipe.getId(),recipe.getGroup(),recipe.category(),recipe.getWidth(),recipe.getHeight(),recipe.getIngredients(),recipe.getResultItem(net.minecraft.core.RegistryAccess.EMPTY));}
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingContainer grid){
        var result=super.getRemainingItems(grid);
        for(int i=0;i<grid.getContainerSize();i++){var stack=grid.getItem(i);if(stack.is(ic2.core.ref.Ic2Items.FORGE_HAMMER)){var hammer=stack.copyWithCount(1);hammer.setDamageValue(hammer.getDamageValue()+1);result.set(i,hammer.getDamageValue()<hammer.getMaxDamage()?hammer:ItemStack.EMPTY);}}
        return result;
    }
    @Override public RecipeSerializer<?> getSerializer(){return SERIALIZER.get();}
    public static class Serializer implements RecipeSerializer<ThaumiumPlateRecipe> {
        @Override public ThaumiumPlateRecipe fromJson(ResourceLocation id,JsonObject json){return new ThaumiumPlateRecipe(RecipeSerializer.SHAPED_RECIPE.fromJson(id,json));}
        @Override public ThaumiumPlateRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buffer){return new ThaumiumPlateRecipe(RecipeSerializer.SHAPED_RECIPE.fromNetwork(id,buffer));}
        @Override public void toNetwork(FriendlyByteBuf buffer,ThaumiumPlateRecipe recipe){RecipeSerializer.SHAPED_RECIPE.toNetwork(buffer,recipe);}
    }
}
