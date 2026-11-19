package tc4revival.aspect.recipe;

import java.util.*;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.*;
import tc4revival.aspect.item.*;
import tc4revival.aspect.recipe.RecipeAspectData.*;
import static org.junit.jupiter.api.Assertions.*;

class RuntimeCraftingSessionTest {
    private final AspectCatalog catalog=new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    private final RecipeAspectData legacy=new RecipeAspectData(Map.of(),List.of(),List.of());
    private Key key(String id){return new Key(id,0);}
    private Recipe recipe(String output,int count,Ingredient... ingredients){return new Recipe(Kind.CRAFTING,key(output),count,List.of(ingredients),Map.of());}
    private ItemBaseAspectSession session(List<Recipe> recipes,List<ItemAspectRule> rules){
        var items=new HashMap<String,ItemProperties>();
        for(var r:recipes){items.put(r.output().item(),new ItemProperties(false,true,null));for(var in:r.ingredients())for(var k:in.alternatives())items.put(k.item(),new ItemProperties(false,true,null));}
        return new ItemBaseAspectSession(new LegacyItemIdentity(Map.of(),Map.of(),legacy),legacy,catalog,
                new ItemAspectResolver(rules,catalog),(id,tag)->tag.equals("test:metals"),(k,v)->v,new RecipeAspectData(items,List.of(),recipes));
    }
    private ItemAspectRule rule(String item,String tag,int amount){return new ItemAspectRule(List.of(item),tag,List.of(new ItemAspectRule.Quantity("terra",amount)),false);}
    @Test void modernOnlyItemsDeriveRecursivelyWithCountsAndTagAlternatives(){
        var recipes=List.of(recipe("addon:part",2,new Ingredient(List.of(key("addon:unknown"),key("addon:metal")),true)),
                recipe("addon:device",1,Ingredient.direct(key("addon:part")),Ingredient.direct(key("addon:part"))));
        var s=session(recipes,List.of(rule("addon:metal","test:metals",16)));
        assertEquals(6,s.base("addon:part").orElseThrow().amount(catalog.require("terra")));
        assertEquals(9,s.base("addon:device").orElseThrow().amount(catalog.require("terra")));
    }
    @Test void replacementAndRemovalChangeFreshGenerationWithoutMutatingExistingCache(){
        var r=recipe("addon:device",1,Ingredient.direct(key("addon:metal")));
        var rules=List.of(rule("addon:metal",null,16));
        var before=session(List.of(r),rules);assertEquals(12,before.base("addon:device").orElseThrow().total());
        var after=session(List.of(recipe("addon:device",2,Ingredient.direct(key("addon:metal")))),rules);
        assertEquals(6,after.base("addon:device").orElseThrow().total());
        assertTrue(session(List.of(),rules).base("addon:device").isEmpty());
        assertEquals(12,before.base("addon:device").orElseThrow().total());
    }
    @Test void explicitEmptyOverridesRecipeAndCycleDoesNotRecurseForever(){
        var r=recipe("addon:device",1,Ingredient.direct(key("addon:metal")));
        var empty=new ItemAspectRule(List.of("addon:device"),null,List.of(),false);
        assertTrue(session(List.of(r),List.of(rule("addon:metal",null,16),empty)).base("addon:device").orElseThrow().isEmpty());
        var cycle=session(List.of(recipe("addon:a",1,Ingredient.direct(key("addon:b"))),recipe("addon:b",1,Ingredient.direct(key("addon:a")))),List.of());
        assertTrue(cycle.base("addon:a").isEmpty());
    }
}
