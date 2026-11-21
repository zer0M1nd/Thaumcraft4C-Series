package thmodern.plane;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class PlaneShellTest {
 @Test void midpointSphereRetainsAllAxesAndReflectionSymmetry(){for(int radius:List.of(1,8,32)){var points=PlaneShell.points(radius);var set=new HashSet<>(points);assertEquals(points.size(),set.size());for(var point:points){assertTrue(Math.abs(point.x())<=radius&&Math.abs(point.y())<=radius&&Math.abs(point.z())<=radius);assertTrue(set.contains(new PlaneShell.Point(-point.x(),-point.y(),-point.z())));}for(var p:List.of(new PlaneShell.Point(radius,0,0),new PlaneShell.Point(0,radius,0),new PlaneShell.Point(0,0,radius)))assertTrue(set.contains(p));assertFalse(set.contains(new PlaneShell.Point(0,0,0)));}}
 @Test void drawingOrderIsStableAndInvalidRadiusIsRejected(){assertEquals(PlaneShell.points(16),PlaneShell.points(16));assertThrows(IllegalArgumentException.class,()->PlaneShell.points(0));assertThrows(IllegalArgumentException.class,()->PlaneShell.points(128));}
}
