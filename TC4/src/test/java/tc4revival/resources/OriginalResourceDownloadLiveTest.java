package tc4revival.resources;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Actual network and original archives; no writes to a player instance. */
@EnabledIfEnvironmentVariable(named="TC4_LIVE_DOWNLOAD_TEST",matches="true")
class OriginalResourceDownloadLiveTest {
    @TempDir Path game;
    @Test void allInstalledAddonOriginalsDownloadExtractRepairAndReuse() throws Exception {
        var targets=new ArrayList<OriginalResourceDownloads.Target>();
        targets.add(new OriginalResourceDownloads.Target("thaumcraft","Thaumcraft","Thaumcraft",OriginalResources.REFERENCE_SHA256,
            "https://www.curseforge.com/minecraft/mc-mods/thaumcraft/files/2227552",OriginalResources.mappings(),true,false));
        try(var modules=Files.list(Path.of("../Addons"))) {
            for(var module:modules.filter(Files::isDirectory).toList())for(String tree:List.of("src/main/resources/data","src/generated/resources/data")) {
                var root=module.resolve(tree);if(!Files.isDirectory(root))continue;
                try(var namespaces=Files.list(root)) {
                    for(var namespace:namespaces.filter(Files::isDirectory).toList()) {
                        var declaration=namespace.resolve("thaumcraft/original_resources.json");
                        if(Files.isRegularFile(declaration))targets.addAll(OriginalDownloadTargets.addons(namespace.getFileName().toString(),declaration));
                    }
                }
            }
        }
        var identities = targets.stream().map(t -> t.legacyModId() + "@" + t.minecraftVersion()).collect(java.util.stream.Collectors.toSet());
        assertTrue(identities.containsAll(Set.of("Thaumcraft@1.7.10", "thaumcraft@1.12.2")));
        assertEquals(identities.size(), OriginalResourceDownloads.grouped(targets).size());
        var results=ArchiveDownloadFixture.obtainMissing(game,targets,System.out::println,CurseForgeDownload::fetch);
        assertEquals(identities.size(),results.size());
        for(var result:results){assertEquals("",result.failure(),result.owner());assertTrue(Files.isDirectory(result.downloaded()));}
        assertTrue(ArchiveDownloadFixture.obtainMissing(game,targets,t->{},(u,p)->fail("Intact resources avoid network")).isEmpty());
        var tc6=targets.stream().filter(t->t.minecraftVersion().equals("1.12.2")).findFirst().orElseThrow();
        var directory=game.resolve("thaumcraft/thaumcraft_1_12_2");Files.delete(directory.resolve(tc6.mapping().values().iterator().next()));
        var repaired=ArchiveDownloadFixture.obtainMissing(game,targets,System.out::println,CurseForgeDownload::fetch);
        assertEquals(1,repaired.size());assertEquals("",repaired.get(0).failure());assertNotNull(tc6.read(directory));
        try(var files=Files.list(game.resolve("thaumcraft"))){assertTrue(files.allMatch(Files::isDirectory));}
    }
}
