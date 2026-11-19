package automagy.logic;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class DisjunctionRulesTest {
 @Test void strongestRingFullOthersHalf(){assertEquals(19,DisjunctionRules.damage(List.of(15,8,0)));assertEquals(22.5,DisjunctionRules.damage(List.of(15,15)));assertEquals(0,DisjunctionRules.damage(List.of()));}
 @Test void sphereHasStrictBoundary(){assertTrue(DisjunctionRules.inRange(0,8.49,0));assertFalse(DisjunctionRules.inRange(8.5,0,0));assertFalse(DisjunctionRules.inRange(5,5,5));}
}
