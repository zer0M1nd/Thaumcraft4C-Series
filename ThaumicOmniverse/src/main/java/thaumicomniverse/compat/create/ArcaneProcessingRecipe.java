package thaumicomniverse.compat.create;

import com.google.gson.*;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.recipe.*;
import com.simibubi.create.content.processing.sequenced.IAssemblyRecipe;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import tc4revival.aspect.*;
import tc4revival.content.TCArcane;

public class ArcaneProcessingRecipe extends ProcessingRecipe<RecipeWrapper> implements IAssemblyRecipe {
    private final boolean infusion;
    private final boolean spouting;
    private String research = "";
    private int instability;
    private AspectAmounts aspects = new AspectAmounts();
    private List<Ingredient> components = List.of();
    private net.minecraft.world.item.ItemStack selectionOutput=net.minecraft.world.item.ItemStack.EMPTY;
    public ArcaneProcessingRecipe(ArcaneAssembly.Type type, ProcessingRecipeBuilder.ProcessingRecipeParams params, boolean infusion) {
        this(type,params,infusion,false);
    }
    public ArcaneProcessingRecipe(ArcaneAssembly.Type type, ProcessingRecipeBuilder.ProcessingRecipeParams params, boolean infusion,boolean spouting) {super(type,params);this.infusion=infusion;this.spouting=spouting;}
    public boolean spouting(){return spouting;}
    public boolean infusion() { return infusion; }
    public String research() { return research; }
    public int instability() { return instability; }
    public AspectAmounts aspects() { return aspects.copy(); }
    public List<Ingredient> components() { return components; }
    public net.minecraft.world.item.ItemStack selectionOutput(net.minecraft.core.RegistryAccess registry){return selectionOutput.isEmpty()?getResultItem(registry):selectionOutput.copy();}
    @Override protected int getMaxInputCount() { return 1; }
    @Override protected int getMaxOutputCount() { return 1; }
    @Override public boolean matches(RecipeWrapper input, Level level) { return !ingredients.isEmpty() && ingredients.get(0).test(input.getItem(0)); }
    @Override public void readAdditional(JsonObject json) {
        research = GsonHelper.getAsString(json, "research", "");
        selectionOutput=json.has("selection_output")?net.minecraft.world.item.crafting.ShapedRecipe.itemStackFromJson(json.getAsJsonObject("selection_output")):net.minecraft.world.item.ItemStack.EMPTY;
        instability = GsonHelper.getAsInt(json, "instability", 0);
        if(instability < 0 || instability > 100) throw new JsonParseException("Invalid instability");
        aspects = new AspectAmounts();
        GsonHelper.getAsJsonObject(json, "aspects").entrySet().forEach(entry -> {
            int amount = entry.getValue().getAsInt();
            if(amount <= 0 || amount > 65536) throw new JsonParseException("Invalid processing aspect amount");
            aspects.add(TCAspects.catalog().require(entry.getKey()), amount);
        });
        if(aspects.isEmpty())throw new JsonParseException("Arcane processing requires aspects");
        if(spouting&&(aspects.size()!=1||aspects.view().values().iterator().next()>64))throw new JsonParseException("Each essentia spouting step requires a single aspect, at most 64 units");
        var list = new ArrayList<Ingredient>();
        if(json.has("components"))for(var element:json.getAsJsonArray("components"))list.add(Ingredient.fromJson(element));
        if(infusion && list.isEmpty() || list.size()>64)throw new JsonParseException("Infusion requires 1..64 components");
        components = List.copyOf(list);
    }
    @Override public void writeAdditional(JsonObject json) {
        json.addProperty("research",research);json.addProperty("instability",instability);
        if(!selectionOutput.isEmpty())json.add("selection_output",ArcaneCreateRecipes.stack(selectionOutput));
        var costs = new JsonObject();aspects.view().forEach((aspect,amount)->costs.addProperty(aspect.tag(),amount));json.add("aspects",costs);
        var list=new JsonArray();components.forEach(ingredient->list.add(ingredient.toJson()));json.add("components",list);
    }
    @Override public void readAdditional(FriendlyByteBuf data) { readAdditional(JsonParser.parseString(data.readUtf(65536)).getAsJsonObject()); }
    @Override public void writeAdditional(FriendlyByteBuf data) { var json=new JsonObject();writeAdditional(json);data.writeUtf(json.toString(),65536); }
    @Override public void addRequiredMachines(Set<ItemLike> machines) { machines.add(spouting?ArcaneCreateContent.SPOUT.get():infusion?TCArcane.INFUSION_MATRIX.get():TCArcane.CRUCIBLE.get());if(infusion)machines.add(TCArcane.ARCANE_PEDESTAL.get()); }
    @Override public void addAssemblyIngredients(List<Ingredient> ingredients) { ingredients.addAll(components); }
    @Override public Component getDescriptionForAssembly() { return spouting
            ?Component.translatable("omniverse.assembly.inject_aspect",thaumcraft.api.aspects.AspectViews.display(aspects.aspects().get(0)).displayName())
            :Component.translatable(infusion?"omniverse.assembly.infusion":"omniverse.assembly.crucible"); }
    @Override public Supplier<Supplier<SequencedAssemblySubCategory>> getJEISubCategory() { return () -> () -> new ArcaneAssemblySubCategory(infusion,spouting); }
}
