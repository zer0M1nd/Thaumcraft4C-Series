package tbmodern.resources;
import com.google.gson.JsonParser;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/** Bases declares inputs; TC4 owns downloading, validation, extraction and pack preparation. */
class LegacyAssetsTest {
    @Test void commonDeclarationIncludesOriginalAssetsAndTranslationAliases() throws Exception {
        var path=Path.of("src/main/resources/data/thaumicbases/thaumcraft/original_resources.json");
        var requirement=tc4revival.resources.AddonResourceRequirements.decode("thaumicbases",JsonParser.parseString(Files.readString(path)).getAsJsonObject()).get(0);
        assertEquals("thaumicbases",requirement.legacyModId());assertEquals("1.7.10",requirement.minecraftVersion());
        assertTrue(requirement.mapping().size()>100);
        assertTrue(requirement.languages().get("assets/thaumicbases/lang/en_us.json").containsKey("itemGroup.thaumicbases"));
        assertTrue(requirement.languages().containsKey("assets/thaumicbases/lang/zh_cn.json"));
    }
}
