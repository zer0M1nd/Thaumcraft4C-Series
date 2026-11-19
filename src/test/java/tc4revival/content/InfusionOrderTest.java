package tc4revival.content;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class InfusionOrderTest {
    @Test void rotationIsAllowedButReflectionIsNot() {
        assertTrue(InfusionOrder.cyclicMatches(List.of("a","b","c","d"),List.of("c","d","a","b"),String::equals));
        assertFalse(InfusionOrder.cyclicMatches(List.of("a","b","c","d"),List.of("a","d","c","b"),String::equals));
        assertFalse(InfusionOrder.cyclicMatches(List.of("a","b"),List.of("a"),String::equals));
    }
    @Test void verticalPedestalsAreExcludedAndJeiPlacementHasSameOrientation() {
        var center=new BlockPos(10,64,10);
        var positions=List.of(center.east(3),center.west(3),center.north(3),center.south(3),center.above(),center.below());
        var ordered=InfusionOrder.clockwise(center,positions);
        assertEquals(4,ordered.size());
        var ccw=InfusionPlacementOrder.counterclockwise(center,ordered);
        var placed=new java.util.ArrayList<BlockPos>();placed.add(ccw.get(0));
        for(int i=ccw.size()-1;i>0;i--)placed.add(ccw.get(i));
        assertTrue(InfusionOrder.cyclicMatches(ordered,placed,BlockPos::equals));
    }
}
