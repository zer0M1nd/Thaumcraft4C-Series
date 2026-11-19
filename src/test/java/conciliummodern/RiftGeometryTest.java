package conciliummodern;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RiftGeometryTest {
    @Test void originalTwoSidedWalkHasFiniteTaperedEnds(){
        for(int size:new int[]{0,1,2,3,49,50,299,300,4096})for(int seed:new int[]{0,1,-1,Integer.MIN_VALUE,Integer.MAX_VALUE}){
            var geometry=RiftGeometry.create(size,seed);int steps=(int)Math.ceil(size/3d);
            assertEquals(steps*2+2,geometry.points().size());assertEquals(geometry.points().size(),geometry.widths().size());
            assertEquals(0f,geometry.widths().get(0));assertEquals(0f,geometry.widths().get(geometry.widths().size()-1));
            for(var point:geometry.points())assertTrue(Double.isFinite(point.x)&&Double.isFinite(point.y)&&Double.isFinite(point.z));
            for(float width:geometry.widths())assertTrue(width>=0&&width<=size/300f);
            for(int i=1;i<steps;i++)assertTrue(geometry.widths().get(i)>=geometry.widths().get(i-1));
        }
    }
    @Test void savedSizeAndSeedReconstructTheSameShape(){
        var original=RiftGeometry.create(50,-2468);assertEquals(original,RiftGeometry.create(50,-2468));assertNotEquals(original,RiftGeometry.create(50,-2467));
        assertThrows(UnsupportedOperationException.class,()->original.points().clear());assertThrows(UnsupportedOperationException.class,()->original.widths().clear());
    }
}
