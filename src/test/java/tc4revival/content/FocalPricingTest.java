package tc4revival.content;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FocalPricingTest {
    @Test void fixedPointBudgetForEachBatchSize(){
        int[] levels={14,21,25,28,30},points={280,612,910,1186,1395};
        for(int count=1;count<=5;count++){
            int level=FocalUpgradePlan.experienceForCount(count);
            assertEquals(levels[count-1],level);
            assertEquals(points[count-1],FocalUpgradePlan.experiencePoints(level));
        }
        assertThrows(IllegalArgumentException.class,()->FocalUpgradePlan.experienceForCount(0));
        assertThrows(IllegalArgumentException.class,()->FocalUpgradePlan.experienceForCount(6));
    }
    @Test void vanillaExperienceCurveBoundaries(){
        assertEquals(0,FocalUpgradePlan.experiencePoints(0));
        assertEquals(352,FocalUpgradePlan.experiencePoints(16));
        assertEquals(394,FocalUpgradePlan.experiencePoints(17));
        assertEquals(1507,FocalUpgradePlan.experiencePoints(31));
        assertEquals(1628,FocalUpgradePlan.experiencePoints(32));
        assertEquals(2920,FocalUpgradePlan.experiencePoints(40));
    }
}
