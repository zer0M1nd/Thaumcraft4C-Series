package automagy;
import automagy.logic.MobLureRules;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MobLureRulesTest {
 @Test void charmUsesACylinderNotTheEntitySearchSquare(){assertTrue(MobLureRules.inRange(8.5,0,10,10,14,8.5));assertFalse(MobLureRules.inRange(8,8,12,10,14,8.5));assertTrue(MobLureRules.inRange(0,32.5,14,10,14,32.5));assertFalse(MobLureRules.inRange(0,0,14.01,10,14,32.5));}
 @Test void nearbyRevengeExpiresAtOneHundredTicks(){assertTrue(MobLureRules.protectsTarget(true,199,100,99.9));assertFalse(MobLureRules.protectsTarget(true,200,100,99.9));assertFalse(MobLureRules.protectsTarget(true,199,100,100));assertFalse(MobLureRules.protectsTarget(false,199,100,0));}
}
