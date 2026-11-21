package gadomancymodern.familiar;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
public class FamiliarAugmentTest {
    @Test void elementalAndSecondaryConflictsAreSymmetric(){for(var first:FamiliarAugment.REGISTRY.values())for(var second:FamiliarAugment.REGISTRY.values())if(first!=second&&first.element==second.element)assertFalse(second.accepts(Map.of(first.id,1),1));}
    @Test void enhancementsNeedAnElementAndPreserveAuthorsUnboundedLevelRule(){assertFalse(FamiliarAugment.DAMAGE.accepts(Map.of(),1));assertTrue(FamiliarAugment.DAMAGE.accepts(Map.of("fire",1),1));assertFalse(FamiliarAugment.DAMAGE.accepts(Map.of("fire",1),2));assertTrue(FamiliarAugment.DAMAGE.accepts(Map.of("fire",3,"damage",3),4));}
    @Test void ratesAndFractionalVisCostsKeepOriginalUnits(){var augments=Map.of("shock",3,"speed",2);assertEquals(Map.of("aer",9,"perditio",6,"ordo",4,"ignis",2),FamiliarAugment.costs(augments));assertEquals(20,FamiliarAugment.delay(augments));assertEquals(4.3,FamiliarAugment.damage(Map.of("damage",3)),.0001);assertEquals(9.6,FamiliarAugment.range(Map.of("range",2)),.0001);}
}
