package thaumicomniverse.compat.create.airship;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightManeuverTest extends FlightTestConfig {
    @Test void tightReturnsAndReversalsDoNotWhipTheHeading(){
        for(var end:new Vec3[]{new Vec3(-6,0,0),new Vec3(.17,0,.02),new Vec3(6,4,6)})
            for(float yaw:new float[]{0,15,90,180,270}){
                var a=FlightNavigation.forward(yaw);var b=new Vec3(-1,0,0);
                var path=FlightPath.transition(Vec3.ZERO,a,end,b,6);
                assertTrue(path.position(0).distanceTo(Vec3.ZERO)<1e-8);
                assertTrue(path.position(path.length()).distanceTo(end)<1e-8);
                assertEquals(1,path.tangent(0).dot(a),1e-7);assertEquals(1,path.tangent(path.length()).dot(b),1e-7);
                for(double d=.1;d<=path.length();d+=.1)
                    assertTrue(path.tangent(d-.1).dot(path.tangent(d))>Math.cos(.1/3),"Sharp turn at "+d+" end="+end+" yaw="+yaw);
                for(int i=1;i<path.curves.size();i++)assertEquals(1,path.curves.get(i-1).tangent(path.curves.get(i-1).length()).dot(path.curves.get(i).tangent(0)),1e-7);
            }
    }
}
