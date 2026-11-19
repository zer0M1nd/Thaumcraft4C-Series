package thaumicomniverse.compat.create;

import java.util.List;
import net.minecraft.nbt.*;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArcaneGeometryTest {
    @Test void drillingDiskRemainsPerpendicularAtVerticalAndObliqueAngles() {
        for (Vec3 normal : List.of(new Vec3(0,1,0), new Vec3(0,-1,0), new Vec3(1,.37,-.6).normalize())) {
            var origin=new Vec3(3,19,-8);
            var rays=MovingBoreGeometry.rays(origin,normal,2);
            assertEquals(13,rays.size());
            for (var ray:rays) {
                assertEquals(32,ray.to().distanceTo(ray.from()),1e-8);
                assertEquals(1.5,ray.from().subtract(origin).dot(normal),1e-8);
                assertEquals(0,ray.to().subtract(ray.from()).normalize().distanceTo(normal),1e-8);
                assertTrue(ray.from().distanceTo(origin.add(normal.scale(1.5)))<=2.000001);
            }
        }
        assertThrows(IllegalArgumentException.class,()->MovingBoreGeometry.rays(Vec3.ZERO,Vec3.ZERO,1));
    }
    @Test void flightSamplesByDistanceAndToleratesRepeatedPoints() {
        var route=List.of(Vec3.ZERO,Vec3.ZERO,new Vec3(0,0,2),new Vec3(8,0,2));
        assertEquals(10,ArcaneFlightPath.length(route),1e-8);
        assertEquals(new Vec3(3,0,2),ArcaneFlightPath.sample(route,.5));
        assertEquals(route.get(0),ArcaneFlightPath.sample(route,-1));
        assertEquals(route.get(3),ArcaneFlightPath.sample(route,2));
        assertEquals(route,ArcaneFlightPath.load(ArcaneFlightPath.save(route)));
    }
    @Test void allEasingModesStartSlowAndEndSlowWithoutOvershooting() {
        for (int mode=0;mode<3;mode++) {
            double before=-1;
            for (int step=0;step<=1000;step++) {
                double t=step/1000d,now=ArcaneFlightPath.ease(t,mode);
                assertTrue(now>=before&&now>=0&&now<=1);
                assertEquals(1,now+ArcaneFlightPath.ease(1-t,mode),1e-10);
                before=now;
            }
            assertTrue(ArcaneFlightPath.ease(.01,mode)<.001);
            assertTrue(ArcaneFlightPath.ease(.99,mode)>.999);
        }
    }
    @Test void savedRoutesRejectNonFiniteAndOutOfWorldCoordinates() {
        var saved=ArcaneFlightPath.save(List.of(new Vec3(0,100,0),new Vec3(Double.NaN,1,1),new Vec3(1,Double.POSITIVE_INFINITY,1),new Vec3(30000000,1,1)));
        assertEquals(List.of(new Vec3(0,100,0)),ArcaneFlightPath.load(saved));
    }
    @Test void recordedZigzagsBecomeOneCubicWithFixedEndpoints() {
        var input=new java.util.ArrayList<Vec3>();
        for(int i=0;i<=20;i++)input.add(new Vec3(i,100+(i==0||i==20?0:i%2==0?2:-2),i*.4));
        var fit=ArcaneFlightPath.fit(input);
        assertEquals(input.get(0),fit.get(0));assertEquals(input.get(20),fit.get(fit.size()-1));
        for(int i=0;i+4<fit.size();i++){
            var fourth=fit.get(i).subtract(fit.get(i+1).scale(4)).add(fit.get(i+2).scale(6)).subtract(fit.get(i+3).scale(4)).add(fit.get(i+4));
            assertTrue(fourth.length()<1e-10,"The fitted path must be at most cubic");
        }
        assertTrue(ArcaneFlightPath.length(fit)<ArcaneFlightPath.length(input)*.6);
        assertEquals(List.of(Vec3.ZERO,Vec3.ZERO),ArcaneFlightPath.fit(List.of(Vec3.ZERO,Vec3.ZERO,Vec3.ZERO)));
    }
    @Test void fuelMultiplierScalesRoundedOriginalTicks() {
        for(int energy:thaumicomniverse.compat.gregtech.EssentiaFuelValues.ENERGY.values()){
            int original=Math.max(1,(int)Math.round(energy*8d/4096));
            assertEquals(original*4,thaumicomniverse.compat.gregtech.EssentiaFuelValues.ticks(energy,4));
            assertEquals(original*7,thaumicomniverse.compat.gregtech.EssentiaFuelValues.ticks(energy,7));
        }
    }
}
