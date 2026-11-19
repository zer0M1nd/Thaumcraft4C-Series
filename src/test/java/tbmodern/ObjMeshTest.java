package tbmodern;

import org.junit.jupiter.api.Test;
import tbmodern.resources.ObjMesh;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;

class ObjMeshTest {
    @Test void revolverFitsInsideInventorySlotAfterLegacyGuiRotation() throws Exception {
        var path=java.nio.file.Path.of("references/ThaumicBases-1.3.1710.4.jar");
        org.junit.jupiter.api.Assumptions.assumeTrue(java.nio.file.Files.isRegularFile(path));
        try(var jar=new java.util.zip.ZipFile(path.toFile());
            var reader=new InputStreamReader(jar.getInputStream(jar.getEntry("assets/thaumicbases/models/revolver/revolver.obj")),java.nio.charset.StandardCharsets.UTF_8)) {
            var mesh=ObjMesh.read(reader);var fit=tbmodern.resources.GuiModelFit.of(mesh);
            assertFalse(mesh.groups().isEmpty());
            for(var group:mesh.groups().values())for(var face:group)for(var vertex:java.util.List.of(face.a(),face.b(),face.c())) {
                var p=tbmodern.resources.GuiModelFit.project(vertex.position());
                double x=.5+(p.x()-fit.x)*fit.scale,y=.5+(p.y()-fit.y)*fit.scale;
                assertTrue(x>=.079 && x<=.921 && y>=.079 && y<=.921,"Each part stays within the slot with a margin");
            }
        }
    }
    @Test void bothLegacyNodeDevicesHaveUsableMappedGeometryAndTextures() throws Exception {
        var root=java.nio.file.Path.of("references/ThaumicBases-1.3.1710.4.jar");
        org.junit.jupiter.api.Assumptions.assumeTrue(java.nio.file.Files.isRegularFile(root));
        var mappings=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/tbmodern/resource-map.json"))).getAsJsonObject().getAsJsonObject("mapping");
        try(var jar=new java.util.zip.ZipFile(root.toFile())) {
            for(String name:java.util.List.of("nodelinker","nodemanipulator")) {
                var path=mappings.get("assets/thaumicbases/models/"+name+"/"+name+".obj").getAsString();
                var entry=jar.getEntry(path);assertNotNull(entry,path);
                ObjMesh mesh;try(var reader=new InputStreamReader(jar.getInputStream(entry),java.nio.charset.StandardCharsets.UTF_8)){mesh=ObjMesh.read(reader);}
                assertFalse(mesh.groups().isEmpty());
                if(name.equals("nodemanipulator"))assertEquals(java.util.Set.of("base_Cube.001","handle_0_Cube.002","handle_1_Cube.002","handle_2_Cube.002","handle_3_Cube.002","foci_Cube.003"),mesh.groups().keySet());
                for(var group:mesh.groups().values())assertTrue(group.size()>=12);
                var tex="assets/thaumicbases/textures/blocks/"+name+"/"+(name.equals("nodelinker")?"nodelinkeruv":"baseuvmap")+".png";
                var texture=javax.imageio.ImageIO.read(jar.getInputStream(jar.getEntry(mappings.get(tex).getAsString())));
                assertNotNull(texture);assertTrue(texture.getWidth()>16);
            }
        }
    }
    private static final String QUAD="v 0 0 0\nv 1 0 0\nv 1 1 0\nv 0 1 0\nvt 0 0\nvt 1 0\nvt 1 1\nvt 0 1\no cylinder\nf -4/1 -3/2 -2/3 -1/4\n";
    @Test void triangulatesNamedPartsAndNegativeIndices() throws IOException {
        var faces=ObjMesh.read(new StringReader(QUAD)).groups().get("cylinder");
        assertEquals(2,faces.size());
        assertEquals(1,faces.get(0).a().uv().v());
        assertEquals(1,faces.get(0).a().normal().z());
        assertEquals(new ObjMesh.Point(0,1,0),faces.get(1).c().position());
        assertThrows(UnsupportedOperationException.class,()->faces.clear());
    }
    @Test void rejectsInvalidIndicesAndNonFiniteCoordinates() {
        assertThrows(IOException.class,()->ObjMesh.read(new StringReader("v NaN 0 0")));
        assertThrows(IOException.class,()->ObjMesh.read(new StringReader(QUAD.replace("-4/1","0/1"))));
        assertThrows(IOException.class,()->ObjMesh.read(new StringReader(QUAD.replace("-4/1","50/1"))));
    }
    @Test void honorsProvidedNormalsAndDoesNotResolveMaterialPaths() throws IOException {
        var mesh=ObjMesh.read(new StringReader("mtllib ../../outside.mtl\nusemtl anything\nv 0 0 0\nv 1 0 0\nv 0 1 0\nvn 0 0 -1\nf 1//1 2//1 3//1"));
        assertEquals(-1,mesh.groups().get("default").get(0).a().normal().z());
    }
}
