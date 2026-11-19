package tc4revival.aspect.recipe;

import java.util.*;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.*;
import tc4revival.aspect.item.StackAspectBonuses;
import static org.junit.jupiter.api.Assertions.*;

class Report0919AspectTest {
    @Test void reusedBaseSessionReturnsIndependentResultsAndIsolatesTraversalCaches() {
        var catalog=new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
        var data=LegacyRecipeFixture.data();
        var identity=new tc4revival.aspect.item.LegacyItemIdentity(Map.of("minecraft:beacon",new RecipeAspectData.Key("minecraft:beacon",0)),Map.of(),data);
        var session=new tc4revival.aspect.item.ItemBaseAspectSession(identity,data,catalog,new tc4revival.aspect.item.ItemAspectResolver(List.of(),catalog),(a,b)->false);
        var first=session.base("minecraft:beacon").orElseThrow();var expected=first.copy();
        first.remove(first.aspects().get(0));
        assertEquals(expected.view(),session.base("minecraft:beacon").orElseThrow().view());
        var canonical=session.canonical("minecraft:beacon").orElseThrow();canonical.remove(canonical.aspects().get(0));
        assertEquals(expected.view(),session.base("minecraft:beacon").orElseThrow().view());
    }
    @Test void balancedShardKeepsAllEightLegacyRegisteredAspects() {
        var catalog=new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
        var base=new RecipeAspectEngine(LegacyRecipeFixture.data(),catalog).object(new RecipeAspectData.Key("Thaumcraft:ItemShard",6));
        var result=new StackAspectBonuses(catalog).apply(base,null,null,List.of());
        assertEquals(8,result.size());
        assertEquals(2,result.amount(catalog.require("aer")));
        for(String tag:List.of("praecantatio","vitreus","ignis","aqua","terra","ordo","perditio"))assertEquals(1,result.amount(catalog.require(tag)),tag);
    }
    @Test void allocationFreeKeyValidationRetainsTheFullLegacyGrammar() {
        var grammar=java.util.regex.Pattern.compile("[A-Za-z0-9_.-]+:[A-Za-z0-9/._-]+");
        var samples=new ArrayList<>(List.of("Thaumcraft:ItemShard","addon:path/name","a:b",":b","a:","a:b:c","a/b:c","a:空间","a:b\n",""));
        var random=new Random(919);String alphabet="ABCabc019_./-: @\n";
        for(int i=0;i<1000;i++) { var b=new StringBuilder();for(int n=random.nextInt(30);n>0;n--)b.append(alphabet.charAt(random.nextInt(alphabet.length())));samples.add(b.toString()); }
        for(String value:samples)assertEquals(grammar.matcher(value).matches(),RecipeAspectData.validItem(value),value);
        assertThrows(IllegalArgumentException.class,()->new RecipeAspectData.Key(null,0));
    }
}
