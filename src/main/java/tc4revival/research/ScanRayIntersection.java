package tc4revival.research;

import java.util.Optional;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Inclusive segment/face intersection with the old vector parallel-axis threshold. */
public  class ScanRayIntersection {
    private ScanRayIntersection() {}
    public static Optional<Vec3> intersect(AABB box, Vec3 from, Vec3 to) {
        double[] start = { from.x, from.y, from.z };
        double[] delta = { to.x - from.x, to.y - from.y, to.z - from.z };
        double[] min = { box.minX, box.minY, box.minZ };
        double[] max = { box.maxX, box.maxY, box.maxZ };
        Vec3 closest = null;
        double distance = Double.POSITIVE_INFINITY;
        for (int axis = 0; axis < 3; axis++) {
            if (delta[axis] * delta[axis] < 1.0000000116860974E-7) continue;
            for (int face = 0; face < 2; face++) {
                double plane = face == 0 ? min[axis] : max[axis];
                double fraction = (plane - start[axis]) / delta[axis];
                if (!(fraction >= 0 && fraction <= 1)) continue;
                int a = (axis + 1) % 3;
                int b = (axis + 2) % 3;
                double hitA = start[a] + delta[a] * fraction;
                double hitB = start[b] + delta[b] * fraction;
                if (hitA < min[a] || hitA > max[a] || hitB < min[b] || hitB > max[b]) continue;
                var point = from.add(delta[0] * fraction, delta[1] * fraction, delta[2] * fraction);
                double next = from.distanceToSqr(point);
                if (next < distance) { closest = point; distance = next; }
            }
        }
        return Optional.ofNullable(closest);
    }
}
