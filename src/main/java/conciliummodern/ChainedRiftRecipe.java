package conciliummodern;

import com.google.gson.JsonObject;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.*;
import tc4revival.aspect.*;

/** Reloadable bound-rift recipes. Catalyst count is explicit; extra dropped items survive. */
public final class ChainedRiftRecipe implements Recipe<SimpleContainer> {
    public static final DeferredRegister<RecipeType<?>> TYPES=DeferredRegister.create(ForgeRegistries.RECIPE_TYPES,ThaumicConcilium.ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS,ThaumicConcilium.ID);
    public static final RegistryObject<RecipeType<ChainedRiftRecipe>> TYPE=TYPES.register("chained_rift",()->new RecipeType<>(){});
    public static final RegistryObject<RecipeSerializer<ChainedRiftRecipe>> SERIALIZER=SERIALIZERS.register("chained_rift",Serializer::new);
    private final ResourceLocation id;
    private final String research;
    private final Ingredient catalyst;
    private final int count;
    private final ItemStack output;
    private final Map<String,Integer> amounts;
    private ChainedRiftRecipe(ResourceLocation id,String research,Ingredient catalyst,int count,ItemStack output,Map<String,Integer> amounts){
        if(count<1 || count>64 || output.isEmpty() || amounts.isEmpty() || amounts.size()>256)throw new IllegalArgumentException("Invalid rift recipe "+id);
        amounts.forEach((key,value)->{TCAspects.catalog().require(key);if(value<=0)throw new IllegalArgumentException("Invalid rift essentia amount");});
        this.id=id;this.research=research;this.catalyst=catalyst;this.count=count;this.output=output.copy();this.amounts=Map.copyOf(amounts);
    }
    public boolean matches(ItemStack stack,AspectAmounts stored,java.util.function.Predicate<String> knowledge){
        return (research.isEmpty()||knowledge.test(research)) && stack.getCount()>=count && catalyst.test(stack)
                && amounts.entrySet().stream().allMatch(e->stored.amount(TCAspects.catalog().require(e.getKey()))>=e.getValue());
    }
    public int catalystCount(){return count;}
    public String research(){return research;}
    public Map<String,Integer> aspectCosts(){return amounts;}
    public AspectAmounts removeMatching(AspectAmounts stored){var result=stored.copy();amounts.forEach((key,value)->result.remove(TCAspects.catalog().require(key),value));return result;}
    @Override public boolean matches(SimpleContainer input,Level level){return input.getContainerSize()>0 && input.getItem(0).getCount()>=count && catalyst.test(input.getItem(0));}
    @Override public ItemStack assemble(SimpleContainer input,RegistryAccess access){return output.copy();}
    @Override public boolean canCraftInDimensions(int width,int height){return width*height>=1;}
    @Override public ItemStack getResultItem(RegistryAccess access){return output.copy();}
    @Override public NonNullList<Ingredient> getIngredients(){return NonNullList.of(Ingredient.EMPTY,catalyst);}
    @Override public ResourceLocation getId(){return id;}
    @Override public RecipeSerializer<?> getSerializer(){return SERIALIZER.get();}
    @Override public RecipeType<?> getType(){return TYPE.get();}
    @Override public boolean isSpecial(){return true;}
    private static final class Serializer implements RecipeSerializer<ChainedRiftRecipe>{
        @Override public ChainedRiftRecipe fromJson(ResourceLocation id,JsonObject json){
            var amounts=new LinkedHashMap<String,Integer>();GsonHelper.getAsJsonObject(json,"aspects").entrySet().forEach(e->amounts.put(e.getKey(),e.getValue().getAsInt()));
            return new ChainedRiftRecipe(id,GsonHelper.getAsString(json,"research",""),Ingredient.fromJson(json.get("catalyst")),GsonHelper.getAsInt(json,"count",1),ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json,"result")),amounts);
        }
        @Override public ChainedRiftRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buffer){
            String research=buffer.readUtf();var catalyst=Ingredient.fromNetwork(buffer);int count=buffer.readVarInt();var output=buffer.readItem();int size=buffer.readVarInt();
            if(size<1||size>256)throw new IllegalArgumentException("Invalid rift aspect count");
            var amounts=new LinkedHashMap<String,Integer>();for(int i=0;i<size;i++)amounts.put(buffer.readUtf(),buffer.readVarInt());
            return new ChainedRiftRecipe(id,research,catalyst,count,output,amounts);
        }
        @Override public void toNetwork(FriendlyByteBuf buffer,ChainedRiftRecipe recipe){
            buffer.writeUtf(recipe.research);recipe.catalyst.toNetwork(buffer);buffer.writeVarInt(recipe.count);buffer.writeItem(recipe.output);buffer.writeVarInt(recipe.amounts.size());
            recipe.amounts.forEach((key,value)->{buffer.writeUtf(key);buffer.writeVarInt(value);});
        }
    }
}
