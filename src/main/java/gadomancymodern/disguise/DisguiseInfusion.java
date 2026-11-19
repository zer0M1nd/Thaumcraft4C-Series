package gadomancymodern.disguise;

import com.google.gson.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import tc4revival.content.*;

/** The matched disguise is part of the saved infusion transaction, not a live reference to a pedestal. */
public class DisguiseInfusion extends InfusionRecipe {
    protected final JsonObject raw;
    protected ItemStack selectedCenter=ItemStack.EMPTY,selectedDisguise=ItemStack.EMPTY;
    public DisguiseInfusion(ResourceLocation id,JsonObject json){super(id,json);raw=json.deepCopy();}
    @Override public boolean acceptsCenter(ItemStack stack){return ArmorDisguise.armor(stack)&&!ArmorDisguise.disguised(stack)&&stack.getCount()<=stack.getMaxStackSize();}
    @Override public boolean matches(Input input,Level level){
        if(!super.matches(input,level))return false;
        ItemStack disguise=ItemStack.EMPTY;for(int i=1;i<input.getContainerSize();i++){var stack=input.getItem(i);if(ArmorDisguise.armor(stack)||ArmorDisguise.invisible(stack)){if(!disguise.isEmpty())return false;disguise=stack;}}
        if(!ArmorDisguise.valid(input.getItem(0),disguise))return false;
        // Vanilla altar calls plan immediately after matching on the same logical thread.
        // Keep only the last snapshot, unlike the author's unbounded identity cache.
        selectedCenter=input.getItem(0).copy();selectedDisguise=disguise.copyWithCount(1);return true;
    }
    @Override public Plan plan(ItemStack center){
        var base=super.plan(center);if(!ItemStack.isSameItemSameTags(center,selectedCenter)||selectedDisguise.isEmpty())return base;
        var ingredients=new java.util.ArrayList<InfusionIngredient>();var selected=new JsonObject();var item=new JsonObject();item.addProperty("item",net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(selectedDisguise.getItem()).toString());selected.add("ingredient",item);selected.addProperty("damage",-1);if(selectedDisguise.hasTag())selected.addProperty("nbt",selectedDisguise.getTag().toString());ingredients.add(new InfusionIngredient(selected));
        ingredients.addAll(base.components().subList(0,base.components().size()-1));var result=ArmorDisguise.apply(center,selectedDisguise);
        return new Plan(ingredients,base.aspects(),-20,0,false,InfusionOutput.patch("gadomancy",result.getTag().getCompound("gadomancy")));
    }
    @Override public Plan planForCraft(ItemStack center,Level level){
        if(!ItemStack.isSameItemSameTags(center,selectedCenter)||selectedDisguise.isEmpty())throw new IllegalStateException("Disguise infusion has no matching snapshot");return plan(center);
    }
    @Override public RecipeSerializer<?> getSerializer(){return gadomancymodern.Gadomancy.DISGUISE_RECIPE.get();}
    public static class Serializer implements RecipeSerializer<DisguiseInfusion>{
        public DisguiseInfusion fromJson(ResourceLocation id,JsonObject json){return new DisguiseInfusion(id,json);}
        public DisguiseInfusion fromNetwork(ResourceLocation id,FriendlyByteBuf data){return fromJson(id,JsonParser.parseString(data.readUtf(1048576)).getAsJsonObject());}
        public void toNetwork(FriendlyByteBuf data,DisguiseInfusion recipe){data.writeUtf(recipe.raw.toString(),1048576);}
    }
}
