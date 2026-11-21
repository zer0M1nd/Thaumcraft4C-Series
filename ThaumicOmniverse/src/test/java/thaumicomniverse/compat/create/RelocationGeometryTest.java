package thaumicomniverse.compat.create;

import java.util.*;
import net.minecraft.world.phys.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static thaumicomniverse.compat.create.RelocationGeometry.*;

class RelocationGeometryTest {
    private Box box(double x,double y,double z,double sx,double sy,double sz){return axisAligned(new AABB(x,y,z,x+sx,y+sy,z+sz));}
    private Box rotated(Vec3 center,double angle){
        double c=Math.cos(angle),s=Math.sin(angle),e=(Math.abs(c)+Math.abs(s))*.5;
        return new Box(center,new Vec3[]{new Vec3(c,0,s),new Vec3(0,1,0),new Vec3(-s,0,c)},new double[]{.5,.5,.5},new AABB(center.add(-e,-.5,-e),center.add(e,.5,e)));
    }
    @Test void broadPhaseOverlapDoesNotCollideWithEmptyCornerOfRotatedCube(){
        var a=rotated(new Vec3(.5,.5,.5),Math.PI/4);var b=box(1.1,0,1.1,.1,1,.1);
        assertTrue(a.bounds().intersects(b.bounds()));assertFalse(overlaps(a,b));
        assertEquals(Vec3.ZERO,correction(List.of(a),List.of(b)));
    }
    @Test void touchingFacesAreNotOverlaps(){assertFalse(overlaps(box(0,0,0,1,1,1),box(0,-1,0,1,1,1)));}
    @Test void shallowGroundOverlapOnlyChangesHeightAtAnyYaw(){
        for(double yaw:new double[]{0,.1,.4,.8,1.3}){
            var a=rotated(new Vec3(3.127,.49,4.338),yaw);var ground=box(-50,-1,-50,100,1,100);
            var offset=correction(List.of(a),List.of(ground));assertEquals(0,offset.x);assertEquals(0,offset.z);assertEquals(.01,offset.y,1e-6);assertFalse(overlaps(a.move(offset),ground));
        }
    }
    @Test void oneByOneShaftPreservesTheUnconstrainedFractionalHeight(){
        var a=box(.02,7.125,-.03,1,1,1);
        var walls=List.of(box(-5,0,-5,5,20,10),box(1,0,-5,5,20,10),box(0,0,-5,1,20,5),box(0,0,1,1,20,5));
        var offset=correction(List.of(a),walls);assertEquals(-.02,offset.x,1e-6);assertEquals(.03,offset.z,1e-6);assertEquals(0,offset.y);
        for(var b:walls)assertFalse(overlaps(a.move(offset),b));
    }
    @Test void separatedPartsDoNotCollideWithAnObstacleInTheHullGap(){
        assertEquals(Vec3.ZERO,correction(List.of(box(-2,0,0,1,1,1),box(2,0,0,1,1,1)),List.of(box(0,0,0,1,1,1))));
    }
}
