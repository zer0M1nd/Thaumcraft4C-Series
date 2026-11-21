package tc4revival.content;

import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.nbt.TagParser;
import net.minecraftforge.gametest.*;
import tc4revival.research.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class InfusionRecipeAuditServerTests {
    private static Set<String> originalInfusionKeys() {
        // This is the 61-recipe TC4 baseline audit; loaded addons have separate recipe audits.
        try(var reader=new java.io.InputStreamReader(java.util.Objects.requireNonNull(InfusionRecipeAuditServerTests.class.getResourceAsStream("/data/thaumcraft/research_page_recipes/legacy.json")),java.nio.charset.StandardCharsets.UTF_8)) {
            var result=new TreeSet<String>();
            for(var row:com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("entries")) {
                var entry=row.getAsJsonObject();
                if(entry.getAsJsonObject("data").get("kind").getAsString().equals("infusion"))result.add(entry.get("key").getAsString());
            }
            return result;
        }catch(java.io.IOException failure){throw new java.io.UncheckedIOException(failure);}
    }
    private static ItemStack stack(ResearchPage.Recipe value,long time) {
        return ResearchDisplayItems.bundled().resolve(((ResearchPage.Item)value).stack(),time).orElseThrow();
    }
    @GameTest(template="wand_test_empty")
    public static void ordinaryInfusionCatalogMatchesLoadedRecipesAndOutputs(GameTestHelper test) {
        var level=test.getLevel();var problems=new ArrayList<String>();var matched=new TreeSet<String>();int rows=0,cases=0;
        boolean mirrors=tc4revival.config.TCConfig.SERVER.allowMirrors.get();tc4revival.config.TCConfig.SERVER.allowMirrors.set(true);
        try {
            for(var baselineKey:originalInfusionKeys()) {
                var entry=ResearchPageRecipes.bundled().require(baselineKey);
                var old=(ResearchPageRecipes.Infusion)entry.definition();
                rows++;
                try {
                    var inputDisplay=ResearchDisplayItems.bundled().find(((ResearchPage.Item)old.input()).stack()).orElseThrow();
                    for(int variant=0;variant<inputDisplay.variants().size();variant++) {
                        var center=stack(old.input(),variant*1000L);var components=old.ingredients().elements().stream().map(value->stack(value,0)).toList();
                        var input=new InfusionRecipe.Input(center,components,key->key.equals(old.research()));
                        var recipe=InfusionRecipe.find(level,input).orElseThrow(()->new AssertionError("no matching loaded recipe"));
                        matched.add(recipe.getId().toString());var plan=recipe.plan(center);
                        if(plan.instability()!=old.instability()||plan.enchanting()||plan.xp()!=0||plan.aspects().size()!=old.aspects().size())throw new AssertionError("base cost/instability mismatch");
                        for(var cost:old.aspects().entrySet())if(plan.aspects().amount(tc4revival.aspect.TCAspects.catalog().require(cost.getKey()))!=cost.getValue())throw new AssertionError("aspect "+cost.getKey());
                        ItemStack expected;
                        if(old.output() instanceof ResearchPage.Item) {
                            expected=stack(old.output(),0);
                            // Book-only explicit default metadata equals the unlinked modern item default.
                            if((expected.is(TCItems.ITEM_MIRROR.get())||expected.is(TCItems.ESSENTIA_MIRROR.get()))&&expected.hasTag()) {
                                expected.getTag().remove("tc4_metadata");if(expected.getTag().isEmpty())expected.setTag(null);
                            }
                        }
                        else {
                            var patch=((ResearchPage.Sequence)old.output()).elements();String key=(String)((ResearchPage.Literal)patch.get(0)).value();
                            var value=TagParser.parseTag("{value:"+((ResearchPage.Nbt)patch.get(1)).value()+"}").get("value");expected=center.copy();expected.getOrCreateTag().put(key,value);
                        }
                        var result=recipe.assemble(input,level.registryAccess());
                        if(!ItemStack.matches(result,expected))throw new AssertionError("output mismatch: actual="+result+" "+result.getTag()+", expected="+expected+" "+expected.getTag());
                        if(recipe.matches(new InfusionRecipe.Input(center,components,key->false),level))throw new AssertionError("research gate missing");
                        var reversed=new ArrayList<>(components);Collections.reverse(reversed);
                        if(!recipe.matches(new InfusionRecipe.Input(center,reversed,key->true),level))throw new AssertionError("component order incorrectly constrained");
                        var missing=new ArrayList<>(components);missing.remove(missing.size()-1);
                        if(recipe.matches(new InfusionRecipe.Input(center,missing,key->true),level))throw new AssertionError("missing component accepted");
                        cases++;
                    }
                }catch(Exception|AssertionError failure){problems.add(entry.key()+": "+failure.getMessage());}
            }
        }finally{tc4revival.config.TCConfig.SERVER.allowMirrors.set(mirrors);}
        for(String problem:problems)tc4revival.TC4Revival.LOGGER.error("Infusion catalog audit: {}",problem);
        test.assertTrue(rows==61&&problems.isEmpty(),"All 61 named old ordinary infusion definitions: "+problems);
        int potions=0;
        for(var entry:ResearchDisplayItems.bundled().entries().values())if(entry.identity().item().equals("minecraft:potion")&&entry.identity().nbt().isEmpty()) {
            var identity=entry.identity();var represented=tc4revival.aspect.item.PotionAspects.data().represented().get(identity.metadata());
            test.assertTrue(represented!=null,"Every named book potion has an independently loaded potion identity: "+identity.metadata());
            var shown=entry.variants().get(0);
            test.assertTrue(shown.item().equals(represented.item())&&shown.tag().isPresent(),"Book potion preserves drink/splash identity");
            try{test.assertTrue(TagParser.parseTag(shown.tag().orElseThrow()).getString("Potion").equals(represented.potion()),"Book potion effect/strength matches old metadata: "+identity.metadata());}
            catch(com.mojang.brigadier.exceptions.CommandSyntaxException invalid){throw new AssertionError(invalid);}
            potions++;
        }
        var extra=level.getRecipeManager().getAllRecipesFor(TCArcane.INFUSION_RECIPE_TYPE.get()).stream().map(recipe->recipe.getId().toString()).filter(id->!matched.contains(id)&&!id.contains("/enchantment/")).sorted().toList();
        tc4revival.TC4Revival.LOGGER.info("PASS ordinaryInfusionCatalogMatchesLoadedRecipesAndOutputs: {} entries, {} variants, {} runtime recipes, {} book potions; remaining dynamic recipes {}",rows,cases,matched.size(),potions,extra);test.succeed();
    }
}
