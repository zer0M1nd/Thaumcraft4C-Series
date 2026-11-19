package tc4revival.client;

import java.io.StringReader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GroupedObjMeshTest {
    @Test void textureOnlyTriangleUsesFaceNormalAndPreservesUvs() throws Exception {
        var mesh=GroupedObjMesh.read(new StringReader("""
                v 0 0 0
                v 1 0 0
                v 0 1 0
                vt 0 .25
                vt 1 .25
                vt 0 1
                g pillar
                f 1/1 2/2 3/3
                """));
        var vertices=mesh.groups().get("pillar");
        assertEquals(4, vertices.size());
        assertEquals(vertices.get(2), vertices.get(3));
        assertEquals(.75f, vertices.get(0).v());
        for(var vertex:vertices) {
            assertEquals(0,vertex.nx());
            assertEquals(0,vertex.ny());
            assertEquals(1,vertex.nz());
        }
    }
    @Test void explicitNormalsAndRelativeIndicesRemainSupported() throws Exception {
        var mesh=GroupedObjMesh.read(new StringReader("""
                v 0 0 0
                v 1 0 0
                v 0 1 0
                vn 0 0 -1
                f -3//1 -2//1 -1//1
                """));
        for(var vertex:mesh.groups().get("default"))assertEquals(-1,vertex.nz());
    }
}
