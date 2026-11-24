package tc4revival.resources;

import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.net.URI;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LegacyOfficialTranslationsTest {
    @TempDir Path game;
    static final String URL="https://raw.githubusercontent.com/GTNewHorizons/Example/"+"a".repeat(40)+"/src/main/resources/assets/legacy/lang/zh_CN.lang";
    @Test void cachedSourcesUseOnlyExplicitKeysWithPinnedHashes() throws Exception {
        byte[] bytes="old=已有中文\nother=未声明的键\n".getBytes(StandardCharsets.UTF_8);
        String hash=OriginalResources.sha256(bytes);
        var source=new LegacyOfficialTranslations.Source("example",URI.create(URL),hash,Map.of("item.example.modern","old"),Map.of());
        Path file=game.resolve("resources/thaumcraft-localization/official/"+hash+".lang");
        Files.createDirectories(file.getParent());Files.write(file,bytes);
        var result=LegacyOfficialTranslations.load(game,List.of(source),false);
        assertEquals(Map.of("example",Map.of("item.example.modern","已有中文")),result);
        var prepared=LegacyLocalization.prepare(game,result);
        assertEquals(Set.of("pack.mcmeta","assets/example/lang/zh_cn.json"),prepared.allowed());
        Files.writeString(file,"old=替换过的文件");
        assertTrue(LegacyOfficialTranslations.load(game,List.of(source),false).isEmpty());
        assertEquals("old=替换过的文件",Files.readString(file),"Offline cache inspection must not rewrite files or fetch the network");
    }
    @Test void supplementsRequirePinnedChineseSourceAndHash() throws Exception {
        String declaration="{\"official_supplements\":[{\"url\":\"%s\",\"sha256\":\"%s\",\"keys\":{\"modern\":\"old\"}}]}";
        var valid=JsonParser.parseString(declaration.formatted(URL,"b".repeat(64))).getAsJsonObject();
        assertEquals("example",LegacyOfficialTranslations.decode("example",valid).get(0).owner());
        for(String url:List.of(URL.replace("a".repeat(40),"master"),URL.replace("zh_CN","en_US"),URL.replace("raw.githubusercontent.com","example.org"),URL+"?other=1"))
            assertThrows(java.io.IOException.class,()->LegacyOfficialTranslations.decode("example",JsonParser.parseString(declaration.formatted(url,"b".repeat(64))).getAsJsonObject()));
        assertThrows(java.io.IOException.class,()->LegacyOfficialTranslations.decode("../other",valid));
        assertThrows(java.io.IOException.class,()->LegacyOfficialTranslations.decode("example",JsonParser.parseString(declaration.formatted(URL,"bad")).getAsJsonObject()));
    }

    @Test void alternateTranslationIsBoundToItsSourceAndSurvivesReload() throws Exception {
        byte[] primary = "old=首选\n".getBytes(StandardCharsets.UTF_8);
        byte[] alternate = "old=备用\n".getBytes(StandardCharsets.UTF_8);
        String second = URL.replace("a".repeat(40), "c".repeat(40));
        String declaration = """
                {"official_supplements":[{"url":"%s","sha256":"%s","keys":{"modern":"old"},
                  "downloads":[{"url":"%s","sha256":"%s"}]}]}
                """.formatted(URL, OriginalResources.sha256(primary), second, OriginalResources.sha256(alternate));
        var source = LegacyOfficialTranslations.decode("example", JsonParser.parseString(declaration).getAsJsonObject()).get(0);
        var task = TranslationDownload.official(game, source);
        assertThrows(java.io.IOException.class,
                () -> task.prepareDownload(URI.create(URL), temp -> Files.write(temp, alternate)));
        try (var accepted = task.prepareDownload(URI.create(second), temp -> Files.write(temp, alternate))) {
            accepted.apply();
        }
        try (var cached = task.local()) { assertNotNull(cached); }
        assertEquals("备用", LegacyOfficialTranslations.load(game, List.of(source), false).get("example").get("modern"));
    }
}
