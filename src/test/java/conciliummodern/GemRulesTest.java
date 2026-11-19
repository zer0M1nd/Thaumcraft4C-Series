package conciliummodern;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GemRulesTest {
    @Test void damagedSelectionTagsStayWithinEveryAllowedSampleCount(){
        for(int count=1;count<=16;count++)for(int saved:new int[]{Integer.MIN_VALUE,-12345,-17,-1,0,1,16,12345,Integer.MAX_VALUE}){
            int selected=GemRules.selected(saved,count);assertTrue(selected>=0&&selected<count);
            assertEquals(0,Math.floorMod((long)saved-selected,count));
        }
        assertEquals(0,GemRules.selected(Integer.MIN_VALUE,0));
        assertEquals(0,GemRules.sampleCount(Integer.MIN_VALUE));assertEquals(16,GemRules.sampleCount(Integer.MAX_VALUE));
    }
    @Test void bindingRejectsDuplicatesForeignInputsMissingSamplesAndFullGems(){
        for(int stored=0;stored<16;stored++)assertTrue(GemRules.canBind(1,1,0,stored,true));
        for(int rifts=0;rifts<3;rifts++)for(int terra=0;terra<3;terra++)for(int others=0;others<2;others++){
            if(rifts!=1||terra!=1||others!=0)assertFalse(GemRules.canBind(rifts,terra,others,0,true));
        }
        for(int stored:new int[]{-1,16,17,Integer.MAX_VALUE})assertFalse(GemRules.canBind(1,1,0,stored,true));
        assertFalse(GemRules.canBind(1,1,0,0,false));
    }
    @Test void transferPreservesEntryAndUsesOriginalTenfoldExitInBothDirections(){
        for(double source:new double[]{-7654.375,-.25,0,.25,7654.375}){
            assertEquals(source,GemRules.coordinate(source,true,false));assertEquals(source,GemRules.coordinate(source,true,true));
            assertEquals(source*10,GemRules.coordinate(source,false,false));
            assertEquals(source,GemRules.coordinate(GemRules.coordinate(source,false,false),false,true),1e-8);
        }
    }
    @Test void finiteWorldLimitsAreCheckedAfterScaling(){
        for(double limit:new double[]{-29999984,29999984})assertTrue(GemRules.validCoordinate(limit));
        for(double invalid:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,-29999985,29999985})assertFalse(GemRules.validCoordinate(invalid));
        assertFalse(GemRules.validCoordinate(GemRules.coordinate(3000000,false,false)));
        assertTrue(GemRules.validCoordinate(GemRules.coordinate(3000000,false,true)));
    }
}
