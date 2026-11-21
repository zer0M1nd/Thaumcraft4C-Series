package dev.tc4.updater;

import com.electronwill.nightconfig.toml.TomlParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class UpdaterConfigTest {
    @TempDir Path game;

    private Path config() { return game.resolve("config/thaumcraftupdater.toml"); }
    private void write(String text) throws Exception {
        Files.createDirectories(config().getParent());
        Files.writeString(config(), text);
    }
    private void assertWrittenModes(String major, String stable, String testing) throws Exception {
        var parsed = new TomlParser().parse(Files.readString(config()));
        assertEquals(major, parsed.get("majorUpdates"));
        assertEquals(stable, parsed.get("stableUpdates"));
        assertEquals(testing, parsed.get("testingUpdates"));
    }

    @Test void freshConfigUsesEnglishAndKeepsDefaultPolicy() throws Exception {
        var expected = new UpdaterConfig(UpdateMode.ASK, UpdateMode.YES, UpdateMode.NO, List.of());
        assertEquals(expected, UpdaterConfig.read(game));
        assertWrittenModes("ask", "yes", "no");
        String first = Files.readString(config());
        assertEquals(expected, UpdaterConfig.read(game));
        assertEquals(first, Files.readString(config()));
    }

    @ParameterizedTest @ValueSource(strings={"majorUpdates", "stableUpdates", "testingUpdates", "installOnce"})
    void fillingMissingOptionWritesEnglishAndPreservesOtherChoices(String missing) throws Exception {
        String text = "majorUpdates = '是'\nstableUpdates = '否'\ntestingUpdates = '询问'\ninstallOnce = []\n";
        write(text.replaceAll("(?m)^" + missing + " = .*\\n", ""));
        var expected = new UpdaterConfig(missing.equals("majorUpdates") ? UpdateMode.ASK : UpdateMode.YES,
                missing.equals("stableUpdates") ? UpdateMode.YES : UpdateMode.NO,
                missing.equals("testingUpdates") ? UpdateMode.NO : UpdateMode.ASK, List.of());
        assertEquals(expected, UpdaterConfig.read(game));
        assertWrittenModes(missing.equals("majorUpdates") ? "ask" : "yes",
                missing.equals("stableUpdates") ? "yes" : "no", missing.equals("testingUpdates") ? "no" : "ask");
        assertEquals(expected, UpdaterConfig.read(game));
    }

    @ParameterizedTest @CsvSource({"false,false,no,no", "false,true,yes,no", "true,false,no,yes", "true,true,yes,yes"})
    void legacyBooleansMigrateToEnglishWithoutChangingPolicy(boolean testing, boolean major,
                                                           String majorText, String testingText) throws Exception {
        write("includeUnstable = " + testing + "\nallowMajorUpdates = " + major + "\n");
        assertEquals(new UpdaterConfig(testing, major), UpdaterConfig.read(game));
        assertWrittenModes(majorText, "yes", testingText);
        assertFalse(Files.readString(config()).contains("includeUnstable"));
        assertFalse(Files.readString(config()).contains("allowMajorUpdates"));
    }

    @ParameterizedTest @CsvSource({"是,否,询问,YES,NO,ASK", "否,询问,是,NO,ASK,YES", "询问,是,否,ASK,YES,NO",
            "yes,no,ask,YES,NO,ASK", "ASK,Yes,nO,ASK,YES,NO"})
    void completeChineseAndEnglishConfigsRemainReadableAndUnchanged(String major, String stable, String testing,
                                                                   UpdateMode majorMode, UpdateMode stableMode, UpdateMode testingMode) throws Exception {
        String original = "# user choices\nmajorUpdates = '" + major + "'\nstableUpdates = '" + stable
                + "'\ntestingUpdates = '" + testing + "'\ninstallOnce = []\n";
        write(original);
        assertEquals(new UpdaterConfig(majorMode, stableMode, testingMode, List.of()), UpdaterConfig.read(game));
        assertEquals(original, Files.readString(config()));
    }

    @Test void oneShotConsumptionPreservesChineseChoices() throws Exception {
        write("majorUpdates = '询问'\nstableUpdates = '是'\ntestingUpdates = '否'\ninstallOnce = ['thaumcraft']\n");
        assertEquals(new UpdaterConfig(UpdateMode.ASK, UpdateMode.YES, UpdateMode.NO, List.of("thaumcraft")), UpdaterConfig.read(game));
        assertWrittenModes("询问", "是", "否");
        assertEquals(List.of(), UpdaterConfig.read(game).installOnce());
    }
}
