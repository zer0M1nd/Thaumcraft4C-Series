package tc4revival.aspect;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AspectTextureTest {
    @Test void legacyNamesAddonNamespacesAndExplicitIconsResolve() {
        assertEquals("thaumcraft:textures/aspects/aer.png", aspect("aer", null).texture().toString());
        assertEquals("forbidden_magic:textures/aspects/gula.png", aspect("forbidden_magic:gula", null).texture().toString());
        assertEquals("example:textures/custom/hunger.png", aspect("forbidden_magic:gula", "example:textures/custom/hunger.png").texture().toString());
    }
    private Aspect aspect(String tag, String icon) { return new Aspect(tag, 0xffffff, List.of(), "f", 1, icon); }
}
