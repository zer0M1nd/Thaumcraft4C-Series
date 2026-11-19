package tc4revival.aspect.entity;
import java.util.*;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.*;
import static org.junit.jupiter.api.Assertions.*;
class EntityAspectContributionsTest {
    private final AspectCatalog catalog=new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    private final EntityFacts facts=new EntityFacts("minecraft:pig",null,false,false,false,false,false,-1,"");
    @Test void orderedOperationsPreserveScanIdentityAndCopyBase() {
        var json=JsonParser.parseString("""
            {"format":1,"entries":[
            {"entity":"minecraft:pig","operation":"add","aspects":{"sensus":2}},
            {"entity":"minecraft:pig","operation":"merge","aspects":{"sensus":1,"bestia":4}},
            {"entity":"absent:mob","operation":"add","optional":true,"aspects":{"aer":1}}]}
            """).getAsJsonObject();
        var entries=EntityAspectContributions.decode(json,id->id.equals("minecraft:pig"));
        var base=new EntityAspectResolver.Result(new AspectAmounts().add(catalog.require("sensus"),1),"PigCHILD");
        var result=new EntityAspectContributions(entries,catalog).apply(facts,base);
        assertEquals(3,result.aspects().amount(catalog.require("sensus")));assertEquals(4,result.aspects().amount(catalog.require("bestia")));
        assertEquals(base.scanHash(),result.scanHash());assertEquals(1,base.aspects().total());
        var replace=new EntityAspectContributions.Entry(new EntityAspectRule("minecraft:pig","ignored",EntityAspectRule.Variant.ANY,"",List.of(new EntityAspectRule.Quantity("aer",5))),"replace");
        assertEquals(5,new EntityAspectContributions(List.of(replace),catalog).apply(facts,result).aspects().total());
    }
    @Test void unknownRequiredEntityAndOperationFail() {
        var json=JsonParser.parseString("""
            {"format":1,"entries":[{"entity":"missing:mob","operation":"add","aspects":{"aer":1}}]}
            """).getAsJsonObject();
        assertThrows(IllegalArgumentException.class,()->EntityAspectContributions.decode(json,id->false));
    }
}
