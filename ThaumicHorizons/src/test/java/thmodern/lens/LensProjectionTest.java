package thmodern.lens;

import org.junit.jupiter.api.Test;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import static org.junit.jupiter.api.Assertions.*;

class LensProjectionTest {
    @Test void allCameraHeadingsKeepFrontVisibleAndBackHidden() {
        for (float yaw : new float[]{0, 90, 180, 270}) {
            for (float pitch : new float[]{-80, 0, 80}) {
                var rotation = new Quaternionf().rotationYXZ((float)Math.toRadians(-yaw), (float)Math.toRadians(pitch), 0);
                var front = new Vector3f(0, 0, 8).rotate(rotation);
                var point = LensProjection.project(front, rotation, 800, 600, 70);
                assertNotNull(point);
                assertEquals(400, point.x(), .001);
                assertEquals(300, point.y(), .001);
                assertNull(LensProjection.project(new Vector3f(front).negate(), rotation, 800, 600, 70));
                var left = LensProjection.project(new Vector3f(2, 1, 8).rotate(rotation), rotation, 800, 600, 70);
                assertTrue(left.x() < 400);
                assertTrue(left.y() < 300);
            }
        }
    }
    @Test void rejectsCameraPlaneAndPreservesInputVectors() {
        var relative = new Vector3f(0, 0, 0);
        var rotation = new Quaternionf();
        assertNull(LensProjection.project(relative, rotation, 800, 600, 70));
        assertEquals(new Vector3f(), relative);
        assertEquals(new Quaternionf(), rotation);
    }
}
