package tc4revival.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClientTextSearchTest {
    @Test void worksWithoutOptionalJec() {
        assertTrue(ClientTextSearch.contains("Earth", "EAR"));
        assertTrue(ClientTextSearch.contains("大地", "地"));
        assertFalse(ClientTextSearch.contains("大地", "di"));
        assertFalse(ClientTextSearch.contains("Research", "unknown"));
    }
}
