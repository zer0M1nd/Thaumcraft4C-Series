package emtmodern.item;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class ToolProfileParityTest {
    @Test void authorBoreDurabilityRoundsOnlyEligiblePickaxes(){
        var expected=java.util.Map.of("thaumium_drill",240,"iron_omnitool",200,"diamond_omnitool",350,"thaumium_omnitool",467,"diamond_chainsaw",27,"thaumium_chainsaw",27);
        for(var profile:ToolProfile.ALL)assertEquals(expected.get(profile.id()),profile.boreDurability(),profile.id());
        assertEquals(2571,new ToolProfile("rockbreaker_drill","",900000,3,900,350,400,12,25,true,false,true,false).boreDurability());
    }
}
