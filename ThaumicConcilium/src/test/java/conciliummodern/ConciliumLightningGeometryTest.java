package conciliummodern;

import java.util.Comparator;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConciliumLightningGeometryTest {
    @Test void legacyRotationUsesTheOriginalMatrixHandedness(){
        var rotated=ConciliumLightningGeometry.rotate(new Vec3(1,0,0),new Vec3(0,0,1),90);
        assertEquals(0,rotated.x,1e-7);assertEquals(-1,rotated.y,1e-7);assertEquals(0,rotated.z,1e-7);
        var reverse=ConciliumLightningGeometry.rotate(rotated,new Vec3(0,0,1),-90);assertTrue(reverse.distanceTo(new Vec3(1,0,0))<1e-7);
    }
    @Test void fractalKeepsEndpointsAndFiniteJoinsAtEveryViewingAxis(){
        var from=new Vec3(-3,70,2);
        for(var delta:new Vec3[]{Vec3.ZERO,new Vec3(0,16,0),new Vec3(12,0,0),new Vec3(.01,-4,20)}){
            var to=from.add(delta);var bolt=new ConciliumLightningGeometry(from,to,731);
            var main=bolt.edges.stream().filter(edge->edge.brightness==1).sorted(Comparator.comparingInt(edge->edge.progress)).toList();
            assertFalse(main.isEmpty());assertEquals(from,main.get(0).start());assertEquals(to,main.get(main.size()-1).end());
            for(var edge:bolt.edges)for(var view:new Vec3[]{new Vec3(1,0,0),new Vec3(0,1,0),new Vec3(0,0,1)}){
                assertTrue(Double.isFinite(edge.start().lengthSqr()));assertTrue(Double.isFinite(edge.end().lengthSqr()));
                assertTrue(Double.isFinite(edge.join(true,view,.125f).lengthSqr()));assertTrue(Double.isFinite(edge.join(false,view,.125f).lengthSqr()));
            }
        }
    }
    @Test void shortAndLongBoltsRevealMonotonicallyAndAlwaysExpire(){
        for(double distance:new double[]{.001,1,8,50,200}){
            var bolt=new ConciliumLightningGeometry(Vec3.ZERO,new Vec3(distance,0,0),17);int shown=-1,ticks=0;
            while(!bolt.expired()&&ticks++<200){int next=bolt.visibleSegments(0);assertTrue(next>=shown);shown=next;bolt.tick();}
            assertTrue(bolt.expired(),"Cosmetic bolt must leave its client list within a bounded lifetime");
        }
    }
}
