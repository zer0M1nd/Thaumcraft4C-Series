package tc4revival.content;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InfusionPlacementOrderTest {
    @Test void picksOppositesBeforeFillingAngularGaps() {
        var c=BlockPos.ZERO;var east=new BlockPos(4,-2,0);var west=new BlockPos(-4,-2,0);
        var north=new BlockPos(0,-2,-4);var south=new BlockPos(0,-2,4);
        var ring=List.of(east,new BlockPos(3,-2,-3),north,new BlockPos(-3,-2,-3),west,new BlockPos(-3,-2,3),south,new BlockPos(3,-2,3));
        var selected=InfusionPlacementOrder.select(c,ring,8,Direction.EAST);
        assertEquals(east,selected.get(0));assertEquals(west,selected.get(1));
        assertTrue(selected.subList(0,4).containsAll(List.of(north,south)));
        assertEquals(ring,InfusionPlacementOrder.counterclockwise(c,selected));
    }
    @Test void pairedPedestalsPrecedeCloserUnpairedPedestalAcrossAngleSeam() {
        var single=new BlockPos(1,-2,0);var a=new BlockPos(-4,-2,-1);var b=new BlockPos(4,-2,1);
        assertEquals(List.of(b,a,single),InfusionPlacementOrder.select(BlockPos.ZERO,List.of(single,a,b),3,Direction.EAST));
        assertEquals(List.of(),InfusionPlacementOrder.select(BlockPos.ZERO,List.of(a),0,Direction.NORTH));
        assertThrows(IllegalArgumentException.class,()->InfusionPlacementOrder.select(BlockPos.ZERO,List.of(a),2,Direction.NORTH));
    }
}
