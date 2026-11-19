package tc4revival.content;

import com.google.gson.JsonObject;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/** Supplemental smelting output. Zero base yield is separate from the nonempty result item. */
public  class InfernalBonusRecipe implements Recipe<SimpleContainer> {
    private final ResourceLocation id;
    private final JsonObject definition;
    private final Ingredient input;
    private final ItemStack result;
    private final int baseCount,order;
    protected InfernalBonusRecipe(ResourceLocation id,JsonObject json) {
        this.id=id; definition=json.deepCopy(); input=Ingredient.fromJson(json.get("ingredient"));
        result=ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json,"result"));
        baseCount=GsonHelper.getAsInt(json,"base_count",0); order=GsonHelper.getAsInt(json,"legacy_order",Integer.MAX_VALUE);
        if (result.isEmpty() || result.getCount()!=1 || baseCount<0 || baseCount>result.getMaxStackSize()-3)
            throw new IllegalArgumentException("Invalid infernal furnace bonus "+id);
    }
    public static Optional<InfernalBonusRecipe> find(Level level,ItemStack input) {
        return level.getRecipeManager().getAllRecipesFor(TCArcane.INFERNAL_BONUS_TYPE.get()).stream()
                .filter(recipe->recipe.matches(new SimpleContainer(input),level)).min(Comparator.comparingInt((InfernalBonusRecipe recipe)->recipe.order)
                        .thenComparing(recipe->recipe.id.toString()));
    }
    public ItemStack roll(int bellows,RandomSource random) {
        int count=baseCount,active=Math.min(3,Math.max(0,bellows));
        if (active==0) { if (random.nextInt(4)==0) count++; }
        else for (int i=0;i<active;i++) if (random.nextFloat()<.44f) count++;
        if (count==0) return ItemStack.EMPTY;
        var stack=result.copy(); stack.setCount(count); return stack;
    }
    @Override public boolean matches(SimpleContainer inventory,Level level) { return input.test(inventory.getItem(0)); }
    @Override public ItemStack assemble(SimpleContainer inventory,RegistryAccess access) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int width,int height) { return width*height>=1; }
    @Override public ItemStack getResultItem(RegistryAccess access) { return result.copy(); }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return TCArcane.INFERNAL_BONUS_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return TCArcane.INFERNAL_BONUS_TYPE.get(); }
    @Override public boolean isSpecial() { return true; }
    public static  class Serializer implements RecipeSerializer<InfernalBonusRecipe> {
        @Override public InfernalBonusRecipe fromJson(ResourceLocation id,JsonObject json) { return new InfernalBonusRecipe(id,json); }
        @Override public InfernalBonusRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buffer) {
            return fromJson(id,com.google.gson.JsonParser.parseString(buffer.readUtf()).getAsJsonObject());
        }
        @Override public void toNetwork(FriendlyByteBuf buffer,InfernalBonusRecipe recipe) { buffer.writeUtf(recipe.definition.toString()); }
    }
}
