package tc4revival.content;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.TCAspects;

/** Reloadable arcane recipes, including the variable cap/rod assembly recipes. */
public  class ArcaneRecipe implements Recipe<CraftingContainer> {
    private final ResourceLocation id;
    private final String mode, research;
    private final boolean requiresWardedStone;
    private final CraftingRecipe crafting;
    private final Map<String, Integer> costHundredths;
    private final JsonObject definition;
    private final java.util.Set<ResourceLocation> undamaged = new java.util.HashSet<>();
    public ArcaneRecipe(ResourceLocation id, JsonObject definition) {
        this.id = id; this.definition = definition.deepCopy();
        requiresWardedStone = GsonHelper.getAsBoolean(definition, "requires_warded_stone", false);
        if (definition.has("undamaged")) for (var entry : definition.getAsJsonArray("undamaged")) undamaged.add(ResourceLocation.parse(entry.getAsString()));
        mode = GsonHelper.getAsString(definition, "mode", "shaped");
        research = GsonHelper.getAsString(definition, "research", "");
        crafting = switch (mode) {
            case "shaped" -> RecipeSerializer.SHAPED_RECIPE.fromJson(id, definition);
            case "shapeless" -> RecipeSerializer.SHAPELESS_RECIPE.fromJson(id, definition);
            case "wand", "sceptre" -> null;
            default -> throw new IllegalArgumentException("Unknown arcane recipe mode: " + mode);
        };
        var amounts = new LinkedHashMap<String, Integer>();
        if (definition.has("vis")) definition.getAsJsonObject("vis").entrySet().forEach(entry -> {
            int value = entry.getValue().getAsBigDecimal().movePointRight(2).intValueExact();
            if (!TCAspects.catalog().require(entry.getKey()).isPrimal() || value < 0)
                throw new IllegalArgumentException("Invalid arcane vis cost");
            amounts.put(entry.getKey(), value);
        });
        costHundredths = java.util.Collections.unmodifiableMap(amounts);
    }
    private Optional<WandAssembly.Result> assembly(CraftingContainer grid, Predicate<String> knowledge) {
        if (grid.getWidth() != 3 || grid.getHeight() != 3) return Optional.empty();
        var parts = new ArrayList<WandAssembly.Ingredient>();
        for (int i = 0; i < 9; i++) {
            var stack = grid.getItem(i);
            if (stack.isEmpty()) { parts.add(null); continue; }
            String legacy = null; int meta = 0;
            if (stack.is(Items.STICK)) legacy = "minecraft:stick";
            else {
                for (var component : WandComponent.all()) if (stack.is(TCItems.wandComponent(component.path()))) {
                    legacy = "Thaumcraft:" + component.legacyItem(); meta = component.metadata(); break;
                }
                if (stack.is(TCItems.PRIMAL_CHARM.get())) {
                    legacy = "Thaumcraft:ItemResource"; meta = 15;
                }
            }
            if (legacy == null) { legacy=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();meta=0; }
            parts.add(new WandAssembly.Ingredient(legacy, meta, stack.getTag()));
        }
        return WandAssembly.match(parts, mode.equals("sceptre"), knowledge);
    }
    public boolean unlocked(CraftingContainer grid, Predicate<String> knowledge) {
        return crafting == null ? assembly(grid, knowledge).filter(value -> value.unlocked() && value.hasOutput()).isPresent()
                : research.isEmpty() || knowledge.test(research);
    }
    public JsonObject displayDefinition() { return definition.deepCopy(); }
    public Map<Aspect, Integer> visCost(CraftingContainer grid) {
        var result = new LinkedHashMap<Aspect, Integer>();
        if (crafting != null) {
            var exact = new LinkedHashMap<Aspect,Integer>();
            costHundredths.forEach((key, amount) -> {
                var aspect = TCAspects.catalog().require(key);
                exact.put(aspect,amount);
                result.put(aspect,amount/100+(amount%100==0?0:1));
            });
            if (exact.values().stream().anyMatch(n -> n%100 != 0)) return new FractionalCosts(result,exact);
        }
        else assembly(grid, key -> true).ifPresent(value -> thaumcraft.api.aspects.AspectSelection.select(TCAspects.catalog(),thaumcraft.api.aspects.AspectSelection.WAND_ASSEMBLY_COST).forEach(aspect -> result.put(aspect, value.costPerPrimal())));
        return result;
    }
    /** Older whole-vis readers see the minimum whole balance sufficient to craft. */
    private static final class FractionalCosts extends java.util.AbstractMap<Aspect,Integer> {
        private final Map<Aspect,Integer> whole;
        private final Map<Aspect,Integer> exact;
        FractionalCosts(Map<Aspect,Integer> whole,Map<Aspect,Integer> exact) {
            this.whole=Map.copyOf(whole);
            this.exact=Map.copyOf(exact);
        }
        @Override public java.util.Set<Entry<Aspect,Integer>> entrySet() { return whole.entrySet(); }
    }
    public static int hundredths(Map<Aspect,Integer> costs,Aspect aspect) {
        return costs instanceof FractionalCosts f ? f.exact.getOrDefault(aspect,0)
                : Math.multiplyExact(costs.getOrDefault(aspect,0),100);
    }
    public static String formatHundredths(int amount) {
        return java.math.BigDecimal.valueOf(amount,2).stripTrailingZeros().toPlainString();
    }
    @Override public boolean matches(CraftingContainer grid, Level level) {
        if(!thaumcraft.api.research.ResearchCategories.enabled(research))return false;
        // Only the two recipes conditionally registered by old ConfigRecipes use this gate.
        if (requiresWardedStone && !tc4revival.config.TCConfig.SERVER.wardedStone.get()) return false;
        for (int slot=0;slot<grid.getContainerSize();slot++) {
            var stack=grid.getItem(slot);
            if (stack.isDamaged() && undamaged.contains(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()))) return false;
        }
        return crafting != null ? crafting.matches(grid, level) : assembly(grid, key -> true).filter(WandAssembly.Result::hasOutput).isPresent();
    }
    @Override public ItemStack assemble(CraftingContainer grid, RegistryAccess registries) {
        if (crafting != null) {
            var result = crafting.assemble(grid, registries);
            return definition.has("result_transform") ? thaumcraft.api.crafting.ArcaneResultTransforms.apply(
                    definition.getAsJsonObject("result_transform"), grid, registries, result) : result;
        }
        return assembly(grid, key -> true).filter(WandAssembly.Result::hasOutput)
                .map(value -> TCItems.CASTING_WAND.get().create(value.rod(), value.cap(), value.sceptre(), false)).orElse(ItemStack.EMPTY);
    }
    @Override public boolean canCraftInDimensions(int width, int height) { return crafting != null ? crafting.canCraftInDimensions(width, height) : width == 3 && height == 3; }
    @Override public ItemStack getResultItem(RegistryAccess registries) { return crafting == null ? ItemStack.EMPTY : crafting.getResultItem(registries); }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return TCArcane.RECIPE_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return TCArcane.RECIPE_TYPE.get(); }
    @Override public boolean isSpecial() { return true; }
    public static  class Serializer implements RecipeSerializer<ArcaneRecipe> {
        @Override public ArcaneRecipe fromJson(ResourceLocation id, JsonObject json) { return new ArcaneRecipe(id, json); }
        @Override public ArcaneRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new ArcaneRecipe(id, com.google.gson.JsonParser.parseString(buffer.readUtf(1048576)).getAsJsonObject());
        }
        @Override public void toNetwork(FriendlyByteBuf buffer, ArcaneRecipe recipe) { buffer.writeUtf(recipe.definition.toString(), 1048576); }
    }
}
