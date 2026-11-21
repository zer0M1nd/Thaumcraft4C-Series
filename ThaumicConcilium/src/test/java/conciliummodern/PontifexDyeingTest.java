package conciliummodern;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PontifexDyeingTest {
    @Test void originalRedMixedWithWhiteUsesIntegerBrightness(){assertEquals(0xcf8b8b,PontifexDyeing.blend(List.of(0xa01818,0xffffff)));}
    @Test void originalRedMixedWithLegacyBlackKeepsBaseContribution(){assertEquals(0x5c1818,PontifexDyeing.blend(List.of(0xa01818,0x191919)));}
    @Test void blackAndOrderingAreSafe(){assertEquals(0,PontifexDyeing.blend(List.of(0,0)));assertEquals(PontifexDyeing.blend(List.of(0xaa0044,0x80ff08,0x78)),PontifexDyeing.blend(List.of(0x78,0xaa0044,0x80ff08)));assertThrows(IllegalArgumentException.class,()->PontifexDyeing.blend(List.of()));}
}
