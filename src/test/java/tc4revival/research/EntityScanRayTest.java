package tc4revival.research;

import java.util.List;
import org.junit.jupiter.api.Test;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import static org.junit.jupiter.api.Assertions.*;

class EntityScanRayTest {
    private final Vec3 eye = new Vec3(0, 1, 0);
    private final Vec3 end = new Vec3(10, 1, 0);
    private EntityScanRay.Candidate<String> candidate(String name, double x, double z, float distance, boolean visible) {
        return new EntityScanRay.Candidate<>(name, new AABB(x, 0, z, x + 0.5, 2, z + 0.5), 0.1f, distance, visible);
    }

    @Test void nearerInterceptWinsButEqualDistancesPreserveWorldIterationOrder() {
        var far = candidate("far", 7, 0, 7, true);
        var near = candidate("near", 3, 0, 3, true);
        assertEquals("near", EntityScanRay.select(eye, end, List.of(far, near)).orElseThrow());
        var tie = candidate("tie", 3, 0, 3, true);
        assertEquals("near", EntityScanRay.select(eye, end, List.of(near, tie)).orElseThrow());
        assertEquals("tie", EntityScanRay.select(eye, end, List.of(tie, near)).orElseThrow());
    }
    @Test void minimumFeetDistanceIsHalfABlockAndOccludedCandidatesAreSkipped() {
        var close = candidate("close", 2, 0, 0.499f, true);
        var hidden = candidate("hidden", 2, 0, 2, false);
        var boundary = candidate("boundary", 3, 0, 0.5f, true);
        assertEquals("boundary", EntityScanRay.select(eye, end, List.of(close, hidden, boundary)).orElseThrow());
    }
    @Test void pointEightExpansionIncludesNearMissesButDoesNotChangeRayLength() {
        assertEquals("edge", EntityScanRay.select(eye, end, List.of(candidate("edge", 4, 0.79, 4, true))).orElseThrow());
        assertTrue(EntityScanRay.select(eye, end, List.of(candidate("miss", 4, 0.81, 4, true))).isEmpty());
        assertTrue(EntityScanRay.select(eye, end, List.of(candidate("beyond", 10.81, 0, 11, true))).isEmpty());
    }
    @Test void largerCollisionBordersAreHonored() {
        var expanded = new EntityScanRay.Candidate<>("large", new AABB(4, 0, 1, 5, 2, 2), 1.1f, 4, true);
        assertEquals("large", EntityScanRay.select(eye, end, List.of(expanded)).orElseThrow());
    }
    @Test void insideHitWinsRegardlessOfCandidateOrder() {
        var inside = candidate("inside", 0.5, 0, 1, true);
        var far = candidate("far", 7, 0, 7, true);
        assertEquals("inside", EntityScanRay.select(eye, end, List.of(far, inside)).orElseThrow());
        assertEquals("inside", EntityScanRay.select(eye, end, List.of(inside, far)).orElseThrow());
    }
    @Test void candidatesBehindTheEyeAndAnEmptyListDoNotProduceHits() {
        assertTrue(EntityScanRay.select(eye, end, List.of(candidate("behind", -3, 0, 3, true))).isEmpty());
        assertTrue(EntityScanRay.select(eye, end, List.of()).isEmpty());
    }
    @Test void segmentEndpointsAndBoxEdgesAreInclusiveAndAnInsideRayUsesItsExitFace() {
        var box = new AABB(5, 0, 0, 6, 2, 1);
        assertEquals(new Vec3(5, 1, 0), ScanRayIntersection.intersect(box, eye, new Vec3(5, 1, 0)).orElseThrow());
        assertTrue(ScanRayIntersection.intersect(box, eye, new Vec3(4.999, 1, 0)).isEmpty());
        assertEquals(new Vec3(6, 1, 0.5), ScanRayIntersection.intersect(box, new Vec3(5.5, 1, 0.5), new Vec3(7, 1, 0.5)).orElseThrow());
    }
    @Test void nearlyParallelAxesUseTheOldSquaredDeltaThreshold() {
        var box = new AABB(0, 0, 0, 1, 1, 1);
        assertTrue(ScanRayIntersection.intersect(box, new Vec3(-0.00001, 0.5, 0.5), new Vec3(0.00001, 0.5, 0.5)).isEmpty());
        assertEquals(new Vec3(0, 0.5, 0.5), ScanRayIntersection.intersect(box, new Vec3(-0.001, 0.5, 0.5), new Vec3(0.001, 0.5, 0.5)).orElseThrow());
    }
}
