package tc4revival.research;

import java.util.List;
import java.util.Optional;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Nearest visible scan target using vanilla bounding-box intersection. */
public  class EntityScanRay {
    public record Candidate<T>(T entity, AABB bounds, float border, float feetDistance, boolean visible) {}
    private EntityScanRay() {}

    public static <T> Optional<T> select(Vec3 eye, Vec3 end, List<Candidate<T>> candidates) {
        T selected = null;
        double distance = Double.POSITIVE_INFINITY;
        for (var candidate : candidates) {
            if (candidate.feetDistance() < 0.5f || !candidate.visible()) continue;
            var bounds = candidate.bounds().inflate(Math.max(0.8f, candidate.border()));
            boolean inside = bounds.contains(eye);
            if (inside) {
                selected = candidate.entity();
                distance = 0;
                break;
            } else {
                var hit = bounds.clip(eye, end);
                if (hit.isEmpty()) continue;
                double next = eye.distanceTo(hit.get());
                if (next < distance) {
                    selected = candidate.entity();
                    distance = next;
                }
            }
        }
        return Optional.ofNullable(selected);
    }
}
