package tc4revival.content;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LegacyArmorAbsorptionTest {
    private LegacyArmorAbsorption.Piece piece(int slot,int priority,double ratio,int cap) { return new LegacyArmorAbsorption.Piece(slot,priority,ratio,cap); }
    @Test void ordinaryFullSetUsesLegacyLinearAbsorption() {
        var result=LegacyArmorAbsorption.apply(10,List.of(piece(0,0,.08,300),piece(1,0,.30,600),piece(2,0,.35,640),piece(3,0,.15,440)));
        assertEquals(1.2f,result.remaining(),.00001f);
        assertEquals(1,result.wear().get(0)); assertEquals(3,result.wear().get(1)); assertEquals(3,result.wear().get(2));
    }
    @Test void priorityConsumesDamageBeforeOrdinaryBoots() {
        var result=LegacyArmorAbsorption.apply(10,List.of(piece(0,0,.12,500),piece(1,1,.8,600)));
        assertEquals(1.76f,result.remaining(),.00001f); assertEquals(8,result.wear().get(1)); assertEquals(1,result.wear().get(0));
    }
    @Test void durabilityCapsAreInTwentyFifthsOfHealth() {
        var result=LegacyArmorAbsorption.apply(20,List.of(piece(1,0,.35,1)));
        assertEquals(19.96f,result.remaining(),.00001f); assertEquals(1,result.wear().get(1));
    }
    @Test void overfullPriorityStopsLowerPriorityArmorWear() {
        var result=LegacyArmorAbsorption.apply(10,List.of(piece(0,0,.12,500),piece(1,1,.45,600),piece(2,1,.55,640),piece(3,1,.2,440)));
        assertEquals(0,result.remaining(),.00001f); assertFalse(result.wear().containsKey(0));
    }
    @Test void cappedPieceDoesNotKeepItsUnusableShare() {
        var result=LegacyArmorAbsorption.apply(10,List.of(piece(1,1,.6,25),piece(2,1,.6,600)));
        assertEquals(3,result.remaining(),.00001f); assertEquals(1,result.wear().get(1)); assertEquals(6,result.wear().get(2));
    }
}
