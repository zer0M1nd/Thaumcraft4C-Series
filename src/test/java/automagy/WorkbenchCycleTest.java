package automagy;
import automagy.logic.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class WorkbenchCycleTest {
 @Test void upgradesStackButRecipeAndFuzzyLimitsRemain(){var u=WorkbenchUpgrades.from(new int[]{1,1,1,5});assertEquals(3,u.recipes());assertTrue(u.fuzzy());assertEquals(840,u.heatThreshold());assertEquals(840,WorkbenchUpgrades.from(new int[]{5,5,-1,-1}).heatThreshold());assertEquals(2160,WorkbenchUpgrades.from(new int[]{3,3,3,3}).heatThreshold());}
 @Test void speedFloorAndHeatPenaltyUseLegacyOrder(){var u=WorkbenchUpgrades.from(new int[]{0,0,0,0});assertEquals(4.1f,u.craftTime(10,0,0));assertEquals(200, u.craftTime(0,0,12000));}
 @Test void motionPausesWorkAndCoolingConsumesOnlyWhenOverheated(){var u=WorkbenchUpgrades.from(new int[]{-1,-1,-1,-1});var c=new WorkbenchCycle();assertFalse(c.work(u,0,0,false,()->{throw new AssertionError();}));assertEquals(0,c.state().progress());c.restore(new WorkbenchCycle.State(0,1300,0));assertFalse(c.work(u,0,0,true,()->true));assertEquals(-695,c.state().heat());for(int i=0;i<5;i++)c.passive(false);assertEquals(-694,c.state().heat());}
 @Test void completionOccursAfterTheWorkingTicksAndAddsHeat(){var c=new WorkbenchCycle();var u=WorkbenchUpgrades.from(new int[]{-1,-1,-1,-1});for(int i=0;i<38;i++){c.passive(true);assertFalse(c.work(u,0,0,true,()->false));}c.passive(true);assertTrue(c.work(u,0,0,true,()->false));assertEquals(0,c.state().progress());}
}
