package temodern.crafting;

import java.util.ArrayList;
import appeng.api.crafting.*;
import appeng.api.stacks.*;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.*;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import tc4revival.content.TCArcane;
import temodern.TE;

/** Pattern definitions are checked against today's recipe manager when decoded and when executed. */
public final class ArcanePattern implements IPatternDetails {
    private final AEItemKey definition;
    private final IPatternDetails processing;
    private final CraftGrid grid;
    private final String recipe;
    private IInput[] inputs;
    private ArcanePattern(ItemStack stack, IPatternDetails processing, CraftGrid grid, String recipe) {
        this.definition = AEItemKey.of(stack); this.processing = processing; this.grid = grid; this.recipe = recipe;this.inputs=processing.getInputs();
    }
    public CraftGrid grid() {
        var copy = new CraftGrid(); for (int i=0;i<9;i++) copy.setItem(i,grid.getItem(i).copy()); return copy;
    }
    public String recipe() { return recipe; }
    /** Resolve through the manager's ID index on every use, including after /reload. */
    public static tc4revival.content.ArcaneRecipe recipe(Level level,String name) {
        var id=net.minecraft.resources.ResourceLocation.tryParse(name);
        if(id==null)return null;
        return level.getRecipeManager().byKey(id).filter(r->r.getType()==TCArcane.RECIPE_TYPE.get())
                .filter(tc4revival.content.ArcaneRecipe.class::isInstance)
                .map(tc4revival.content.ArcaneRecipe.class::cast).orElse(null);
    }
    public static ItemStack encode(CraftGrid grid, tc4revival.content.ArcaneRecipe recipe, Level level) {
        var stack = new ItemStack(TE.MATERIALS.get("encoded_arcane_pattern").get());
        var slots = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i=0;i<9;i++) slots.set(i,grid.getItem(i).copyWithCount(grid.getItem(i).isEmpty()?0:1));
        ContainerHelper.saveAllItems(stack.getOrCreateTag(),slots);
        stack.getOrCreateTag().putString("recipe",recipe.getId().toString());
        return stack;
    }
    @Override public AEItemKey getDefinition() { return definition; }
    @Override public IInput[] getInputs() { return inputs; }
    @Override public GenericStack[] getOutputs() { return processing.getOutputs(); }
    @Override public boolean supportsPushInputsToExternalInventory() { return false; }
    @Override public boolean equals(Object other) { return other instanceof ArcanePattern pattern && definition.equals(pattern.definition); }
    @Override public int hashCode() { return definition.hashCode(); }
    private boolean sameResult(CraftGrid proposed,tc4revival.content.ArcaneRecipe recipe,Level level){
        if(!recipe.matches(proposed,level)||!ItemStack.matches(recipe.assemble(grid,level.registryAccess()),recipe.assemble(proposed,level.registryAccess()))||!recipe.visCost(grid).equals(recipe.visCost(proposed)))return false;
        var expected=CraftRemainders.all(recipe,grid,recipe.assemble(grid,level.registryAccess()));
        var actual=CraftRemainders.all(recipe,proposed,recipe.assemble(proposed,level.registryAccess()));
        for(int i=0;i<9;i++)if(!ItemStack.matches(expected.get(i),actual.get(i)))return false;
        return true;
    }
    private IInput[] alternatives(tc4revival.content.ArcaneRecipe recipe,Level level){
        var candidates=new java.util.LinkedHashSet<AEItemKey>();
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try{
            TCArcane.RECIPE_SERIALIZER.get().toNetwork(buffer,recipe);
            var json=com.google.gson.JsonParser.parseString(buffer.readUtf(1048576)).getAsJsonObject();
            var ingredients=new java.util.ArrayList<com.google.gson.JsonElement>();
            if(json.has("key"))ingredients.addAll(json.getAsJsonObject("key").asMap().values());
            if(json.has("ingredients"))json.getAsJsonArray("ingredients").forEach(ingredients::add);
            for(var ingredient:ingredients)for(var item:net.minecraft.world.item.crafting.Ingredient.fromJson(ingredient).getItems())if(!item.isEmpty())candidates.add(AEItemKey.of(item));
        }finally{buffer.release();}
        var list=new java.util.ArrayList<IInput>();
        for(int slot=0;slot<9;slot++){
            var original=grid.getItem(slot);if(original.isEmpty())continue;
            var choices=new java.util.ArrayList<GenericStack>();choices.add(new GenericStack(AEItemKey.of(original),1));
            for(var candidate:candidates){
                if(candidate.equals(AEItemKey.of(original)))continue;
                var test=grid();test.setItem(slot,candidate.toStack());
                if(sameResult(test,recipe,level))choices.add(new GenericStack(candidate,1));
            }
            var possible=choices.toArray(GenericStack[]::new);
            list.add(new IInput(){
                public GenericStack[] getPossibleInputs(){return possible;}
                public long getMultiplier(){return 1;}
                public boolean isValid(AEKey input,Level world){return java.util.Arrays.stream(possible).anyMatch(s->s.what().equals(input));}
                public AEKey getRemainingKey(AEKey input){return null;}
            });
        }
        return list.toArray(IInput[]::new);
    }
    public CraftGrid suppliedGrid(KeyCounter[] supplied,Level level){
        if(supplied.length!=inputs.length)return null;
        var proposed=grid();int input=0;
        for(int slot=0;slot<9;slot++){
            if(grid.getItem(slot).isEmpty())continue;
            var counter=supplied[input];if(counter.size()!=1)return null;
            var entry=counter.iterator().next();
            if(entry.getLongValue()!=1||!(entry.getKey() instanceof AEItemKey key)||!inputs[input].isValid(key,level))return null;
            proposed.setItem(slot,key.toStack());input++;
        }
        var recipe=recipe(level,this.recipe);
        return recipe!=null&&sameResult(proposed,recipe,level)?proposed:null;
    }
    public static final class Decoder implements IPatternDetailsDecoder {
        @Override public boolean isEncodedPattern(ItemStack stack) { return stack.is(TE.MATERIALS.get("encoded_arcane_pattern").get()) && stack.hasTag() && stack.getTag().contains("recipe"); }
        @Override public IPatternDetails decodePattern(AEItemKey key, Level level) { return decodePattern(key.toStack(),level,false); }
        @Override public IPatternDetails decodePattern(ItemStack stack, Level level, boolean recover) {
            if (!isEncodedPattern(stack)) return null;
            var slots = NonNullList.withSize(9,ItemStack.EMPTY); ContainerHelper.loadAllItems(stack.getTag(), slots);
            var grid = new CraftGrid(); for(int i=0;i<9;i++) grid.setItem(i,slots.get(i));
            String id=stack.getTag().getString("recipe");
            var recipe=recipe(level,id);
            if(recipe==null || !recipe.matches(grid,level))return null;
            var output=recipe.assemble(grid,level.registryAccess()); if(output.isEmpty())return null;
            var in = new ArrayList<GenericStack>();
            for(var item:grid.getItems())if(!item.isEmpty())in.add(new GenericStack(AEItemKey.of(item),1));
            var out = new ArrayList<GenericStack>(); out.add(new GenericStack(AEItemKey.of(output),output.getCount()));
            for(var rest:CraftRemainders.all(recipe,grid,output))if(!rest.isEmpty())out.add(new GenericStack(AEItemKey.of(rest),rest.getCount()));
            if(in.isEmpty())return null;
            var encoded=PatternDetailsHelper.encodeProcessingPattern(in.toArray(GenericStack[]::new),out.toArray(GenericStack[]::new));
            var details=PatternDetailsHelper.decodePattern(encoded,level);
            if(details==null)return null;
            var result=new ArcanePattern(stack,details,grid,id);
            result.inputs=result.alternatives(recipe,level);
            return result;
        }
    }
}
