package tc4revival.content;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RobeDyeTest {
    @Test void mixingRetainsAverageBrightnessRatherThanDarkeningSaturatedDyes() {
        assertEquals(0xff00ff,RobeDyeRecipe.blend(List.of(0xff0000,0x0000ff)));
        assertEquals(0x202020,RobeDyeRecipe.blend(List.of(0x101010,0x303030)));
    }
    @Test void oldRobeBaseParticipatesInTheFirstDye() {
        assertEquals(0x8c3960,RobeDyeRecipe.blend(List.of(6961280,0x993333)));
    }
    @Test void allBlackKeepsAValidBlackResult() {
        assertEquals(0,RobeDyeRecipe.blend(List.of(0,0,0)));
    }
}
