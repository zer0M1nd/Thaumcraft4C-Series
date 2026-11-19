package heartwork.recipe;

import com.google.gson.JsonObject;
import heartwork.Heartwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

public class CountedSmeltingSerializer implements RecipeSerializer<SmeltingRecipe> {
    private static SmeltingRecipe create(ResourceLocation id, String group, Ingredient input, ItemStack output, float xp, int time) {
        return new SmeltingRecipe(id, group, CookingBookCategory.FOOD, input, output, xp, time) {
            @Override public RecipeSerializer<?> getSerializer() { return Heartwork.SMELTING.get(); }
        };
    }
    @Override public SmeltingRecipe fromJson(ResourceLocation id, JsonObject json) {
        return create(id, GsonHelper.getAsString(json,"group",""), Ingredient.fromJson(json.get("ingredient")),
                ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json,"result")), GsonHelper.getAsFloat(json,"experience",0), GsonHelper.getAsInt(json,"cookingtime",200));
    }
    @Override public SmeltingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf input) {
        return create(id,input.readUtf(),Ingredient.fromNetwork(input),input.readItem(),input.readFloat(),input.readVarInt());
    }
    @Override public void toNetwork(FriendlyByteBuf output, SmeltingRecipe recipe) {
        output.writeUtf(recipe.getGroup());recipe.getIngredients().get(0).toNetwork(output);
        output.writeItem(recipe.getResultItem(net.minecraft.core.RegistryAccess.EMPTY));output.writeFloat(recipe.getExperience());output.writeVarInt(recipe.getCookingTime());
    }
}
