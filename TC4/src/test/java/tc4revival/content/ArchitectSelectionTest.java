package tc4revival.content;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArchitectSelectionTest {
    @Test void asymmetricTradeSelectionRotatesWithAllSixFacesAndNeverLeavesThePlane() {
        var origin = new BlockPos(-150, -32, 400);
        for (var face : Direction.values()) {
            var result = ArchitectSelection.collect(origin, face, 1, 0, 2, false, pos -> true);
            assertEquals(15, result.size(), face.toString());
            for (var pos : result) {
                var delta = pos.subtract(origin);
                assertEquals(0, face.getAxis().choose(delta.getX(), delta.getY(), delta.getZ()));
                if (face.getAxis() == Direction.Axis.Y) { assertTrue(Math.abs(delta.getX()) <= 1); assertTrue(Math.abs(delta.getZ()) <= 2); }
                else { assertTrue(Math.abs(delta.getY()) <= 1); assertTrue(Math.abs(face.getAxis() == Direction.Axis.Z ? delta.getX() : delta.getZ()) <= 2); }
            }
        }
    }
    @Test void anIneligibleBridgeSeparatesOtherwiseEligibleBlocksInsideTheSameBounds() {
        var origin = BlockPos.ZERO;
        Set<BlockPos> allowed = Set.of(origin, origin.east(), origin.east(3), origin.north().east());
        var result = ArchitectSelection.collect(origin, Direction.UP, 4, 0, 4, false, allowed::contains);
        assertEquals(Set.of(origin, origin.east(), origin.north().east()), new HashSet<>(result));
        assertEquals(result.size(), new HashSet<>(result).size());
        assertTrue(ArchitectSelection.collect(origin.above(), Direction.UP, 4, 0, 4, false, allowed::contains).isEmpty());
    }
    @Test void maximumVolumesAreBoundedWithoutRecursiveTraversalAndEvaluateEachCandidateOnce() {
        Set<BlockPos> inspected = new HashSet<>();
        var result = ArchitectSelection.collect(BlockPos.ZERO, Direction.WEST, 8, 8, 8, true, pos -> {
            assertTrue(inspected.add(pos), "Each eligible candidate must be evaluated once");
            assertTrue(Math.abs(pos.getX()) <= 8 && Math.abs(pos.getY()) <= 8 && Math.abs(pos.getZ()) <= 8);
            return true;
        });
        assertEquals(4913, result.size()); assertEquals(inspected, new HashSet<>(result));
        assertThrows(IllegalArgumentException.class, () -> ArchitectSelection.collect(BlockPos.ZERO, Direction.UP, Integer.MAX_VALUE, 0, 0, true, pos -> true));
    }
    @Test void singleBlockAndTraversalOrderRemainDeterministic() {
        assertEquals(java.util.List.of(BlockPos.ZERO), ArchitectSelection.collect(BlockPos.ZERO, Direction.SOUTH, 0, 0, 0, true, pos -> true));
        var first = ArchitectSelection.collect(BlockPos.ZERO, Direction.UP, 1, 1, 1, true, pos -> true);
        assertEquals(first, ArchitectSelection.collect(BlockPos.ZERO, Direction.UP, 1, 1, 1, true, pos -> true));
        assertEquals(BlockPos.ZERO, first.get(0)); assertEquals(BlockPos.ZERO.below(), first.get(1));
    }
}
