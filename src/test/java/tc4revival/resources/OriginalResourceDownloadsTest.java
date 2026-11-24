package tc4revival.resources;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class OriginalResourceDownloadsTest {
    @TempDir Path game;
    static final String A="assets/example/a.png",B="assets/example/b.png";
    private byte[] jar(String version) throws IOException {
        return jar(version,"Example");
    }
    private byte[] jar(String version,String modId) throws IOException {
        var bytes=new ByteArrayOutputStream();
        try(var zip=new ZipOutputStream(bytes)) {
            for(var e:Map.of("mcmod.info","[{\"modid\":\""+modId+"\",\"mcversion\":\""+version+"\"}]",A,"first",B,"second",A+".mcmeta","{}","example/Main.class","never retained","assets/unused/file.txt","unused").entrySet()) {
                var entry=new ZipEntry(e.getKey());entry.setTime(0);zip.putNextEntry(entry);zip.write(e.getValue().getBytes());zip.closeEntry();
            }
        }return bytes.toByteArray();
    }
    private OriginalResourceDownloads.Target target(byte[] data,String owner,String version,String resource) {
        return new OriginalResourceDownloads.Target(owner,"Example","Example",OriginalResources.sha256(data),
            "https://www.curseforge.com/minecraft/mc-mods/thaumcraft/files/2227552",Map.of("assets/"+owner+"/image.png",resource),false,false,version);
    }
    @Test void qualifiedLegacyIdentityExtractsAndValidatesItsOwnCache() throws Exception {
        String id="flammpfeil.slashblade";
        byte[] data=jar("1.7.10",id);
        var target=new OriginalResourceDownloads.Target("heartwork","SlashBlade",id,OriginalResources.sha256(data),
            "https://mediafilez.forgecdn.net/files/2705/970/SlashBlade-mc1.7.10-r88.jar",Map.of("assets/heartwork/a.png",A),false,false,"1.7.10");
        Path root=game.resolve("thaumcraft");Files.createDirectories(root);Files.write(root.resolve("r88.jar"),data);
        assertTrue(ArchiveDownloadFixture.obtainMissing(game,List.of(target),t->{},(u,p)->fail()).isEmpty());
        Path cache=root.resolve("flammpfeil.slashblade_1_7_10");
        assertEquals("first",new String(target.read(cache).resources().get(A)));
        assertFalse(Files.exists(root.resolve("r88.jar")));
        assertTrue(ArchiveDownloadFixture.obtainMissing(game,List.of(target),t->{},(u,p)->fail()).isEmpty());
        Files.writeString(cache.resolve(A),"tampered");assertThrows(IOException.class,()->target.read(cache));
        assertNotEquals(ProcessedOriginalResources.directoryName(id,"1.7.10"),ProcessedOriginalResources.directoryName("flammpfeil_slashblade","1.7.10"));
        for(String invalid:List.of("..","../x","x/../y","x\\y","x..y",".x","x.","C:x"))
            assertThrows(IllegalArgumentException.class,()->ProcessedOriginalResources.directoryName(invalid,"1.7.10"));
    }
    @Test void localJarIsExtractedOnceOnlyRequiredDataIsKeptAndJarIsDeleted() throws Exception {
        var data=jar("1.7.10");var target=target(data,"example","1.7.10",A);
        var folder=game.resolve("thaumcraft");Files.createDirectories(folder);Path original=folder.resolve("user choice.JAR");Files.write(original,data);
        assertTrue(ArchiveDownloadFixture.obtainMissing(game,List.of(target),text->{},(u,p)->fail("No download needed")).isEmpty());
        assertFalse(Files.exists(original));var dir=folder.resolve("example_1_7_10");
        assertEquals("first",Files.readString(dir.resolve(A)));assertTrue(Files.isRegularFile(dir.resolve(A+".mcmeta")));
        assertFalse(Files.exists(dir.resolve(B)));assertFalse(Files.exists(dir.resolve("example/Main.class")));assertFalse(Files.exists(dir.resolve("mcmod.info")));
        assertTrue(ArchiveDownloadFixture.obtainMissing(game,List.of(target),text->{},(u,p)->fail("Processed resource reused")).isEmpty());
    }
    @Test void allAddonRequestsAreUnionedBeforeRemovingSharedArchive() throws Exception {
        var data=jar("1.7.10");var first=target(data,"one","1.7.10",A);var second=target(data,"two","1.7.10",B);var calls=new AtomicInteger();
        var result=ArchiveDownloadFixture.obtainMissing(game,List.of(first,second),t->{},(u,p)->{calls.incrementAndGet();Files.write(p,data);});
        assertEquals(1,calls.get());assertEquals("",result.get(0).failure());var directory=result.get(0).downloaded();
        assertNotNull(first.read(directory));assertNotNull(second.read(directory));assertEquals(1,OriginalResources.candidates(directory.getParent()).size());
    }
    @Test void missingOrCorruptedResourceAndMetadataCauseFreshDownloadAndExtraction() throws Exception {
        var data=jar("1.7.10");var target=target(data,"example","1.7.10",A);var calls=new AtomicInteger();
        ArchiveDownloadFixture.Transfer transfer=(u,p)->{calls.incrementAndGet();Files.write(p,data);};
        var directory=ArchiveDownloadFixture.obtainMissing(game,List.of(target),t->{},transfer).get(0).downloaded();
        for(String path:List.of(A,A+".mcmeta",ProcessedOriginalResources.MANIFEST)) {
            Files.delete(directory.resolve(path));var result=ArchiveDownloadFixture.obtainMissing(game,List.of(target),t->{},transfer);
            assertEquals("",result.get(0).failure());assertNotNull(target.read(directory));
        }
        Files.writeString(directory.resolve(A),"damage");assertThrows(IOException.class,()->target.read(directory));
        assertEquals("",ArchiveDownloadFixture.obtainMissing(game,List.of(target),t->{},transfer).get(0).failure());assertEquals(5,calls.get());
    }
    @Test void cdnFailureFallsBackToProxyAndInvalidDownloadsNeverBecomeResources() throws Exception {
        var data=jar("1.7.10");var target=target(data,"example","1.7.10",A);var urls=new ArrayList<java.net.URI>();
        var result=ArchiveDownloadFixture.obtainMissing(game,List.of(target),t->{},(u,p)->{
            urls.add(u);if(u.getHost().endsWith("forgecdn.net"))throw new IOException("Direct connection failed");Files.write(p,data);
        }).get(0);
        assertEquals("",result.failure());assertEquals(2,urls.size());assertEquals(CurseForgeDownload.PROXY.getHost(),urls.get(1).getHost());
        Files.delete(result.downloaded().resolve(A));
        var failed=ArchiveDownloadFixture.obtainMissing(game,List.of(target),t->{},(u,p)->Files.writeString(p,"<html>challenge</html>")).get(0);
        assertNull(failed.downloaded());assertFalse(failed.failure().isBlank());
        try(var files=Files.list(game.resolve("thaumcraft"))){assertTrue(files.noneMatch(p->p.toString().endsWith(".jar")||p.toString().endsWith(".part")));}
    }
    @Test void hashMismatchFailsEveryMirrorWithoutInstallingOrCaching() throws Exception {
        var data=jar("1.7.10");var target=target(jar("1.12.2"),"example","1.7.10",A);
        var result=ArchiveDownloadFixture.obtainMissing(game,List.of(target),t->{},(u,p)->Files.write(p,data)).get(0);
        assertTrue(result.failure().contains("SHA-256"));assertNull(result.downloaded());
        try (var files=Files.list(game.resolve("thaumcraft"))) { assertEquals(0,files.count()); }
    }
    @Test void automaticSourcesArePinnedIndividuallyAndBrowserOnlyVariantsCanBeDropped() throws Exception {
        byte[] a=jar("1.7.10");
        var bytes=new ByteArrayOutputStream(); bytes.write(a); bytes.write(42); byte[] b=bytes.toByteArray();
        String first="https://mediafilez.forgecdn.net/files/1000/100/a.jar", second="https://mediafilez.forgecdn.net/files/1000/200/b.jar";
        String browser="https://www.mcmod.cn/download/123.html";
        var data=com.google.gson.JsonParser.parseString("""
            {"name":"Example","legacy_modid":"Example","sha256":"%s","downloads":["%s",{"url":"%s","sha256":"%s"}],
             "manual_downloads":[{"url":"%s","sha256":"%s"}],"mapping":{"assets/example/image.png":"%s"}}
            """.formatted(OriginalResources.sha256(a),first,second,OriginalResources.sha256(b),browser,OriginalResources.sha256(b),A)).getAsJsonObject();
        Path declaration=Files.writeString(game.resolve("declaration.json"),data.toString());
        var target=OriginalDownloadTargets.addons("example",declaration).get(0);
        var task=new OriginalArchiveTask(game,target);
        assertFalse(task.item().links().contains(java.net.URI.create(browser)));
        assertEquals(List.of(java.net.URI.create(browser)),task.item().manualLinks().stream().map(tc4revival.download.ManualLink::url).toList());
        assertThrows(IOException.class,()->task.prepareDownload(java.net.URI.create(first),p->Files.write(p,b)));
        try(var prepared=task.prepareDownload(java.net.URI.create(second),p->Files.write(p,b))) { prepared.apply(); }
        assertNotNull(task.local());
        Path drop=Files.write(game.resolve("external.jar"),b);
        try(var supplied=task.dropped(new tc4revival.download.DownloadQueue.DroppedFile(drop,b.length,OriginalResources.sha256(b),""))) { supplied.apply(); }
        assertArrayEquals(b,Files.readAllBytes(drop));
    }

    @Test void hostedFixRejectsIncorrectBytesBeforeExtraction() throws Exception {
        var reference = new OriginalResourceDownloads.Target("example","Example","Example",OriginalResources.sha256(jar("1.12.2")),
                CurseForgeDownload.HEARTWORK_FIX.toString(),Map.of("assets/example/image.png",A),false,false);
        var task = new OriginalArchiveTask(game, reference);
        var different = jar("1.7.10");
        var failure = assertThrows(IOException.class, () -> task.prepareDownload(
                CurseForgeDownload.HEARTWORK_FIX, file -> Files.write(file, different)));
        assertTrue(failure.getMessage().contains("SHA-256"));
        try (var remaining = Files.list(game.resolve("thaumcraft"))) {
            assertEquals(0, remaining.count());
        }
    }
    @Test void unknownLocalArchiveIsNotInstalledAndWrongDropIsIgnored() throws Exception {
        var data=jar("1.7.10");var target=target(jar("1.12.2"),"example","1.7.10",A);
        Files.createDirectories(game.resolve("thaumcraft"));
        Path file=Files.write(game.resolve("thaumcraft/manual.jar"),data);
        var task=new OriginalArchiveTask(game,target);
        assertNull(task.local());
        assertNull(task.dropped(new tc4revival.download.DownloadQueue.DroppedFile(file,data.length,OriginalResources.sha256(data),"")));
        assertTrue(Files.isRegularFile(file));
    }
    @Test void minecraftVersionsAreSeparateAndFailedExtractionPreservesOriginalJar() throws Exception {
        var old=jar("1.7.10");var newer=jar("1.12.2");var targets=List.of(target(old,"example","1.7.10",A),target(newer,"example6","1.12.2",A));
        Files.createDirectories(game.resolve("thaumcraft"));Files.write(game.resolve("thaumcraft/old.jar"),old);Files.write(game.resolve("thaumcraft/new.jar"),newer);
        assertTrue(ArchiveDownloadFixture.obtainMissing(game,targets,t->{},(u,p)->fail()).isEmpty());
        assertNotNull(targets.get(0).read(game.resolve("thaumcraft/example_1_7_10")));assertNotNull(targets.get(1).read(game.resolve("thaumcraft/example_1_12_2")));
        assertThrows(IOException.class,()->targets.get(1).read(game.resolve("thaumcraft/example_1_7_10")));
        Path other=game.resolve("blocked");Files.createDirectories(other.resolve("thaumcraft"));Files.write(other.resolve("thaumcraft/source.jar"),old);Files.writeString(other.resolve("thaumcraft/example_1_7_10"),"user file");
        var failed=ArchiveDownloadFixture.obtainMissing(other,List.of(targets.get(0)),t->{},(u,p)->fail()).get(0);
        assertFalse(failed.failure().isEmpty());assertArrayEquals(old,Files.readAllBytes(other.resolve("thaumcraft/source.jar")));
    }
    @Test void directoryRequirementsAndNewAddonRequirementsTriggerReprocessing() throws Exception {
        var data=jar("1.7.10");var first=target(data,"example","1.7.10",A);
        ArchiveDownloadFixture.obtainMissing(game,List.of(first),t->{},(u,p)->Files.write(p,data));
        var all=new OriginalResourceDownloads.Target(first.owner(),first.name(),first.legacyModId(),first.sha256(),first.page(),first.mapping(),false,false,first.minecraftVersion(),first.downloads(),List.of("assets/example/"));
        var second=target(data,"new_addon","1.7.10",B);var calls=new AtomicInteger();
        var result=ArchiveDownloadFixture.obtainMissing(game,List.of(all,second),t->{},(u,p)->{calls.incrementAndGet();Files.write(p,data);});
        assertEquals(1,calls.get());assertNotNull(second.read(result.get(0).downloaded()));
    }
}
