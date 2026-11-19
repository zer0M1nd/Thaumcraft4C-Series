package thaumicomniverse.compat.create.airship;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightInterpolationTest extends FlightTestConfig {
    @Test void delayedPacketsCannotSkipAcrossTheDeckAndEventuallySettle() {
        var p=Vec3.ZERO;var target=new Vec3(12,0,0);
        for(int tick=0;tick<30;tick++){
            var next=FlightInterpolation.next(p,target,Math.max(1,3-tick),0);
            assertTrue(next.distanceTo(p)<=1.000001);p=next;
        }
        assertEquals(target,p);
    }
    @Test void configuredPoweredSpeedRemainsAttainable() {
        var p=Vec3.ZERO;double speed=2.5;
        for(int tick=1;tick<=200;tick++)p=FlightInterpolation.next(p,new Vec3(tick*speed,0,0),3,speed);
        assertEquals(500,p.x,5.01);
    }
    @Test void ordinarySmallUpdatesKeepTheirInterpolation() {
        assertTrue(new Vec3(.2,.1,-.1).distanceTo(FlightInterpolation.next(Vec3.ZERO,new Vec3(.6,.3,-.3),3,.7))<1e-12);
    }
}
