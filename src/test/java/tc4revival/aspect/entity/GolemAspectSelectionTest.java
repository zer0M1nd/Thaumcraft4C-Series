package tc4revival.aspect.entity;
import java.util.*;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.*;
import static org.junit.jupiter.api.Assertions.*;
class GolemAspectSelectionTest {
    @Test void materialSpecificAddonDefinitionCannotOverrideAnOrdinaryGolem(){
        var catalog=new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
        var base=new EntityAspectRule("thaumcraft:golem","Thaumcraft.Golem",EntityAspectRule.Variant.ANY,"",List.of(new EntityAspectRule.Quantity("humanus",4)));
        var addon=new EntityAspectRule("thaumcraft:golem","ThaumicHorizons.GolemTH",EntityAspectRule.Variant.ANY,"",List.of(new EntityAspectRule.Quantity("aer",2)),"thaumichorizons:material/rock");
        var rules=EntityAspectJson.decode(EntityAspectJson.encode(List.of(base,addon)));
        var resolver=new EntityAspectResolver(rules,catalog);
        var normal=new EntityFacts("thaumcraft:golem",null,false,false,false,false,false,-1,"WOOD");
        var animated=new EntityFacts("thaumcraft:golem",null,false,false,false,false,false,-1,"thaumichorizons:material/rock");
        assertEquals(4,resolver.resolve(normal).orElseThrow().aspects().amount(catalog.require("humanus")));
        assertEquals(2,resolver.resolve(animated).orElseThrow().aspects().amount(catalog.require("aer")));
        assertTrue(resolver.resolve(animated).orElseThrow().legacyIdentity().startsWith("ThaumicHorizons.GolemTH"));
    }
}
