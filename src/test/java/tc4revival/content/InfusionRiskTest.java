package tc4revival.content;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class InfusionRiskTest {
    @Test void environmentCanCancelOrIncreaseRecipeRisk() {
        assertEquals(0,InfusionRisk.preview(5,-5).actual());
        assertTrue(InfusionRisk.preview(5,-5).safe());
        assertTrue(InfusionRisk.preview(5,-6).safe());
        assertFalse(InfusionRisk.preview(5,-4).safe());
        assertEquals(8,InfusionRisk.preview(5,3).actual());
    }
    @Test void runningCraftColorUsesActualInstabilityIncludingAccumulatedPenalties() {
        assertFalse(new InfusionRisk(2,-10,1).safe());
        assertEquals(0xffff5555,new InfusionRisk(2,-10,1).color());
        assertEquals(0xff55ff55,new InfusionRisk(2,3,-1).color());
    }
    @Test void overflowCannotReportDangerousRecipeAsSafe() {
        assertThrows(ArithmeticException.class,()->InfusionRisk.preview(Integer.MAX_VALUE,1));
    }
}
