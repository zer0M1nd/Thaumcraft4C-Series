package tc4revival.resources;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OriginalDownloadTargetsTest {
    @Test void allSixExistingAddonDeclarationsValidateTheirRealReferenceWithoutAddonChanges() throws Exception {
        var roots=Map.of("ThaumicTinkerer","thaumic_tinkerer","ForbiddenMagic","forbidden_magic","ThaumicBases","thaumicbases",
                "ThaumicEnergistics","thaumicenergistics","ThaumicHorizons","thaumichorizons","Automagy","automagy");
        for(var entry:roots.entrySet()) {
            var root=Path.of("../Addons",entry.getKey());
            org.junit.jupiter.api.Assumptions.assumeTrue(Files.isDirectory(root),"Local addon checkouts required");
            var resources=root.resolve("src/main/resources");
            var target=OriginalDownloadTargets.addon(entry.getValue(),resources.resolve("data/"+entry.getValue()+"/thaumcraft/original_resources.json"),resources.resolve("tbmodern/resource-map.json"));
            assertNotNull(target,entry.getKey());assertNotNull(CurseForgeDownload.endpoint(target.page()));
            var matches=new ArrayList<Path>();
            // Original archives moved outside Code; keep old checkout locations as fallbacks.
            var archives=List.of(Path.of("../../References/Projects",entry.getKey()),
                    Path.of("../../References/Addons"),root.resolve("references"),root.resolve("run-server-final/thaumcraft"));
            org.junit.jupiter.api.Assumptions.assumeTrue(archives.stream().anyMatch(Files::isDirectory), "Local original archives required");
            for(var directory:archives) {
                if(!Files.isDirectory(directory))continue;
                try(var files=Files.walk(directory)) {
                    for(var file:files.filter(p->Files.isRegularFile(p)&&p.toString().endsWith(".jar")).toList()) {
                        if(OriginalResources.sha256(Files.readAllBytes(file)).equals(target.sha256()))matches.add(file);
                    }
                }
                if(!matches.isEmpty())break;
            }
            assertFalse(matches.isEmpty(),"Reference hash present: "+entry.getKey());
            assertEquals(target.sha256(),target.read(matches.get(0)).sha256(),entry.getKey());
            assertFalse(target.mappedOnly());
        }
    }
}
