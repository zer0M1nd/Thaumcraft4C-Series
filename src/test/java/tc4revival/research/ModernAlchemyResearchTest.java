package tc4revival.research;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ModernAlchemyResearchTest {
    @Test void baseResourcesDoNotRequestTc6OrRegisterOmniverseResearch() throws Exception {
        assertFalse(Files.exists(Path.of("src/main/resources/data/thaumcraft/thaumcraft/original_resources.json")));
        assertFalse(Files.exists(Path.of("src/main/resources/data/thaumcraft/research_catalog/modern_alchemy.json")));
        assertFalse(Files.readString(Path.of("src/main/java/tc4revival/content/TCItems.java")).contains("alchemical_brass_ingot"));
    }
}
