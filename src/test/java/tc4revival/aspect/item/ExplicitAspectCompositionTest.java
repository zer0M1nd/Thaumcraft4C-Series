package tc4revival.aspect.item;

import java.util.*;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.*;
import tc4revival.aspect.recipe.*;
import static org.junit.jupiter.api.Assertions.*;

class ExplicitAspectCompositionTest {
    private final AspectCatalog catalog=new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    private ItemAspectRule rule(String tag,int amount){return new ItemAspectRule(List.of("minecraft:chest"),null,List.of(new ItemAspectRule.Quantity(tag,amount)));}
    @Test void baseMergeIsOrderIndependentAndRetainsDistinctTypes(){
        var rows=List.of(rule("arbor",6),rule("arbor",2),rule("vacuos",4));
        var reverse=new ArrayList<>(rows);Collections.reverse(reverse);
        var a=new ItemAspectResolver(rows,catalog).resolve("minecraft:chest",t->true).orElseThrow();
        assertEquals(10,a.total());assertEquals(a.view(),new ItemAspectResolver(reverse,catalog).resolve("minecraft:chest",t->true).orElseThrow().view());
    }
    @Test void optionalBonusUsesSumAcrossDefinitionsAndAgainstBase(){
        var base=new ItemAspectResolver(List.of(rule("arbor",6)),catalog).resolve("minecraft:chest",t->true).orElseThrow();
        var bonus=new ItemAspectResolver(List.of(rule("arbor",2),rule("arbor",3)),catalog,true).resolve("minecraft:chest",t->true).orElseThrow();
        assertEquals(11,new StackAspectBonuses(catalog).apply(base,null,bonus,List.of()).amount(catalog.require("arbor")));
    }
    @Test void explicitEmptyBaseSuppressesLegacyWithoutAFlag(){
        var key=new RecipeAspectData.Key("minecraft:chest",0);
        var data=new RecipeAspectData(Map.of(key.item(),new RecipeAspectData.ItemProperties(false,false,null)),List.of(new RecipeAspectData.Registration(new RecipeAspectData.Key(key.item(),32767),Map.of("arbor",6,"vacuos",4))),List.of());
        var identity=new LegacyItemIdentity(Map.of(key.item(),key),Map.of(),data);
        var empty=new ItemAspectRule(List.of(key.item()),null,List.of());
        var session=new ItemBaseAspectSession(identity,data,catalog,new ItemAspectResolver(List.of(empty),catalog),(i,t)->true);
        assertTrue(session.base(key.item()).orElseThrow().isEmpty());
        assertEquals(10,new ItemBaseAspectSession(identity,data,catalog,new ItemAspectResolver(List.of(),catalog),(i,t)->true).base(key.item()).orElseThrow().total());
    }
    @Test void blockStatesMergeAndExplicitEmptyIsNotMissing(){
        var json=com.google.gson.JsonParser.parseString("{\"format\":1,\"entries\":[{\"block\":\"minecraft:end_portal\",\"aspects\":{}},{\"block\":\"minecraft:water\",\"properties\":{\"level\":\"0\"},\"aspects\":{\"aqua\":3}},{\"block\":\"minecraft:water\",\"aspects\":{\"aqua\":1}}]}");
        var table=new BlockAspectTable(List.of(json),catalog);
        assertTrue(table.resolve("minecraft:end_portal",Map.of()).orElseThrow().isEmpty());
        assertEquals(3,table.resolve("minecraft:water",Map.of("level","0")).orElseThrow().total());
        assertEquals(1,table.resolve("minecraft:water",Map.of("level","1")).orElseThrow().total());
    }
}
