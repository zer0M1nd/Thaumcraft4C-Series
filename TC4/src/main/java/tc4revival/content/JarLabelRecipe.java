package tc4revival.content;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import tc4revival.aspect.AspectAmounts;

public  class JarLabelRecipe extends CustomRecipe {
    private final com.google.gson.JsonObject definition;
    private final net.minecraft.world.item.crafting.Ingredient label,source;
    private final ItemStack output;
    private final int sourceAmount;
    public JarLabelRecipe(ResourceLocation id,com.google.gson.JsonObject json) {
        super(id,CraftingBookCategory.MISC);definition=json.deepCopy();
        label=net.minecraft.world.item.crafting.Ingredient.fromJson(json.get("label"));source=net.minecraft.world.item.crafting.Ingredient.fromJson(json.get("source"));
        output=net.minecraft.world.item.crafting.ShapedRecipe.itemStackFromJson(json.getAsJsonObject("result"));sourceAmount=json.get("source_amount").getAsInt();
    }
    private ItemStack phial(CraftingContainer grid) {
        int labels=0,phials=0; ItemStack essence=ItemStack.EMPTY;
        for (int slot=0;slot<grid.getContainerSize();slot++) {
            var stack=grid.getItem(slot); if (stack.isEmpty()) continue;
            if (label.test(stack)) labels++;
            else if (source.test(stack)) { phials++; essence=stack; }
            else return null;
        }
        if (labels!=1 || phials>1) return null;
        if (phials==1) {
            if(!(essence.getItem() instanceof StoredAspectItem))return null;
            var stored=((StoredAspectItem)essence.getItem()).storedAspects(essence);
            if (stored.size()!=1 || stored.total()!=sourceAmount) return null;
        }
        return essence;
    }
    @Override public boolean matches(CraftingContainer grid,Level level) { return phial(grid)!=null; }
    @Override public ItemStack assemble(CraftingContainer grid,RegistryAccess access) {
        var essence=phial(grid); if (essence==null) return ItemStack.EMPTY;
        var result=output.copy();
        if (!essence.isEmpty() && result.getItem() instanceof StoredAspectItem stored) stored.setStoredAspects(result,new AspectAmounts().add(((StoredAspectItem)essence.getItem()).storedAspects(essence).aspects().get(0),0));
        return result;
    }
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingContainer grid) {
        var result=NonNullList.withSize(grid.getContainerSize(),ItemStack.EMPTY);
        for (int slot=0;slot<grid.getContainerSize();slot++) if (source.test(grid.getItem(slot))) { var copy=grid.getItem(slot).copy(); copy.setCount(1); result.set(slot,copy); }
        return result;
    }
    @Override public boolean canCraftInDimensions(int width,int height) { return width*height>=1; }
    @Override public RecipeSerializer<?> getSerializer() { return TCArcane.JAR_LABEL_SERIALIZER.get(); }
    public static  class Serializer implements RecipeSerializer<JarLabelRecipe> {
        public JarLabelRecipe fromJson(ResourceLocation id,com.google.gson.JsonObject json) { return new JarLabelRecipe(id,json); }
        public JarLabelRecipe fromNetwork(ResourceLocation id,net.minecraft.network.FriendlyByteBuf data) { return fromJson(id,com.google.gson.JsonParser.parseString(data.readUtf()).getAsJsonObject()); }
        public void toNetwork(net.minecraft.network.FriendlyByteBuf data,JarLabelRecipe recipe) { data.writeUtf(recipe.definition.toString()); }
    }

}
