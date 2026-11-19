package tc4revival.resources;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import thaumcraft.api.resources.ExternalResourceConverters;
import static org.junit.jupiter.api.Assertions.*;

class ExternalResourceConvertersTest {
    @Test void firstExtractionTransformsBeforeForgeGameplayBusStarts() {
        var calls=new AtomicInteger();
        ExternalResourceConverters.register(ResourceLocation.parse("converter_test:first_load"), resources->{calls.incrementAndGet();resources.put("converted.png",new byte[]{1,2,3});});
        var resources=new HashMap<String,byte[]>();
        ExternalResourceConverters.prepare("unrelated_owner",resources);
        assertTrue(resources.isEmpty());assertEquals(0,calls.get());
        ExternalResourceConverters.prepare("converter_test",resources);
        assertArrayEquals(new byte[]{1,2,3},resources.get("converted.png"));assertEquals(1,calls.get());
        ExternalResourceConverters.prepare("converter_test",new HashMap<>());
        assertEquals(2,calls.get());
        assertThrows(IllegalArgumentException.class,()->ExternalResourceConverters.register(ResourceLocation.parse("converter_test:first_load"),ignored->{}));
    }
}
