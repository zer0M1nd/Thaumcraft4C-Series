package automagy.content;

import com.google.gson.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import tc4revival.content.CrucibleRecipe;

public final class EntropicRefiningRecipe extends CrucibleRecipe {
    private final JsonObject definition;
    private EntropicRefiningRecipe(ResourceLocation id,JsonObject json) { super(id,json);definition=json.deepCopy(); }
    @Override public ItemStack getResultItem(RegistryAccess registries) {
        int count=automagy.AutomagyConfig.REFINING_SLIVERS.get();
        return count==0?ItemStack.EMPTY:super.getResultItem(registries).copyWithCount(count);
    }
    @Override public ItemStack assemble(Input input,RegistryAccess registries) { return getResultItem(registries); }
    @Override public RecipeSerializer<?> getSerializer() { return Content.REFINING_RECIPE.get(); }
    public static class Serializer implements RecipeSerializer<EntropicRefiningRecipe> {
        @Override public EntropicRefiningRecipe fromJson(ResourceLocation id,JsonObject json) { return new EntropicRefiningRecipe(id,json); }
        @Override public EntropicRefiningRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buffer) { return fromJson(id,JsonParser.parseString(buffer.readUtf()).getAsJsonObject()); }
        @Override public void toNetwork(FriendlyByteBuf buffer,EntropicRefiningRecipe recipe) { buffer.writeUtf(recipe.definition.toString()); }
    }
}
