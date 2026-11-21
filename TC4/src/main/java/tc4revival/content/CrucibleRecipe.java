package tc4revival.content;

import com.google.gson.JsonObject;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.TCAspects;

/** Data-pack crucible recipes; priority depends on distinct aspects, not their total quantity. */
public  class CrucibleRecipe implements Recipe<CrucibleRecipe.Input> {
    public static  class Input extends SimpleContainer {
        private final AspectAmounts aspects;
        private final Predicate<String> research;
        public AspectAmounts aspects() { return aspects.copy(); }
        public boolean researchKnown(String key) { return research.test(key); }
        public Input(ItemStack catalyst, AspectAmounts aspects, Predicate<String> research) {
            super(catalyst); this.aspects = aspects.copy(); this.research = research;
        }
    }
    private final ResourceLocation id;
    private final Ingredient catalyst;
    private final ItemStack output;
    private final AspectAmounts costs = new AspectAmounts();
    private final String research;
    private final int legacyOrder;
    private final JsonObject definition;
    protected CrucibleRecipe(ResourceLocation id, JsonObject json) {
        this.id = id; definition = json.deepCopy(); research = GsonHelper.getAsString(json, "research");
        legacyOrder = GsonHelper.getAsInt(json, "legacy_order", Integer.MAX_VALUE);
        catalyst = Ingredient.fromJson(json.get("catalyst"));
        output = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
        var result = GsonHelper.getAsJsonObject(json, "result");
        if (result.has("nbt")) try { output.setTag(net.minecraft.nbt.TagParser.parseTag(GsonHelper.getAsString(result, "nbt"))); }
        catch (com.mojang.brigadier.exceptions.CommandSyntaxException error) { throw new IllegalArgumentException("Invalid crucible result NBT: " + id, error); }
        if (output.isEmpty() || output.getCount() > output.getMaxStackSize()) throw new IllegalArgumentException("Invalid crucible result: " + id);
        GsonHelper.getAsJsonObject(json, "aspects").entrySet().forEach(entry -> {
            int amount = entry.getValue().getAsInt();
            if (amount <= 0) throw new IllegalArgumentException("Nonpositive crucible aspect cost: " + id);
            costs.add(TCAspects.catalog().require(entry.getKey()), amount);
        });
        if (costs.isEmpty()) throw new IllegalArgumentException("Crucible recipes require aspects: " + id);
    }
    public AspectAmounts costs() { return costs.copy(); }
    public String research() { return research; }
    public net.minecraft.world.item.crafting.Ingredient displayCatalyst() { return catalyst; }
    public int legacyOrder() { return legacyOrder; }
    public boolean catalystMatches(ItemStack stack) { return catalyst.test(stack); }
    @Override public boolean matches(Input input, Level level) {
        if(!thaumcraft.api.research.ResearchCategories.enabled(research))return false;
        if (!input.research.test(research) || !catalystMatches(input.getItem(0))) return false;
        return costs.view().entrySet().stream().allMatch(entry -> input.aspects.amount(entry.getKey()) >= entry.getValue());
    }
    public Optional<AspectAmounts> remainder(AspectAmounts contents) {
        if (costs.view().entrySet().stream().anyMatch(entry -> contents.amount(entry.getKey()) < entry.getValue())) return Optional.empty();
        var remainder = contents.copy(); costs.view().forEach(remainder::remove); return Optional.of(remainder);
    }
    public static Optional<CrucibleRecipe> find(Level level, Input input) {
        var supplied=thaumcraft.api.crafting.MagicRecipeProviders.crucible(level,input);if(supplied.isPresent())return supplied;
        return level.getRecipeManager().getAllRecipesFor(TCArcane.CRUCIBLE_RECIPE_TYPE.get()).stream()
                .filter(recipe -> recipe.matches(input, level))
                .sorted(Comparator.<CrucibleRecipe>comparingInt(recipe -> recipe.costs().size()).reversed()
                        .thenComparingInt(recipe -> recipe.legacyOrder).thenComparing(recipe -> recipe.id.toString())).findFirst();
    }
    @Override public ItemStack assemble(Input input, RegistryAccess registries) { return output.copy(); }
    @Override public ItemStack getResultItem(RegistryAccess registries) { return output.copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 1; }
    @Override public boolean isSpecial() { return true; }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return TCArcane.CRUCIBLE_RECIPE_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return TCArcane.CRUCIBLE_RECIPE_TYPE.get(); }
    public static  class Serializer implements RecipeSerializer<CrucibleRecipe> {
        @Override public CrucibleRecipe fromJson(ResourceLocation id, JsonObject json) { return new CrucibleRecipe(id, json); }
        @Override public CrucibleRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf data) {
            return fromJson(id, com.google.gson.JsonParser.parseString(data.readUtf(1048576)).getAsJsonObject());
        }
        @Override public void toNetwork(FriendlyByteBuf data, CrucibleRecipe recipe) { data.writeUtf(recipe.definition.toString(), 1048576); }
    }
}
