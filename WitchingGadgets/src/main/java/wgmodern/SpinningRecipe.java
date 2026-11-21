package wgmodern;

import java.util.*;
import com.google.gson.JsonObject;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Unordered distinct-slot matching; duplicate ingredients cannot reuse the same input. */
public class SpinningRecipe implements Recipe<Container> {
    private final ResourceLocation id;private final NonNullList<Ingredient> inputs;private final ItemStack result;private final int duration;
    public SpinningRecipe(ResourceLocation id,NonNullList<Ingredient> inputs,ItemStack result,int duration){this.id=id;this.inputs=inputs;this.result=result;this.duration=duration;if(inputs.isEmpty()||inputs.size()>5||duration<1)throw new IllegalArgumentException("Invalid spinning recipe");}
    public int duration(){return duration;}
    public static boolean matchMatrix(boolean[][] matrix){if(matrix.length==0)return false;return match(matrix,0,new boolean[matrix.length]);}
    private static boolean match(boolean[][] matrix,int i,boolean[] used){if(i==matrix.length)return true;for(int j=0;j<matrix.length;j++)if(!used[j]&&matrix[i][j]){used[j]=true;if(match(matrix,i+1,used))return true;used[j]=false;}return false;}
    @Override public boolean matches(Container inv,Level level){var stacks=new ArrayList<ItemStack>();for(int i=0;i<Math.min(5,inv.getContainerSize());i++)if(!inv.getItem(i).isEmpty())stacks.add(inv.getItem(i));if(stacks.size()!=inputs.size())return false;var matrix=new boolean[stacks.size()][stacks.size()];for(int i=0;i<stacks.size();i++)for(int j=0;j<inputs.size();j++)matrix[i][j]=inputs.get(j).test(stacks.get(i));return matchMatrix(matrix);}
    @Override public ItemStack assemble(Container inv,RegistryAccess registry){return result.copy();}
    @Override public boolean canCraftInDimensions(int x,int y){return x*y>=inputs.size();}
    @Override public ItemStack getResultItem(RegistryAccess registry){return result;}
    @Override public ResourceLocation getId(){return id;}
    @Override public RecipeSerializer<?> getSerializer(){return Content.SPINNING_SERIALIZER.get();}
    @Override public RecipeType<?> getType(){return Content.SPINNING.get();}
    @Override public NonNullList<Ingredient> getIngredients(){return inputs;}
    public static class Serializer implements RecipeSerializer<SpinningRecipe>{
        public SpinningRecipe fromJson(ResourceLocation id,JsonObject json){var inputs=NonNullList.<Ingredient>create();for(var x:json.getAsJsonArray("ingredients"))inputs.add(Ingredient.fromJson(x));return new SpinningRecipe(id,inputs,ShapedRecipe.itemStackFromJson(json.getAsJsonObject("result")),json.has("duration")?json.get("duration").getAsInt():120);}
        public SpinningRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buf){var inputs=NonNullList.<Ingredient>create();int size=buf.readVarInt();if(size<1||size>5)throw new IllegalArgumentException("Invalid spinning input count");for(int i=0;i<size;i++)inputs.add(Ingredient.fromNetwork(buf));return new SpinningRecipe(id,inputs,buf.readItem(),buf.readVarInt());}
        public void toNetwork(FriendlyByteBuf buf,SpinningRecipe recipe){buf.writeVarInt(recipe.inputs.size());recipe.inputs.forEach(i->i.toNetwork(buf));buf.writeItem(recipe.result);buf.writeVarInt(recipe.duration);}
    }
}
