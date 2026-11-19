package tc4revival.research;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BrowserResearchGateTest {
    @Test void creativeBypassesResearchWhileSurvivalRequiresExactMarker() {
        assertTrue(BrowserRecipes.unlocked("",Set.of(),false));
        assertFalse(BrowserRecipes.unlocked("INFUSION",Set.of("INFUSIONOTHER"),false));
        assertTrue(BrowserRecipes.unlocked("INFUSION",Set.of("INFUSION"),false));
        assertTrue(BrowserRecipes.unlocked("INFUSION",Set.of(),true));
    }
}
