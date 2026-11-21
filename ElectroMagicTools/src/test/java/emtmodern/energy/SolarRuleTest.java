package emtmodern.energy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SolarRuleTest {
    @Test void originalEnvironmentalMultipliers(){
        assertEquals(25,SolarRule.production("air",10,true,true,false,true,false,false,160));
        assertEquals(10,SolarRule.production("air",10,true,true,false,true,false,false,159));
        assertEquals(25,SolarRule.production("earth",10,true,true,false,true,false,false,10));
        assertEquals(30,SolarRule.production("order",10,true,true,false,true,false,false,64));
    }
    @Test void weatherAndDimensions(){
        assertEquals(0,SolarRule.production("compressed",10,true,true,true,true,false,false,64));
        assertEquals(10,SolarRule.production("compressed",10,true,true,true,false,false,false,64));
        assertEquals(10,SolarRule.production("water",10,true,false,true,true,false,false,64));
        assertEquals(0,SolarRule.production("dark",10,true,false,true,false,false,false,64));
        assertEquals(10,SolarRule.production("dark",10,true,false,false,true,false,false,64));
        assertEquals(0,SolarRule.production("order",10,true,true,false,false,true,true,64));
        assertEquals(20,SolarRule.production("fire",10,false,false,false,false,true,true,64));
        assertEquals(0,SolarRule.production("fire",10,true,true,false,true,false,false,64));
    }
}
