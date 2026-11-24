package dev.tc4.download;

import org.junit.jupiter.api.Test;
import java.net.URI;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DownloadPreferencesTest {
    @Test void englishAndChineseSettingsSurviveEarlyConfigReading(@org.junit.jupiter.api.io.TempDir java.nio.file.Path game) throws Exception {
        var config = java.nio.file.Files.createDirectories(game.resolve("config")).resolve("thaumcraft-client.toml");
        for (String name : List.of("启用", "enabled", "禁用", "DISABLED")) {
            java.nio.file.Files.writeString(config, "[downloads]\nchinese_localization = \"" + name + "\"\n");
            assertEquals(name.equals("启用") || name.equals("enabled"), DownloadPreferences.chinese(game));
        }
        assertEquals(DownloadPreferences.parse("DETECT"), DownloadPreferences.parse("检测"));
        assertTrue(DownloadPreferences.parse("offline").isEmpty());
    }
    @Test void overridesDoNotProbeAndDetectionRequiresMainlandChina() {
        assertTrue(DownloadPreferences.enabled(DownloadPreferences.Mode.ENABLED, () -> fail()));
        assertFalse(DownloadPreferences.enabled(DownloadPreferences.Mode.DISABLED, () -> fail()));
        for (var code : List.of("CN","HK","MO","TW","SG","US",""))
            assertEquals(code.equals("CN"), DownloadPreferences.enabled(DownloadPreferences.Mode.DETECT, () -> code));
    }

    @Test void browserPagesKeepTheirTypesAndRegionalOrder() {
        var pages = List.of(URI.create("https://github.com/owner/repo/releases/tag/one"), URI.create("https://modrinth.com/mod/sample/version/one"),
                URI.create("https://www.mcmod.cn/download/233.html"), URI.create("https://www.curseforge.com/minecraft/mc-mods/thaumcraft/files/2227552"));
        assertEquals(List.of("MC 百科","CurseForge","Modrinth","GitHub Release"), ManualLink.ordered(pages,true).stream().map(ManualLink::type).toList());
        assertEquals(List.of("CurseForge","Modrinth","GitHub Release","MC 百科"), ManualLink.ordered(pages,false).stream().map(ManualLink::type).toList());
    }
}
