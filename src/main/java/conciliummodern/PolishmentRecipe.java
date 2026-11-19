package conciliummodern;

import com.google.gson.JsonObject;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.core.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;
import tc4revival.aspect.TCAspects;

/** Additional, reloadable completion requirement for an infusion result. */
public class PolishmentRecipe implements Recipe<SimpleContainer> {
    public static final RegistryObject<RecipeType<PolishmentRecipe>> TYPE=ChainedRiftRecipe.TYPES.register("polishment",()->new RecipeType<>(){});
    public static final RegistryObject<RecipeSerializer<PolishmentRecipe>> SERIALIZER=ChainedRiftRecipe.SERIALIZERS.register("polishment",Serializer::new);
    private final ResourceLocation id;
    private final Ingredient result;
    private final String aspect;
    private final int amount;
    public PolishmentRecipe(ResourceLocation id,Ingredient result,String aspect,int amount){
        if(result.isEmpty() || amount<=0)throw new IllegalArgumentException("Invalid polishment recipe "+id);
        TCAspects.catalog().require(aspect);
        this.id=id;this.result=result;this.aspect=aspect;this.amount=amount;
    }
    public String aspect(){return aspect;}
    public int amount(){return amount;}
    public boolean matches(ItemStack stack){return result.test(stack);}
    public static Optional<PolishmentRecipe> find(Level level,ItemStack stack){
        return level.getRecipeManager().getAllRecipesFor(TYPE.get()).stream().filter(r->r.matches(stack)).min(Comparator.comparing(PolishmentRecipe::getId));
    }
    @Override public boolean matches(SimpleContainer input,Level level){return input.getContainerSize()>0&&matches(input.getItem(0));}
    // This is a requirement, not an alternate crafting method or a way to duplicate the input.
    @Override public ItemStack assemble(SimpleContainer input,RegistryAccess access){return ItemStack.EMPTY;}
    @Override public boolean canCraftInDimensions(int width,int height){return false;}
    @Override public ItemStack getResultItem(RegistryAccess access){var items=result.getItems();return items.length==0?ItemStack.EMPTY:items[0].copy();}
    @Override public NonNullList<Ingredient> getIngredients(){return NonNullList.of(Ingredient.EMPTY,result);}
    @Override public ResourceLocation getId(){return id;}
    @Override public RecipeSerializer<?> getSerializer(){return SERIALIZER.get();}
    @Override public RecipeType<?> getType(){return TYPE.get();}
    @Override public boolean isSpecial(){return true;}
    private static class Serializer implements RecipeSerializer<PolishmentRecipe>{
        @Override public PolishmentRecipe fromJson(ResourceLocation id,JsonObject json){return new PolishmentRecipe(id,Ingredient.fromJson(json.get("result")),GsonHelper.getAsString(json,"aspect"),GsonHelper.getAsInt(json,"amount"));}
        @Override public PolishmentRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buffer){return new PolishmentRecipe(id,Ingredient.fromNetwork(buffer),buffer.readUtf(),buffer.readVarInt());}
        @Override public void toNetwork(FriendlyByteBuf buffer,PolishmentRecipe recipe){recipe.result.toNetwork(buffer);buffer.writeUtf(recipe.aspect);buffer.writeVarInt(recipe.amount);}
    }
}
