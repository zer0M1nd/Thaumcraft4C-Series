package tc4revival.resources;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AdditionalOriginalSourcesTest {
    @TempDir Path game;
    private JsonObject source(String id,String texture,byte marker) throws Exception {
        Path jar=game.resolve("thaumcraft").resolve(id+".jar");Files.createDirectories(jar.getParent());
        String original="assets/"+id+"/textures/"+texture+".png";
        try(var zip=new ZipOutputStream(Files.newOutputStream(jar))) {
            zip.putNextEntry(new ZipEntry("mcmod.info"));zip.write(("[{\"modid\":\""+id+"\",\"mcversion\":\"1.7.10\"}]").getBytes(StandardCharsets.UTF_8));zip.closeEntry();
            zip.putNextEntry(new ZipEntry(original));zip.write(new byte[]{marker});zip.closeEntry();
        }
        var json=new JsonObject();json.addProperty("name",id);json.addProperty("legacy_modid",id);
        json.addProperty("sha256",OriginalResources.sha256(Files.readAllBytes(jar)));
        json.addProperty("download","https://www.curseforge.com/minecraft/mc-mods/"+id+"/files/12345");
        var mapping=new JsonObject();mapping.addProperty("assets/example/textures/"+texture+".png",original);json.add("mapping",mapping);
        return json;
    }
    @Test void twoArchivesPrepareDistinctPacksAndTargetsUnderOneOwner() throws Exception {
        var primary=source("first","first",(byte)1);var extra=source("second","second",(byte)2);
        var sources=new JsonArray();sources.add(extra);primary.add("additional_sources",sources);
        var requirements=AddonResourceRequirements.decode("example",primary);
        assertEquals(2,requirements.size());assertTrue(requirements.stream().allMatch(r->r.owner().equals("example")));
        var scan=AddonResourceRequirements.scan(game,requirements);assertTrue(scan.missing().isEmpty());
        var a=AddonResourceRequirements.prepare(game,scan.resolved().get(0));var b=AddonResourceRequirements.prepare(game,scan.resolved().get(1));
        assertNotEquals(a.packId(),b.packId());assertNotEquals(a.directory(),b.directory());
        assertArrayEquals(new byte[]{1},Files.readAllBytes(a.directory().resolve("assets/example/textures/first.png")));
        assertArrayEquals(new byte[]{2},Files.readAllBytes(b.directory().resolve("assets/example/textures/second.png")));
        Path manifest=game.resolve("original_resources.json");Files.writeString(manifest,primary.toString());
        var targets=OriginalDownloadTargets.addons("example",manifest);
        assertEquals(2,targets.size());assertNotEquals(targets.get(0).owner(),targets.get(1).owner());
        assertEquals("second",targets.get(1).legacyModId());
    }
    @Test void extraArchivesCannotOverwriteOtherOwnersOrAmbiguouslySharePaths() throws Exception {
        var primary=source("first","first",(byte)1);var extra=source("second","second",(byte)2);
        var sources=new JsonArray();sources.add(extra);primary.add("additional_sources",sources);
        extra.getAsJsonObject("mapping").addProperty("assets/another/textures/stolen.png","assets/second/textures/second.png");
        assertThrows(IOException.class,()->AddonResourceRequirements.decode("example",primary));
        extra.getAsJsonObject("mapping").remove("assets/another/textures/stolen.png");
        extra.getAsJsonObject("mapping").addProperty("assets/example/textures/first.png","assets/second/textures/second.png");
        assertThrows(IOException.class,()->AddonResourceRequirements.decode("example",primary));
    }
    @Test void multipleDirectLinksAndDirectoryRequirementsDoNotNeedASingularPage() throws Exception {
        var declaration=source("first","first",(byte)1);declaration.remove("download");
        var urls=new JsonArray();urls.add("https://mediafilez.forgecdn.net/files/1/2/first.jar");urls.add("https://edge.forgecdn.net/files/1/2/first.jar");declaration.add("downloads",urls);
        var wanted=new JsonArray();wanted.add("assets/first/textures/");declaration.add("required_resources",wanted);
        var request=AddonResourceRequirements.decode("example",declaration).get(0);
        assertEquals(2,request.downloads().size());assertEquals(List.of("assets/first/textures/"),request.requiredResources());
        var attempts=CurseForgeDownload.attempts(request.downloads());
        assertEquals("mediafilez.forgecdn.net",attempts.get(0).getHost());assertEquals("edge.forgecdn.net",attempts.get(1).getHost());
        assertEquals(CurseForgeDownload.PROXY.getHost(),attempts.get(2).getHost());
    }
}
