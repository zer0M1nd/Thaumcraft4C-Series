package tc4revival.research;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import tc4revival.content.*;

/** Book pages bind to recipe IDs, never to a second copy of the crafting rules. */
public class LiveBookRecipes {
    public record View(ResearchPage.Type type, String id, ItemStack output, List<List<ItemStack>> inputs,
            Map<String,Integer> aspects, int width, int instability, int xp, String caption) {}
    private static final Map<String,List<ResourceLocation>> BINDINGS = readBindings();
    public static Map<String,List<ResourceLocation>> bindings() { return BINDINGS; }
    private static Map<String,List<ResourceLocation>> readBindings() {
        try (var stream=LiveBookRecipes.class.getResourceAsStream("/data/thaumcraft/research_recipe_bindings/legacy.json")) {
            var json=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(Objects.requireNonNull(stream),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            var result=new LinkedHashMap<String,List<ResourceLocation>>();
            json.entrySet().forEach(e->result.put(e.getKey(),java.util.stream.StreamSupport.stream(e.getValue().getAsJsonArray().spliterator(),false)
                    .map(v->ResourceLocation.parse(v.getAsString())).toList()));
            return Collections.unmodifiableMap(result);
        } catch(java.io.IOException e) { throw new IllegalStateException("Missing book recipe bindings",e); }
    }
    public static boolean supported(ResearchPage.Type type) {
        return switch(type) { case NORMAL_CRAFTING, ARCANE_CRAFTING, CRUCIBLE_CRAFTING, INFUSION_CRAFTING, INFUSION_ENCHANTMENT, SMELTING -> true; default -> false; };
    }
    public static List<View> resolve(Level level, ResearchPage page) {
        var rows=new LinkedHashMap<String,View>();
        collect(level,page.recipe(),page.type(),rows);
        return List.copyOf(rows.values());
    }
    private static void collect(Level level, ResearchPage.Recipe page, ResearchPage.Type type, Map<String,View> out) {
        if(page instanceof ResearchPage.Sequence sequence) { sequence.elements().forEach(p->collect(level,p,type,out)); return; }
        if(page instanceof ResearchPage.Reference reference) {
            for(String key:reference.keys()) {
                var ids=BINDINGS.get(key);
                if(ids!=null) {
                    for(var id:ids) level.getRecipeManager().byKey(id).ifPresent(r->append(level,r,null,out));
                    continue; // Removed bound recipes must not resurrect their old display definition.
                }
                var id=ResourceLocation.tryParse(key);
                var actual=id==null?Optional.<Recipe<?>>empty():level.getRecipeManager().byKey(id);
                if(actual.isPresent()) { append(level,actual.get(),null,out); continue; }
                var entry=ResearchPageRecipes.bundled().entries().get(key);
                if(entry!=null)resolveAddonAlias(level,key,type,entry.definition()).forEach(row->out.put(row.id(),row));
            }
        } else if(page instanceof ResearchPage.Arcane arcane) {
            // The book's sample chooses a rod/cap combination; the live assembly recipe decides
            // its costs and output, including datapack changes and registry-added components.
            var grid=grid(); int index=0;
            for(var ingredient:arcane.ingredients().elements()) {
                grid.setItem(index%arcane.width()+3*(index/arcane.width()),sample(ingredient));index++;
            }
            for(var r:level.getRecipeManager().getAllRecipesFor(TCArcane.RECIPE_TYPE.get()))
                if(r.matches(grid,level)) { append(level,r,grid,out); break; }
        } else if(page instanceof ResearchPage.RunicExample example) {
            var center=sample(new ResearchPage.Item(example.input()));
            var components=example.ingredients().elements().stream().map(LiveBookRecipes::sample).toList();
            for(var r:level.getRecipeManager().getAllRecipesFor(TCArcane.INFUSION_RECIPE_TYPE.get()))
                if(!r.enchantmentRecipe()&&r.research().equals(example.research())
                        &&thaumcraft.api.research.ResearchCategories.enabled(r.research()))
                    infusion(level,r,center,components,out);
        } else if(type==ResearchPage.Type.SMELTING) {
            var input=sample(page);
            for(var r:level.getRecipeManager().getAllRecipesFor(RecipeType.SMELTING))
                if(r.getIngredients().stream().anyMatch(i->i.test(input)))append(level,r,null,out);
        }
    }
    /** Older addons use display aliases rather than registry IDs. Match only their own recipes. */
    public static List<View> resolveAddonAlias(Level level,String key,ResearchPage.Type type,ResearchPageRecipes.Definition definition) {
        ResearchPage.Recipe output;String research;
        if(definition instanceof ResearchPageRecipes.Crafting d){output=d.output();research=d.research();}
        else if(definition instanceof ResearchPageRecipes.Crucible d){output=d.output();research=d.research();}
        else if(definition instanceof ResearchPageRecipes.Infusion d){output=ResearchInfusionDisplay.output(d);research=d.research();}
        else return List.of();
        var expected=sample(output);var found=new LinkedHashMap<String,View>();
        // Display aliases retain legacy capitalization. Unqualified migrated aliases
        // inherit their modern output's owner, never an arbitrary recipe namespace.
        int separator=key.indexOf(':');
        String owner=separator>0?key.substring(0,separator):expected.isEmpty()?"":net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(expected.getItem()).getNamespace();
        var id=ResourceLocation.tryParse(owner+":display_alias");
        if(id==null||id.getNamespace().equals("minecraft")||id.getNamespace().equals("thaumcraft"))return List.of();
        for(var recipe:level.getRecipeManager().getRecipes()) {
            if(!recipe.getId().getNamespace().equals(id.getNamespace()))continue;
            boolean kind=switch(type){case ARCANE_CRAFTING->recipe instanceof ArcaneRecipe;case NORMAL_CRAFTING->recipe instanceof CraftingRecipe;case CRUCIBLE_CRAFTING->recipe instanceof CrucibleRecipe;case INFUSION_CRAFTING->recipe instanceof InfusionRecipe;default->false;};
            if(!kind)continue;
            var result=recipe.getResultItem(level.registryAccess());
            // Property-output recipes have no fixed result stack. Compare their actual
            // plan on the page's central item, including the preserved input NBT.
            if(recipe instanceof InfusionRecipe infusion && result.isEmpty()
                    && definition instanceof ResearchPageRecipes.Infusion description) {
                var center=sample(description.input());
                if(!infusion.acceptsCenter(center))continue;
                result=infusion.plan(center).output().apply(center);
            }
            if(expected.isEmpty()||!ItemStack.isSameItemSameTags(expected,result))continue;
            if(recipe instanceof InfusionRecipe r&&!r.research().equals(research))continue;
            if(recipe instanceof CrucibleRecipe r&&!r.research().equals(research))continue;
            if(recipe instanceof ArcaneRecipe r&&!net.minecraft.util.GsonHelper.getAsString(r.displayDefinition(),"research","").equals(research))continue;
            append(level,recipe,null,found);
        }
        if(!found.isEmpty())return List.copyOf(found.values());
        // Some addon pages describe their own machines (e.g. vat infusions), outside RecipeManager.
        // Preserve those server-synchronised presentations; never fall back for bound TC4 recipes.
        List<List<ItemStack>> inputs;Map<String,Integer> aspects;int width=0,instability=0;
        if(definition instanceof ResearchPageRecipes.Crafting d){inputs=displayInputs(d.ingredients());aspects=d.aspects();width=d.width();
            if(type==ResearchPage.Type.ARCANE_CRAFTING) {
                var exact=new LinkedHashMap<String,Integer>();aspects.forEach((tag,n)->exact.put(tag,Math.multiplyExact(n,100)));aspects=exact;
            }}
        else if(definition instanceof ResearchPageRecipes.Crucible d){inputs=List.of(displayChoices(d.catalyst()));aspects=d.aspects();}
        else if(definition instanceof ResearchPageRecipes.Infusion d){inputs=new ArrayList<>();inputs.add(displayChoices(d.input()));inputs.addAll(displayInputs(d.ingredients()));aspects=d.aspects();instability=d.instability();}
        else return List.of();
        return expected.isEmpty()?List.of():List.of(new View(type,key,expected,inputs,aspects,width,instability,0,""));
    }
    private static List<List<ItemStack>> displayInputs(ResearchPage.Sequence inputs){return inputs.elements().stream().map(LiveBookRecipes::displayChoices).toList();}
    private static List<ItemStack> displayChoices(ResearchPage.Recipe input) {
        if(input instanceof ResearchPage.Ore ore) {
            var values=new ArrayList<ItemStack>();
            for(var name:ore.names()) {
                var tag=ResourceLocation.tryParse(name);if(tag==null)continue;
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getTag(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,tag)).ifPresent(items->items.forEach(item->values.add(new ItemStack(item.value()))));
            }
            return values.isEmpty()?ore.elements().stream().flatMap(v->displayChoices(v).stream()).toList():values;
        }
        if(input instanceof ResearchPage.Sequence sequence)return sequence.elements().stream().flatMap(v->displayChoices(v).stream()).toList();
        var stack=sample(input);return stack.isEmpty()?List.of():List.of(stack);
    }
    private static ItemStack sample(ResearchPage.Recipe recipe) {
        if(recipe instanceof ResearchPage.Item item)return ResearchDisplayItems.bundled().resolve(item.stack(),0).orElse(ItemStack.EMPTY);
        if(recipe instanceof ResearchPage.Ore ore)return ore.elements().isEmpty()?ItemStack.EMPTY:sample(ore.elements().get(0));
        if(recipe instanceof ResearchPage.Sequence seq)return seq.elements().isEmpty()?ItemStack.EMPTY:sample(seq.elements().get(0));
        return ItemStack.EMPTY;
    }
    private static TransientCraftingContainer grid() {
        return new TransientCraftingContainer(new AbstractContainerMenu(null,-1) {
            public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}
            public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int i){return ItemStack.EMPTY;}
        },3,3);
    }
    private static List<List<ItemStack>> ingredients(List<Ingredient> ingredients) {
        return ingredients.stream().map(i->Arrays.asList(i.getItems())).toList();
    }
    private static void append(Level level, Recipe<?> recipe, TransientCraftingContainer example, Map<String,View> out) {
        String id=recipe.getId().toString();
        if(recipe instanceof ArcaneRecipe arcane) {
            var json=arcane.displayDefinition();
            if(!thaumcraft.api.research.ResearchCategories.enabled(json.has("research")?json.get("research").getAsString():""))return;
            if(json.has("requires_warded_stone")&&json.get("requires_warded_stone").getAsBoolean()&&!tc4revival.config.TCConfig.SERVER.wardedStone.get())return;
            String mode=json.has("mode")?json.get("mode").getAsString():"shaped";
            var grid=example==null?grid():example;
            List<List<ItemStack>> inputs; int width;
            if(example==null && json.has("display_inputs")) {
                width=net.minecraft.util.GsonHelper.getAsInt(json,"display_width",3);
                if(width<0||width>3)throw new IllegalArgumentException("Invalid arcane display width: "+id);
                var samples=json.getAsJsonArray("display_inputs");
                if(samples.size()>9)throw new IllegalArgumentException("Too many arcane display inputs: "+id);
                inputs=new ArrayList<>();int w=width==0?3:width;
                for(int i=0;i<samples.size();i++) {
                    if(i/w>=3)throw new IllegalArgumentException("Too many arcane display rows: "+id);
                    ItemStack stack;
                    try {stack=samples.get(i).isJsonNull()?ItemStack.EMPTY:ItemStack.of(net.minecraft.nbt.TagParser.parseTag(samples.get(i).getAsString()));}
                    catch(com.mojang.brigadier.exceptions.CommandSyntaxException invalid){throw new IllegalArgumentException("Invalid arcane display stack: "+id,invalid);}
                    inputs.add(stack.isEmpty()?List.of():List.of(stack));
                    grid.setItem(i%w+3*(i/w),stack.copy());
                }
            } else if(mode.equals("wand")||mode.equals("sceptre")) {
                if(example==null)return;
                inputs=new ArrayList<>();for(int i=0;i<9;i++)inputs.add(grid.getItem(i).isEmpty()?List.of():List.of(grid.getItem(i).copy()));width=3;
            } else {
                var crafting=mode.equals("shapeless")?RecipeSerializer.SHAPELESS_RECIPE.fromJson(recipe.getId(),json):RecipeSerializer.SHAPED_RECIPE.fromJson(recipe.getId(),json);
                width=crafting instanceof ShapedRecipe shaped?shaped.getWidth():0;
                inputs=ingredients(crafting.getIngredients());int w=width==0?3:width;
                for(int i=0;i<inputs.size();i++)if(!inputs.get(i).isEmpty())grid.setItem(i%w+3*(i/w),inputs.get(i).get(0).copy());
            }
            if(inputs.stream().anyMatch(group->group.isEmpty())&&width==0)return;
            var costs=new LinkedHashMap<String,Integer>();var amounts=arcane.visCost(grid); amounts.forEach((a,v)->costs.put(a.tag(),ArcaneRecipe.hundredths(amounts,a)));
            if(!arcane.matches(grid,level))return;
            var result=arcane.assemble(grid,level.registryAccess());
            String key=id+(example==null?"":"/"+result.toString()+result.getTag());
            out.put(key,new View(ResearchPage.Type.ARCANE_CRAFTING,id,result,inputs,costs,width,0,0,""));
        } else if(recipe instanceof CrucibleRecipe crucible) {
            if(!thaumcraft.api.research.ResearchCategories.enabled(crucible.research())||crucible.displayCatalyst().getItems().length==0)return;
            out.put(id,new View(ResearchPage.Type.CRUCIBLE_CRAFTING,id,crucible.getResultItem(level.registryAccess()),List.of(Arrays.asList(crucible.displayCatalyst().getItems())),BrowserRecipe.amounts(crucible.costs()),0,0,0,""));
        } else if(recipe instanceof InfusionRecipe infusion) {
            if(!thaumcraft.api.research.ResearchCategories.enabled(infusion.research()))return;
            if(infusion.enchantmentRecipe()) {
                for(var preview:infusion.enchantmentPreviews()) {
                    var plan=preview.plan();String key=id+"/"+preview.before()+"/"+preview.after();
                    out.put(key,new View(ResearchPage.Type.INFUSION_ENCHANTMENT,id,preview.book(),plan.components().stream().map(InfusionIngredient::displayStacks).toList(),BrowserRecipe.amounts(plan.aspects()),0,plan.instability(),plan.xp(),preview.before()+" → "+preview.after()));
                }
            } else {
                var json=infusion.displayDefinition();
                if(json.has("center"))for(var center:new InfusionIngredient(json.getAsJsonObject("center")).displayStacks())infusion(level,infusion,center,out);
            }
        } else if(recipe instanceof CraftingRecipe crafting) {
            var inputs=ingredients(crafting.getIngredients());
            if(inputs.isEmpty())return;
            int width=crafting instanceof ShapedRecipe shaped?shaped.getWidth():0;
            out.put(id,new View(ResearchPage.Type.NORMAL_CRAFTING,id,crafting.getResultItem(level.registryAccess()).copy(),inputs,Map.of(),width,0,0,""));
        } else if(recipe instanceof SmeltingRecipe smelting) {
            out.put(id,new View(ResearchPage.Type.SMELTING,id,smelting.getResultItem(level.registryAccess()).copy(),ingredients(smelting.getIngredients()),Map.of(),0,0,0,""));
        }
    }
    private static void infusion(Level level,InfusionRecipe recipe,ItemStack center,Map<String,View> out) {
        if(!recipe.acceptsCenter(center))return;
        var ingredients=recipe.plan(center).components().stream().map(InfusionIngredient::displayStacks).toList();
        if(ingredients.stream().anyMatch(List::isEmpty))return;
        infusion(level,recipe,center,ingredients.stream().map(i->i.get(0)).toList(),out);
    }
    private static void infusion(Level level,InfusionRecipe recipe,ItemStack center,List<ItemStack> components,Map<String,View> out) {
        // Dynamic addon examples choose correlated inputs (e.g. matching armor slots).
        // Validate those actual inputs before asking the live recipe for its output/costs.
        if(!recipe.acceptsCenter(center)||components.stream().anyMatch(ItemStack::isEmpty)
                ||!recipe.matches(new InfusionRecipe.Input(center,components,key->true),level))return;
        var plan=recipe.plan(center);
        var ingredients=plan.components().stream().map(InfusionIngredient::displayStacks).toList();
        if(ingredients.stream().anyMatch(List::isEmpty))return;
        var inputs=new ArrayList<List<ItemStack>>();inputs.add(List.of(center.copyWithCount(1)));inputs.addAll(ingredients);
        String id=recipe.getId().toString();out.put(id+"/"+center.toString()+center.getTag(),new View(ResearchPage.Type.INFUSION_CRAFTING,id,plan.output().apply(center),inputs,BrowserRecipe.amounts(plan.aspects()),0,plan.instability(),plan.xp(),""));
    }
}
