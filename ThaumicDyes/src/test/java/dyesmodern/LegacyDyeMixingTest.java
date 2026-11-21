package dyesmodern;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LegacyDyeMixingTest {
    @Test void originalRedIncludesWhiteUndyedArmorInAverage(){assertEquals(0xCC9999,LegacyDyeMixing.mix(0xFFFFFF,List.of(14)));}
    @Test void repeatedDyeingUsesExistingColor(){assertEquals(0xB26666,LegacyDyeMixing.mix(0xCC9999,List.of(14)));}
    @Test void oldBlackIsTwentyFiveRatherThanModernPalette(){assertEquals(0x191919,LegacyDyeMixing.mix(0x191919,List.of(15)));}
    @Test void dyesAreOrderIndependentAndDuplicatesMatter(){assertEquals(LegacyDyeMixing.mix(0xFFFFFF,List.of(14,11)),LegacyDyeMixing.mix(0xFFFFFF,List.of(11,14)));assertNotEquals(LegacyDyeMixing.mix(0xFFFFFF,List.of(14)),LegacyDyeMixing.mix(0xFFFFFF,List.of(14,14)));}
    @Test void invalidCraftsDoNotProduceColors(){assertThrows(IllegalArgumentException.class,()->LegacyDyeMixing.mix(0,List.of()));assertThrows(IllegalArgumentException.class,()->LegacyDyeMixing.mix(0,List.of(-1)));assertThrows(IllegalArgumentException.class,()->LegacyDyeMixing.mix(0,List.of(16)));}
}
