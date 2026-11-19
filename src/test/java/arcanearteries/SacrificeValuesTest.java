package arcanearteries;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import arcanearteries.SacrificeValues.School;
import arcanearteries.SacrificeValues.Victim;

class SacrificeValuesTest {
    @Test void originalMobYieldsAreFixedAndDoNotScaleWithHealth() {
        int[][] golden = {{625,2250,225,300,375},{1000,4000,300,400,500},{625,2250,225,300,375}};
        for(int s=0;s<3;s++) for(int v=0;v<5;v++)
            assertEquals(golden[s][v], SacrificeValues.mob(School.values()[s],Victim.values()[v]));
    }
    @Test void soulFrayAndHardModeNeverIncreaseSelfYield() {
        for(School school:new School[]{School.THAUMIC,School.BOTANICAL,School.EVIL}) {
            int normal=school==School.BOTANICAL?500:250;
            assertEquals(normal,SacrificeValues.self(school,false,false,0,0));
            assertEquals(normal/2,SacrificeValues.self(school,true,false,0,0));
            assertEquals(normal/10,SacrificeValues.self(school,false,true,0,0));
            assertEquals(normal/20,SacrificeValues.self(school,true,true,0,0));
        }
    }
    @Test void configurableBladeUsesIndependentFrayValue() {
        assertEquals(1201,SacrificeValues.self(School.CUSTOM,true,false,1201,77));
        assertEquals(77,SacrificeValues.self(School.CUSTOM,false,true,1201,77));
        assertEquals(0,SacrificeValues.self(School.CUSTOM,true,true,1201,0));
    }
}
