package gadomancymodern.golem;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class RunicChargeTest {
    @Test void wholeShieldPointsRechargeAtFiftyTicksAndClampAtMaterialCapacity(){assertEquals(.98f,RunicCharge.charged(49,20),.0001f);assertEquals(1,RunicCharge.charged(50,20));assertEquals(20,RunicCharge.charged(5000,20));assertEquals(7,RunicCharge.charged(1000,7));}
    @Test void FractionalChargeDoesNotAbsorbUntilWholePoint(){var hit=RunicCharge.hit(.98f,5);assertEquals(5,hit.remainingDamage());assertEquals(.98f,hit.nextCharge());hit=RunicCharge.hit(1.8f,3);assertEquals(2,hit.remainingDamage());assertEquals(.8f,hit.nextCharge(),.0001f);assertEquals(40,RunicCharge.ticks(hit.nextCharge()));}
    @Test void fractionalHitRetainsLegacyWholePointDebitAndFraction(){var hit=RunicCharge.hit(5.8f,1.25f);assertEquals(0,hit.remainingDamage());assertEquals(3.8f,hit.nextCharge(),.0001f);assertEquals(190,RunicCharge.ticks(hit.nextCharge()));}
}
