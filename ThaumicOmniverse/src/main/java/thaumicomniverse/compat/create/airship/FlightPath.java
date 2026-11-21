package thaumicomniverse.compat.create.airship;

import java.util.List;
import net.minecraft.world.phys.Vec3;

/** Short, distance-parametrized manoeuvres used only at a route junction or for a departure turn. */
public final class FlightPath {
    public final List<FlightCurve> curves;
    private final double length;
    public FlightPath(List<FlightCurve> curves){
        if(curves.isEmpty()||curves.size()>12)throw new IllegalArgumentException("airship.error.manoeuvre");
        this.curves=List.copyOf(curves);length=curves.stream().mapToDouble(FlightCurve::length).sum();
        for(int i=1;i<curves.size();i++)if(curves.get(i-1).end.distanceToSqr(curves.get(i).start)>1e-8)
            throw new IllegalArgumentException("airship.error.manoeuvre");
    }
    public double length(){return length;}
    public Vec3 position(double distance){
        for(var c:curves){if(distance<=c.length())return c.position(Math.max(0,distance));distance-=c.length();}
        return curves.get(curves.size()-1).end;
    }
    public Vec3 tangent(double distance){
        for(var c:curves){if(distance<=c.length())return c.tangent(Math.max(0,distance));distance-=c.length();}
        var c=curves.get(curves.size()-1);return c.tangent(c.length());
    }
    public static FlightPath transition(Vec3 start,Vec3 startTangent,Vec3 end,Vec3 endTangent,double radius){
        return new FlightPath(List.of(FlightManeuver.connect(start,startTangent,end,endTangent,radius)));
    }
}
