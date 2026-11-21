package dyesmodern;

import com.google.gson.*;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;

/** Real loaded-recipe batches; does not substitute JSON parsing for RecipeManager acceptance. */
@GameTestHolder("thaumicdyes") @PrefixGameTestTemplate(false)
public final class DyesDataServerTests {
    private static boolean owned(Recipe<?> recipe){return recipe.getId().getNamespace().equals("thaumicdyes");}
    private static List<Recipe<?>> recipes(GameTestHelper h){return h.getLevel().getRecipeManager().getRecipes().stream().filter(DyesDataServerTests::owned).toList();}
    private static ItemStack sample(JsonElement source){
        var ingredient=Ingredient.fromJson(source);var stacks=ingredient.getItems();
        if(stacks.length==0)throw new IllegalStateException("Ingredient has no registered sample: "+source);
        return stacks[0].copyWithCount(1);
    }
    private static ItemStack pedestal(JsonObject definition){
        var stacks=new InfusionIngredient(definition).displayStacks();if(stacks.isEmpty())throw new IllegalStateException("Empty pedestal ingredient: "+definition);return stacks.get(0).copyWithCount(1);
    }
    @SuppressWarnings({"rawtypes","unchecked"}) private static <T extends Recipe<?>> T network(T recipe){
        var buffer=new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());try{RecipeSerializer serializer=recipe.getSerializer();serializer.toNetwork(buffer,recipe);return (T)serializer.fromNetwork(recipe.getId(),buffer);}finally{buffer.release();}
    }
    private static TransientCraftingContainer grid(){return new TransientCraftingContainer(new AbstractContainerMenu(null,0){
        @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int slot){return ItemStack.EMPTY;}
        @Override public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}
    },3,3);}
    @GameTest(template="empty",timeoutTicks=200) public static void all186InfusionsMatchTheirRealIngredientsRejectMissingResearchAndPreservePatchNbt(GameTestHelper h)throws Exception{
        var rows=recipes(h).stream().filter(InfusionRecipe.class::isInstance).map(InfusionRecipe.class::cast).toList();h.assertTrue(rows.size()==186,"RecipeManager loads all 186 original infusions");int patches=0,upgrades=0;
        for(var recipe:rows){
            String name=recipe.getId().toString();var definition=recipe.displayDefinition();var center=pedestal(definition.getAsJsonObject("center"));center.getOrCreateTag().putString("DyesValidationOwner","keep");
            var components=new ArrayList<ItemStack>();for(var value:definition.getAsJsonArray("components")){
                var pedestal=value.getAsJsonObject();var stack=pedestal(pedestal);components.add(stack);
                if(pedestal.getAsJsonObject("ingredient").has("nbt")){var wrong=stack.copy();wrong.getOrCreateTag().putString("Potion","minecraft:water");h.assertTrue(!new InfusionIngredient(pedestal).matches(wrong),name+" rejects correct potion item with incorrect effect NBT");}
            }
            var input=new InfusionRecipe.Input(center,components,key->true);h.assertTrue(recipe.matches(input,h.getLevel()),name+" accepts exact registered center and components");
            h.assertTrue(!recipe.matches(new InfusionRecipe.Input(center,components,key->false),h.getLevel()),name+" enforces actual research predicate");
            h.assertTrue(!recipe.matches(new InfusionRecipe.Input(center,components.subList(0,components.size()-1),key->true),h.getLevel()),name+" rejects missing pedestal ingredient");
            var result=recipe.assemble(input,h.getLevel().registryAccess());var declared=definition.getAsJsonObject("result");h.assertTrue(!result.isEmpty(),name+" assembles a real item");
            if(declared.has("patch")){
                patches++;var patch=TagParser.parseTag(declared.get("patch").getAsString());if(patch.contains("upgrade"))upgrades++;
                h.assertTrue(result.getItem()==center.getItem()&&result.getTag().getString("DyesValidationOwner").equals("keep"),name+" patches the live center without deleting unrelated NBT");
                for(String field:patch.getAllKeys())h.assertTrue(result.getTag().get(field).equals(patch.get(field)),name+" keeps exact NBT type and value");
            }else{
                h.assertTrue(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(result.getItem()).toString().equals(declared.get("item").getAsString()),name+" produces its fixed final-version item");
                h.assertTrue(!result.getOrCreateTag().contains("DyesValidationOwner"),name+" follows original new-item output semantics");
            }
            h.assertTrue(center.getTag().getString("DyesValidationOwner").equals("keep")&&!center.getTag().contains("upgrade"),name+" preview leaves inputs unchanged");
            var decoded=network(recipe);h.assertTrue(decoded.displayDefinition().equals(definition)&&decoded.matches(input,h.getLevel()),name+" network roundtrip retains complete recipe");
        }
        h.assertTrue(patches==140&&upgrades==130,"All ten runic upgrades and ten goggles/mask transforms loaded");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=200) public static void all19ArcaneRecipesUseRealGridsResearchVisAndCrimsonBannerNbt(GameTestHelper h){
        var rows=recipes(h).stream().filter(ArcaneRecipe.class::isInstance).map(ArcaneRecipe.class::cast).toList();h.assertTrue(rows.size()==19,"RecipeManager loads all 19 original arcane recipes");
        for(var recipe:rows){
            var definition=recipe.displayDefinition();String name=recipe.getId().toString();var grid=grid();var pattern=definition.getAsJsonArray("pattern");var mapping=definition.getAsJsonObject("key");int occupied=-1;
            for(int y=0;y<pattern.size();y++){String row=pattern.get(y).getAsString();for(int x=0;x<row.length();x++){char symbol=row.charAt(x);if(symbol!=' '){occupied=y*3+x;grid.setItem(occupied,sample(mapping.get(String.valueOf(symbol))));}}}
            h.assertTrue(recipe.matches(grid,h.getLevel())&&recipe.unlocked(grid,key->true)&&!recipe.unlocked(grid,key->false),name+" real shaped grid and research gate");
            var cost=recipe.visCost(grid);for(var entry:definition.getAsJsonObject("vis").entrySet())h.assertTrue(cost.get(tc4revival.aspect.TCAspects.catalog().require(entry.getKey()))==entry.getValue().getAsInt(),name+" original primal vis amount");
            var output=recipe.assemble(grid,h.getLevel().registryAccess());h.assertTrue(!output.isEmpty(),name+" has a real output");
            if(recipe.getId().getPath().equals("crimson_banner"))h.assertTrue(output.getTag().getCompound("BlockEntityTag").getList("Patterns",10).getCompound(0).getString("Pattern").equals("tc_crimson"),"Modern red banner retains the real registered Crimson pattern");
            h.assertTrue(network(recipe).matches(grid,h.getLevel()),name+" survives network serialization");grid.setItem(occupied,ItemStack.EMPTY);h.assertTrue(!recipe.matches(grid,h.getLevel()),name+" rejects a missing shaped ingredient");
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void allThreeCrucibleConversionsRequireExactEssentiaAndResearch(GameTestHelper h){
        var rows=recipes(h).stream().filter(CrucibleRecipe.class::isInstance).map(CrucibleRecipe.class::cast).toList();h.assertTrue(rows.size()==3,"RecipeManager loads all three void-robe conversions");
        for(var recipe:rows){
            var catalyst=recipe.displayCatalyst().getItems()[0].copy();var amounts=recipe.costs();var good=new CrucibleRecipe.Input(catalyst,amounts,key->true);String name=recipe.getId().toString();
            h.assertTrue(recipe.matches(good,h.getLevel()),name+" exact original essentia and registered catalyst");h.assertTrue(!recipe.matches(new CrucibleRecipe.Input(catalyst,amounts,key->false),h.getLevel()),name+" requires research");
            for(var aspect:amounts.aspects()){var shortOne=amounts.copy().remove(aspect,1);h.assertTrue(!recipe.matches(new CrucibleRecipe.Input(catalyst,shortOne,key->true),h.getLevel()),name+" rejects one missing "+aspect.tag());}
            h.assertTrue(recipe.remainder(amounts).orElseThrow().isEmpty(),name+" consumes exact essentia");h.assertTrue(!recipe.assemble(good,h.getLevel().registryAccess()).isEmpty()&&network(recipe).matches(good,h.getLevel()),name+" output and network recipe work");
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void liveCatalogMirrorsAndRecipesProvideAll57Items(GameTestHelper h){
        var all=recipes(h);h.assertTrue(all.size()==209,"All 208 fixed recipes and the eight-family dynamic dye recipe loaded together");
        var obtainable=new HashSet<Item>();for(var recipe:all){var output=recipe.getResultItem(h.getLevel().registryAccess());if(!output.isEmpty())obtainable.add(output.getItem());}
        h.assertTrue(DyesItems.ALL.size()==57,"Final original item catalog contains 57 items");DyesItems.ALL.forEach((name,item)->h.assertTrue(obtainable.contains(item.get()),"Actual survival output exists: "+name));
        var catalog=tc4revival.research.ResearchCatalogs.catalog();var research=catalog.all().stream().filter(r->r.key().startsWith("thaumicdyes:")).toList();h.assertTrue(research.size()==37&&research.stream().filter(tc4revival.research.ResearchDefinition::mirror).count()==5,"Real server merged all research and five core mirrors");
        for(var row:research){for(String parent:row.parents())h.assertTrue(catalog.find(parent).isPresent(),row.key()+" has live parent "+parent);for(String parent:row.hiddenParents())h.assertTrue(catalog.find(parent).isPresent(),row.key()+" has live hidden parent "+parent);if(row.mirror())h.assertTrue(!catalog.original(row.key()).key().startsWith("thaumicdyes:"),row.key()+" resolves to real core research");else h.assertTrue(!tc4revival.research.ResearchPages.bundled().require(row.key()).isEmpty(),row.key()+" has actual book components");}
        h.succeed();
    }
    @GameTest(template="empty") public static void originalExplicitAspectsLoadForAll34ItemsAndTheRealCultistLeader(GameTestHelper h)throws Exception{
        try(var stream=DyesDataServerTests.class.getResourceAsStream("/data/thaumicdyes/item_aspects/original.json");var reader=new java.io.InputStreamReader(Objects.requireNonNull(stream),java.nio.charset.StandardCharsets.UTF_8)){
            var rules=tc4revival.aspect.item.ItemAspectJson.decode(JsonParser.parseReader(reader));h.assertTrue(rules.size()==34,"All original explicit item assignments are present");
            for(var rule:rules){var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(rule.items().get(0)));h.assertTrue(item!=null&&item!=Items.AIR,"Aspect target exists in actual registry");var actual=tc4revival.aspect.item.ItemAspects.direct(new ItemStack(item)).orElseThrow();h.assertTrue(actual.view().equals(rule.amounts(tc4revival.aspect.TCAspects.catalog()).view()),rule.items()+" actual direct aspects match original registration");}
        }
        var leader=TCEntities.CULTIST_LEADER.get().create(h.getLevel());var actual=tc4revival.aspect.entity.EntityAspects.resolve(leader).orElseThrow();h.assertTrue(actual.legacyIdentity().equals("Thaumcraft.CultistLeader"),"Leader keeps original scan identity");
        var expected=Map.of("alienis",2,"humanus",2,"perditio",1,"praecantatio",2);h.assertTrue(actual.aspects().size()==expected.size(),"Leader has the original four-aspect override");expected.forEach((tag,amount)->h.assertTrue(actual.aspects().amount(tc4revival.aspect.TCAspects.catalog().require(tag))==amount,"Leader exact "+tag));h.succeed();
    }
}
