package gadomancymodern.claw;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class ClawInstabilityTest {
    @Test void cheapWandRetainsHighRisk(){assertEquals(29,ClawInstability.calculate(25,false,1.1f,false,false));assertEquals(20,ClawInstability.calculate(100,false,.8f,false,false));}
    @Test void capacityStaffVoidAndPrimalBonusesStack(){assertEquals(-2,ClawInstability.calculate(250,true,.6f,true,true));assertEquals(-3,ClawInstability.calculate(300,true,.8f,true,true));assertEquals(6,ClawInstability.calculate(900,true,.8f,false,false));}
    @Test void extraCapacityOnlyDiscountsUntilThreeHundred(){assertEquals(ClawInstability.calculate(300,false,.8f,false,false),ClawInstability.calculate(1000,false,.8f,false,false));}
}
