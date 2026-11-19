package tc4revival.resources;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResourceLoadingStateTest {
    @BeforeEach @AfterEach void reset() { ResourceLoadingState.clear(); }

    @Test void hashWarningsNeverRequireAConfirmationScreen() {
        for(String owner:new String[]{"thaumcraft", "ThaumicTinkerer", "Thaumcraft 6"})
            ResourceLoadingState.warning(owner+": SHA-256 differs from reference");
        assertFalse(ResourceLoadingState.missing());
        assertFalse(ResourceLoadingState.needsScreen());
        assertEquals("",ResourceLoadingState.details());
        ResourceLoadingState.clear();
        ResourceLoadingState.warning("Repeated after resource reload");
        assertFalse(ResourceLoadingState.needsScreen());
    }

    @Test void missingResourcesStillHaveTheirOwnRetryScreen() {
        ResourceLoadingState.warning("A usable variant");
        ResourceLoadingState.failure("Missing required resource");
        assertTrue(ResourceLoadingState.missing());
        assertTrue(ResourceLoadingState.needsScreen());
        assertEquals("Missing required resource",ResourceLoadingState.details());
        ResourceLoadingState.clear();
        assertFalse(ResourceLoadingState.needsScreen());
    }
}
