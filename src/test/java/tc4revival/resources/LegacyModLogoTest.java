package tc4revival.resources;

import com.google.gson.JsonParser;
import com.electronwill.nightconfig.core.Config;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.minecraftforge.fml.loading.moddiscovery.NightConfigWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LegacyModLogoTest {
    @TempDir Path game;
    private static final byte[] PNG = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aTRsAAAAASUVORK5CYII=");

    private static byte[] archive(String path, String declaration, boolean listWrapper) throws Exception {
        try (var out = new ByteArrayOutputStream(); var zip = new ZipOutputStream(out)) {
            String rows = """
                    [{"modid":"Other","mcversion":"1.7.10","logoFile":"different.png"},
                     {"modid":"Example","mcversion":"1.7.10","logoFile":"%s"}]
                    """.formatted(declaration);
            String metadata = listWrapper ? "{\"modList\":" + rows + "}" : rows;
            for (var entry : Map.of("mcmod.info",metadata.getBytes(StandardCharsets.UTF_8),path,PNG,"assets/example/data.txt",new byte[]{42}).entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey())); zip.write(entry.getValue()); zip.closeEntry();
            }
            zip.finish();
            return out.toByteArray();
        }
    }

    @Test void readsOnlyTheMatchingModAndPreservesRootAndLegacyAbsolutePaths() throws Exception {
        for (String path : List.of("ExampleLogo.PNG", "assets/old/Logo.png")) {
            for (boolean wrapped : List.of(false,true)) {
                var bytes = archive(path,wrapped ? "/" + path : path,wrapped);
                assertArrayEquals(PNG,LegacyModLogo.read(bytes,"Example").orElseThrow());
                assertTrue(LegacyModLogo.read(bytes,"Other").isEmpty());
            }
        }
    }

    @Test void addonsWithResearchIconsKeepTheirPackagedLogos() {
        var config = Config.inMemory();
        var declaration = new NightConfigWrapper(config);
        assertTrue(LegacyModLogo.usesPrimarySource("thaumicfutures", declaration));
        config.set("logoFile", "research.png");
        assertFalse(LegacyModLogo.usesPrimarySource("thaumicomniverse", declaration));
        assertFalse(LegacyModLogo.usesPrimarySource("thaumicfutures", declaration));
        assertTrue(LegacyModLogo.usesPrimarySource("automagy", declaration));
    }

    @Test void missingOrUnsafeLogoIsOptional() throws Exception {
        for (String path : List.of("", "missing.png", "../foreign.png", "//host/file.png", "file:C:/outside.png")) {
            assertTrue(LegacyModLogo.read(archive("foreign.png",path,false),"Example").isEmpty(),path);
        }
    }

    @Test void existingAssetsWorkOfflineAndLaterRecoverTheLogoOnce() throws Exception {
        var bytes = archive("logo.png","/logo.png",true);
        var target=new OriginalResourceDownloads.Target("addon","Example","Example",OriginalResources.sha256(bytes),
                "https://mediafilez.forgecdn.net/files/2227/552/example.jar",Map.of("assets/addon/data.txt","assets/example/data.txt"),false,false);
        var root=game.resolve("thaumcraft");Files.createDirectories(root);
        Files.write(root.resolve("example.jar"),bytes);
        var sources=Set.of("example_1_7_10");
        assertTrue(OriginalResourceDownloads.obtainMissing(game,List.of(target),sources,ignored->{},(u,p)->fail()).isEmpty());
        var cache=root.resolve("example_1_7_10");
        assertArrayEquals(PNG,target.read(cache).resources().get(LegacyModLogo.CACHE_FILE));
        // Reproduce a pre-logo manifest after its source JAR was removed by the original loader.
        var file=cache.resolve(ProcessedOriginalResources.MANIFEST);
        var old=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        old.remove("logo_checked");old.getAsJsonObject("files").remove(LegacyModLogo.CACHE_FILE);
        Files.writeString(file,old.toString());Files.delete(cache.resolve(LegacyModLogo.CACHE_FILE));
        var offline=OriginalResourceDownloads.obtainMissing(game,List.of(target),sources,ignored->{},(u,p)->{throw new java.io.IOException("offline");});
        assertTrue(offline.get(0).failure().isEmpty());assertFalse(offline.get(0).warning().isEmpty());
        assertArrayEquals(new byte[]{42},target.read(cache).resources().get("assets/example/data.txt"));
        var recovered=OriginalResourceDownloads.obtainMissing(game,List.of(target),sources,ignored->{},(u,p)->Files.write(p,bytes));
        assertTrue(recovered.get(0).failure().isEmpty());
        assertArrayEquals(PNG,target.read(cache).resources().get(LegacyModLogo.CACHE_FILE));
        assertTrue(OriginalResourceDownloads.obtainMissing(game,List.of(target),sources,ignored->{},(u,p)->fail()).isEmpty());
    }
}
