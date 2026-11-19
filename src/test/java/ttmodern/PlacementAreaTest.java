package ttmodern;

import net.minecraft.core.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlacementAreaTest {
    @Test void verticalFacesProduceAnOddSquareGrowingUpFromTheTarget(){
        var origin=new BlockPos(7,-25,11);for(var face:Direction.Plane.HORIZONTAL)for(int size=3;size<=11;size+=2){var result=PlacementArea.positions(origin,face,Direction.NORTH,0,size);assertEquals(size*size,result.size());assertEquals(result.size(),new java.util.HashSet<>(result).size());for(var pos:result){assertTrue(pos.getY()>=origin.getY()&&pos.getY()<origin.getY()+size);assertEquals(face.getAxis().choose(origin.getX(),0,origin.getZ()),face.getAxis().choose(pos.getX(),0,pos.getZ()));}}
    }
    @Test void LookingDownBuildsFloorWhileShallowAngleBuildsAWall(){
        var origin=BlockPos.ZERO;var floor=PlacementArea.positions(origin,Direction.UP,Direction.NORTH,80,5);assertEquals(25,floor.size());assertTrue(floor.stream().allMatch(p->p.getY()==0));
        var north=PlacementArea.positions(origin,Direction.UP,Direction.NORTH,30,5);assertEquals(25,north.size());assertTrue(north.stream().allMatch(p->p.getZ()==0));
        var east=PlacementArea.positions(origin,Direction.UP,Direction.EAST,30,5);assertEquals(25,east.size());assertTrue(east.stream().allMatch(p->p.getX()==0));
    }
    @Test void MalformedSizeIsBounded(){assertEquals(121,PlacementArea.positions(BlockPos.ZERO,Direction.NORTH,Direction.NORTH,0,Integer.MAX_VALUE).size());assertEquals(9,PlacementArea.positions(BlockPos.ZERO,Direction.UP,Direction.NORTH,80,-100).size());}
}
