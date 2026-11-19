package thmodern.lens;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Minecraft Camera local axes are left (+X), up (+Y), forward (+Z). */
public class LensProjection {
    public record Point(double x, double y) {}

    public static Point project(Vector3f relative, Quaternionf cameraRotation,
                                int width, int height, double fov) {
        var local = new Vector3f(relative).rotate(new Quaternionf(cameraRotation).conjugate());
        if (local.z <= .05f) return null;
        double focal = height / 2.0 / Math.tan(Math.toRadians(fov) / 2);
        return new Point(width / 2.0 - local.x * focal / local.z,
                height / 2.0 - local.y * focal / local.z);
    }
}
