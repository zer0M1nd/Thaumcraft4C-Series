package ttmodern;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.core.*;

class AwakenedAreaTest {
    @Test void planesContainTwentyFourExtraBlocksAndStayOnHitPlane(){
        var center=new BlockPos(-12,80,18);
        for(var face:Direction.values()){
            var selected=AwakenedArea.select("pick",1,center,face);assertEquals(24,selected.size());assertFalse(selected.contains(center));assertEquals(24,new java.util.HashSet<>(selected).size());
            for(var pos:selected)assertEquals(face.getAxis().choose(center.getX(),center.getY(),center.getZ()),face.getAxis().choose(pos.getX(),pos.getY(),pos.getZ()));
        }
    }
    @Test void tunnelsAreTenBlocksIncludingOriginInEveryDirection(){
        var center=new BlockPos(0,0,0);
        for(var face:Direction.values()){
            var selected=AwakenedArea.select("pick",2,center,face);assertEquals(9,selected.size());assertEquals(center.relative(face.getOpposite(),9),selected.get(8));
        }
    }
    @Test void excavationColumnUsesSixteenBlocksAndUnknownModesDoNothing(){
        var center=new BlockPos(0,20,0);var selected=AwakenedArea.select("shovel",2,center,Direction.NORTH);
        assertEquals(15,selected.size());assertTrue(selected.contains(center.below(8)));assertTrue(selected.contains(center.above(7)));
        assertTrue(AwakenedArea.select("pick",-1,center,Direction.UP).isEmpty());assertTrue(AwakenedArea.select("pick",0,center,Direction.UP).isEmpty());
    }
}
