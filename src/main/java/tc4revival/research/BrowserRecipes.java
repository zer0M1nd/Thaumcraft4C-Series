package tc4revival.research;
import java.util.*;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.content.*;
import tc4revival.aspect.*;
import tc4revival.aspect.item.*;

public class BrowserRecipes {
    public static boolean unlocked(String key,Set<String> research,boolean creative){return key.isEmpty()||creative||research.contains(key);}
    /** One work unit is one ordinary recipe or one dynamic central item, never a whole registry. */
    public static class Catalog {
        private final Iterator<java.util.function.Consumer<List<BrowserRecipe>>> work;
        private long completed;
        private Catalog(Iterator<java.util.function.Consumer<List<BrowserRecipe>>> work){this.work=work;}
        public boolean done(){return !work.hasNext();}
        public long completedWork(){return completed;}
        public List<BrowserRecipe> drain(int units,long nanos){
            var rows=new ArrayList<BrowserRecipe>();long start=System.nanoTime();
            for(int i=0;i<units&&work.hasNext();i++){
                work.next().accept(rows);completed++;
                if(System.nanoTime()-start>=nanos)break;
            }
            return rows;
        }
    }
    public static Catalog catalog(ServerPlayer player){
        return catalog(player.serverLevel(),player.getInventory().items.stream().filter(s->!s.isEmpty()&&s.hasTag()).map(s->s.copyWithCount(1)).toList(),LegacyItemMappings.identity());
    }
    public static Catalog catalog(Level level,LegacyItemIdentity identity){return catalog(level,List.of(),identity);}
    private static Catalog catalog(Level level,List<ItemStack> inventory,LegacyItemIdentity identity){
        // Browsing locked recipes must not unlock crafting; recipe execution keeps its own checks.
        Predicate<String> knows=key->true;
        var first=new ArrayList<java.util.function.Consumer<List<BrowserRecipe>>>();
        for(var r:level.getRecipeManager().getAllRecipesFor(TCArcane.RECIPE_TYPE.get()))first.add(out->arcane(out,r,level,knows,identity));
        for(var r:level.getRecipeManager().getAllRecipesFor(TCArcane.CRUCIBLE_RECIPE_TYPE.get())) {
            if(!knows.test(r.research())||!thaumcraft.api.research.ResearchCategories.enabled(r.research()))continue;
            first.add(out->out.add(new BrowserRecipe("crucible",r.getId().toString(),r.research(),List.of(Arrays.asList(r.displayCatalyst().getItems())),r.getResultItem(level.registryAccess()),BrowserRecipe.amounts(r.costs()),Map.of(),0,0,false)));
        }
        for(var aspect:TCAspects.catalog().all()) {
            if(aspect.isPrimal())continue;
            first.add(out->{var parts=new LinkedHashMap<String,Integer>();aspect.components().forEach(tag->parts.merge(tag,1,Integer::sum));
                out.add(new BrowserRecipe("aspect_combine",aspect.tag(),"",List.of(),ItemStack.EMPTY,parts,Map.of(aspect.tag(),1),0,0,false));});
        }
        for(var recipe:level.getRecipeManager().getAllRecipesFor(TCArcane.INFUSION_RECIPE_TYPE.get())){
            if(recipe.enchantmentRecipe()&&thaumcraft.api.research.ResearchCategories.enabled(recipe.research()))
                first.add(out->out.addAll(enchantments(recipe)));
        }
        return new Catalog(new Iterator<java.util.function.Consumer<List<BrowserRecipe>>>(){
            private final Iterator<java.util.function.Consumer<List<BrowserRecipe>>> ordinary=first.iterator();
            private final Iterator<InfusionRecipe> recipes=level.getRecipeManager().getAllRecipesFor(TCArcane.INFUSION_RECIPE_TYPE.get()).iterator();
            private Iterator<ItemStack> centers=Collections.emptyIterator();
            private InfusionRecipe current;private String research;private boolean dynamic;private int index;
            public boolean hasNext(){
                if(ordinary.hasNext())return true;
                while(!centers.hasNext()&&recipes.hasNext()){
                    current=recipes.next();var json=current.displayDefinition();research=json.has("research")?json.get("research").getAsString():"";
                    if(current.enchantmentRecipe())continue;
                    if(!knows.test(research)||!thaumcraft.api.research.ResearchCategories.enabled(research))continue;
                    dynamic=!json.has("center");index=0;
                    centers=dynamic?java.util.stream.Stream.concat(ForgeRegistries.ITEMS.getValues().stream().filter(item->item!=Items.AIR).map(ItemStack::new),inventory.stream()).iterator():new InfusionIngredient(json.getAsJsonObject("center")).displayStacks().iterator();
                }
                return centers.hasNext();
            }
            public java.util.function.Consumer<List<BrowserRecipe>> next(){
                if(ordinary.hasNext())return ordinary.next();
                if(!hasNext())throw new NoSuchElementException();
                var r=current;var center=centers.next();String key=research;boolean varying=dynamic;int number=index++;
                return out->infusion(out,r,level,knows,center,key,varying,number);
            }
        });
    }
    public static List<BrowserRecipe> recipes(ServerPlayer player) {
        var work=catalog(player);var rows=new ArrayList<BrowserRecipe>();
        while(!work.done())rows.addAll(work.drain(512,Long.MAX_VALUE));
        return rows;
    }
    /** Incremental stack lookup; dynamic infusion variants follow the new stacks, not the whole registry. */
    public static Catalog items(ServerPlayer player,List<ItemStack> stacks) {
        var level=player.serverLevel();
        var work=new ArrayList<java.util.function.Consumer<List<BrowserRecipe>>>();
        for(var stack:stacks){
            if(!BrowserQueryStacks.eligible(stack))continue;
            work.add(out->item(stack).ifPresent(out::add));
            addDynamic(work,level,stack);
        }
        return new Catalog(work.iterator());
    }
    /** Uses vanilla-synchronized recipe definitions; valid on either logical side. */
    public static List<BrowserRecipe> dynamicItems(Level level,ItemStack stack){
        var work=new ArrayList<java.util.function.Consumer<List<BrowserRecipe>>>();addDynamic(work,level,stack);
        var rows=new ArrayList<BrowserRecipe>();work.forEach(task->task.accept(rows));return rows;
    }
    private static void addDynamic(List<java.util.function.Consumer<List<BrowserRecipe>>> work,Level level,ItemStack stack){
        if(!stack.hasTag())return;
        for(var recipe:level.getRecipeManager().getAllRecipesFor(TCArcane.INFUSION_RECIPE_TYPE.get())){
            if(!recipe.dynamicCenter()||recipe.enchantmentRecipe())continue;
            String research=recipe.research();
            if(!thaumcraft.api.research.ResearchCategories.enabled(research))continue;
            work.add(out->infusion(out,recipe,level,key->true,stack,research,true,0));
        }
    }
    /** Capture once per recipe reload, not once per NBT stack. Each recipe is a bounded work unit. */
    public static List<InfusionRecipe> dynamicDefinitions(Level level){
        return level.getRecipeManager().getAllRecipesFor(TCArcane.INFUSION_RECIPE_TYPE.get()).stream()
                .filter(InfusionRecipe::dynamicCenter).filter(r->!r.enchantmentRecipe()).filter(r->thaumcraft.api.research.ResearchCategories.enabled(r.research())).toList();
    }
    public static List<BrowserRecipe> dynamicItem(Level level,ItemStack stack,InfusionRecipe recipe){
        if(recipe.enchantmentRecipe())return List.of();
        var rows=new ArrayList<BrowserRecipe>();infusion(rows,recipe,level,key->true,stack,recipe.research(),true,0);return rows;
    }
    public static Optional<BrowserRecipe> item(ItemStack stack,PlayerKnowledge knowledge,boolean creative) {
        return item(stack);
    }
    /** JEI is a public reference; scanning gates gameplay knowledge, not this item lookup. */
    public static Optional<BrowserRecipe> item(ItemStack stack) {
        if(stack.isEmpty())return Optional.empty();
        var values=ItemStackAspects.resolve(stack).orElseGet(AspectAmounts::new);
        return Optional.of(item(stack,ScanIdentity.item(stack),values));
    }
    public static BrowserRecipe item(ItemStack stack,String identity,AspectAmounts values){
        var shown=new LinkedHashMap<String,Integer>();for(var a:values.alphabetical())shown.put(a.tag(),values.amount(a));
        return new BrowserRecipe("item_aspects",identity,"",List.of(List.of(stack.copyWithCount(1))),ItemStack.EMPTY,Map.of(),shown,0,0,false);
    }
    private static TransientCraftingContainer grid(){return new TransientCraftingContainer(new AbstractContainerMenu(null,-1){public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int i){return ItemStack.EMPTY;}},3,3);}
    private static void arcane(List<BrowserRecipe> out,ArcaneRecipe recipe,Level level,Predicate<String> knows,LegacyItemIdentity identity) {
        var json=recipe.displayDefinition();String research=json.has("research")?json.get("research").getAsString():"";
        if(!knows.test(research)||!thaumcraft.api.research.ResearchCategories.enabled(research))return;
        if(json.has("display_inputs")) {
            var page=new ResearchPage(ResearchPage.Type.ARCANE_CRAFTING,Optional.empty(),Optional.empty(),Optional.empty(),Map.of(),Optional.empty(),new ResearchPage.Reference(List.of(recipe.getId().toString()),"modern")).withId("thaumcraft:browser_preview/"+recipe.getId());
            for(var row:LiveBookRecipes.resolve(level,page)) {
                var choices=new ArrayList<List<ItemStack>>(Collections.nCopies(9,List.of()));
                var grid=grid();int width=row.width()==0?3:row.width();
                for(int i=0;i<row.inputs().size();i++) {
                    int slot=i%width+3*(i/width);var values=row.inputs().get(i);choices.set(slot,values);
                    if(!values.isEmpty())grid.setItem(slot,values.get(0).copy());
                }
                if(recipe.matches(grid,level)&&recipe.unlocked(grid,knows))addArcane(out,recipe,level,grid,choices,research,0);
            }
            return;
        }
        String mode=json.has("mode")?json.get("mode").getAsString():"shaped";
        if(mode.equals("wand")||mode.equals("sceptre")) {
            boolean sceptre=mode.equals("sceptre");int n=0;
            for(var rod:WandParts.Rod.values())for(var cap:WandParts.Cap.values()) {
                if(cap==WandParts.Cap.SILVER&&!tc4revival.content.SilverCaps.available())continue;
                if(!knows.test(rod.research())||!knows.test(cap.research())||(sceptre&&!knows.test("SCEPTRE")))continue;
                var grid=grid();var tip=legacy(cap.item==null?"Thaumcraft:WandCap":cap.item,cap.metadata,identity);
                var core=legacy(rod.item!=null?rod.item:rod==WandParts.Rod.WOOD?"minecraft:stick":"Thaumcraft:WandRod",rod.metadata,identity);
                if(tip.isEmpty()||core.isEmpty())continue;
                grid.setItem(4,core);grid.setItem(6,tip.copy());grid.setItem(sceptre?1:2,tip.copy());
                if(sceptre){grid.setItem(5,tip.copy());grid.setItem(2,legacy("Thaumcraft:ItemResource",15,identity));}
                if(!recipe.matches(grid,level)||!recipe.unlocked(grid,knows))continue;
                var choices=new ArrayList<List<ItemStack>>();for(int i=0;i<9;i++)choices.add(grid.getItem(i).isEmpty()?List.of():List.of(grid.getItem(i).copy()));
                addArcane(out,recipe,level,grid,choices,research+"",n++);
            }
            return;
        }
        CraftingRecipe crafting=mode.equals("shapeless")?RecipeSerializer.SHAPELESS_RECIPE.fromJson(recipe.getId(),json):RecipeSerializer.SHAPED_RECIPE.fromJson(recipe.getId(),json);
        var choices=new ArrayList<List<ItemStack>>(Collections.nCopies(9,List.of()));var grid=grid();int width=crafting instanceof ShapedRecipe shaped?shaped.getWidth():3;
        for(int i=0;i<crafting.getIngredients().size();i++){int slot=i%width+3*(i/width);var values=Arrays.asList(crafting.getIngredients().get(i).getItems());choices.set(slot,values);if(!values.isEmpty())grid.setItem(slot,values.get(0).copy());}
        if(recipe.matches(grid,level))addArcane(out,recipe,level,grid,choices,research,0);
    }
    private static void addArcane(List<BrowserRecipe> out,ArcaneRecipe r,Level level,TransientCraftingContainer grid,List<List<ItemStack>> choices,String research,int n){
        var costs=new LinkedHashMap<String,Integer>();var amounts=r.visCost(grid); amounts.forEach((a,v)->costs.put(a.tag(),ArcaneRecipe.hundredths(amounts,a)));
        out.add(new BrowserRecipe("arcane",r.getId()+"/"+n,research,choices,r.assemble(grid,level.registryAccess()),costs,Map.of(),0,0,false));
    }
    private static ItemStack legacy(String item,int metadata,LegacyItemIdentity identity){
        if(item==null)return ItemStack.EMPTY;
        var direct=ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(item.toLowerCase(Locale.ROOT)));
        if(direct!=null&&direct!=Items.AIR)return new ItemStack(direct);
        return identity.items().entrySet().stream().filter(e->e.getValue().item().equals(item)&&e.getValue().metadata()==metadata)
                .map(e->new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(e.getKey())))).findFirst().orElse(ItemStack.EMPTY);
    }
    private static void infusion(List<BrowserRecipe> out,InfusionRecipe r,Level level,Predicate<String> knows,ItemStack center,String research,boolean dynamic,int index) {
        if(r.enchantmentRecipe())return;
        if(center.isEmpty()||!r.acceptsCenter(center))return;
        var plan=r.plan(center);
        var components=plan.components().stream().map(InfusionIngredient::displayStacks).toList();
        if(components.stream().anyMatch(List::isEmpty))return;
        var representatives=components.stream().map(c->c.get(0)).toList();
        if(!r.matches(new InfusionRecipe.Input(center,representatives,knows),level))return;
        // Custom recipes may capture the selected pedestal material during matching.
        // Display that transaction, rather than the generic pre-match output.
        plan=r.plan(center);
        components=plan.components().stream().map(InfusionIngredient::displayStacks).toList();
        var inputs=new ArrayList<List<ItemStack>>();inputs.add(List.of(center.copyWithCount(1)));inputs.addAll(components);
        out.add(new BrowserRecipe("infusion",r.getId()+"/"+index,research,inputs,plan.output().apply(center),BrowserRecipe.amounts(plan.aspects()),Map.of(),plan.instability(),plan.xp(),dynamic));
    }
    public static List<BrowserRecipe> enchantments(InfusionRecipe recipe){
        var rows=new ArrayList<BrowserRecipe>();
        for(var preview:recipe.enchantmentPreviews()){
            var plan=preview.plan();var inputs=new ArrayList<List<ItemStack>>();inputs.add(List.of());
            inputs.addAll(plan.components().stream().map(InfusionIngredient::displayStacks).toList());
            if(inputs.stream().skip(1).anyMatch(List::isEmpty))continue;
            rows.add(new BrowserRecipe("infusion_enchantment",recipe.getId()+"/levels/"+preview.before()+"/"+preview.after(),recipe.research(),
                    inputs,preview.book(),BrowserRecipe.amounts(plan.aspects()),Map.of(),plan.instability(),plan.xp(),false));
        }
        return rows;
    }
}
