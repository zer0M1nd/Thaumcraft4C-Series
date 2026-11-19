package wgmodern;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class PrimordialRulesTest {
 @Test public void qualityScalesBeforeSquareRoot(){assertEquals(10,PrimordialRules.nodeCharge(100,-1));assertEquals(10,PrimordialRules.nodeCharge(100,0));assertEquals(8,PrimordialRules.nodeCharge(100,1));assertEquals(7,PrimordialRules.nodeCharge(100,2));assertEquals(0,PrimordialRules.nodeCharge(0,0));assertEquals(1,PrimordialRules.nodeCharge(3,2));}
 @Test public void originalWheelMapsFiveFingerSectors(){assertEquals(4,PrimordialRules.radialSlot(100,0));assertEquals(0,PrimordialRules.radialSlot(-100,-100));assertEquals(3,PrimordialRules.radialSlot(0,100));assertEquals(2,PrimordialRules.radialSlot(-100,0));assertEquals(1,PrimordialRules.radialSlot(0,-100));assertTrue(PrimordialRules.radialSlot(0,0)>=0);}
 @Test public void orderPiecesReduceDurationAndStrengthWithoutCreatingNegativeDuration(){assertEquals(200,PrimordialRules.reducedDuration(1000,4));assertEquals(500,PrimordialRules.reducedDuration(1000,1));assertEquals(-1,PrimordialRules.reducedDuration(-1,4));assertEquals(0,PrimordialRules.reducedAmplifier(2,4));assertEquals(2,PrimordialRules.reducedAmplifier(4,2));}
}
