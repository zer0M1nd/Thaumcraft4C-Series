package tc4revival.client;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import tc4revival.resources.OriginalResources;
import static org.junit.jupiter.api.Assertions.*;

class CrabVentModelTest {
    @Test void originalVentIsMappedWholeWithItsStaticTextureAndOutwardGeometry() throws Exception {
        var mapping=OriginalResources.mappings();
        assertEquals("assets/thaumcraft/textures/models/crabvent.obj",mapping.get("assets/thaumcraft/models/crabvent.obj"));
        assertEquals("assets/thaumcraft/textures/models/crabvent.png",mapping.get("assets/thaumcraft/textures/models/crabvent.png"));
        var path=Path.of("references/Thaumcraft-1.7.10-4.2.3.5.jar");
        org.junit.jupiter.api.Assumptions.assumeTrue(java.nio.file.Files.isRegularFile(path));
        try(var jar=new ZipFile(path.toFile())) {
            String text=new String(jar.getInputStream(jar.getEntry(mapping.get("assets/thaumcraft/models/crabvent.obj"))).readAllBytes(),StandardCharsets.UTF_8);
            var mesh=GroupedObjMesh.read(new StringReader(text));
            assertEquals(java.util.Set.of("Vent"),mesh.groups().keySet());
            assertEquals(320*4,mesh.groups().get("Vent").size());
            assertTrue(mesh.groups().get("Vent").stream().allMatch(v->v.z()>=.5f&&v.z()<=.73f));
            var texture=javax.imageio.ImageIO.read(jar.getInputStream(jar.getEntry(mapping.get("assets/thaumcraft/textures/models/crabvent.png"))));
            assertEquals(32,texture.getWidth());assertEquals(32,texture.getHeight());
            assertNull(jar.getEntry("assets/thaumcraft/textures/models/crabvent.png.mcmeta"));
            for(var side:Direction.values()) {
                var front=new Vector3f(0,0,1).rotate(EldritchCrabSpawnerRenderer.rotation(side));
                assertEquals(side.getStepX(),front.x,1e-5);assertEquals(side.getStepY(),front.y,1e-5);assertEquals(side.getStepZ(),front.z,1e-5);
                for(var vertex:mesh.groups().get("Vent")) {
                    var point=new Vector3f(vertex.x(),vertex.y(),vertex.z()).rotate(EldritchCrabSpawnerRenderer.rotation(side));
                    assertTrue(point.dot(front)>=.4999f,"Vent must project outside the facing cube face");
                }
            }
        }
    }
}
