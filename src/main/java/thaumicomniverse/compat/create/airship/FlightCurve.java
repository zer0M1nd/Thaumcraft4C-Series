package thaumicomniverse.compat.create.airship;

import java.util.*;
import net.minecraft.world.phys.Vec3;

/** Editable Bezier route with a distance lookup table. Legacy cubics remain exact. */
public final class FlightCurve {
    private static final int SAMPLES=1024;
    public final Vec3 start, control1, control2, end;
    public final List<Vec3> controls;
    private final List<Vec3> points;
    private final List<Vec3> firstDerivative,secondDerivative;
    private final double[] distance=new double[SAMPLES+1];
    private FlightCurve reversed;
    private double minimumRadius=Double.NaN;

    public FlightCurve(Vec3 start,Vec3 control1,Vec3 control2,Vec3 end) {
        this(start,List.of(control1,control2),end);
    }
    public FlightCurve(Vec3 start,List<Vec3> controls,Vec3 end) {
        if(controls.size()>16)throw new IllegalArgumentException("airship.error.control_count");
        var all=new ArrayList<Vec3>();all.add(start);all.addAll(controls);all.add(end);
        for(var p:all)
            if(!finite(p))throw new IllegalArgumentException("airship.error.coordinates");
        this.start=start;this.end=end;this.controls=List.copyOf(controls);points=List.copyOf(all);
        firstDerivative=differentiate(points);secondDerivative=differentiate(firstDerivative);
        this.control1=controls.isEmpty()?start.lerp(end,1d/3):controls.get(0);
        this.control2=controls.isEmpty()?start.lerp(end,2d/3):controls.get(controls.size()-1);
        var previous=start;
        for(int i=1;i<=SAMPLES;i++){
            var next=at((double)i/SAMPLES);
            distance[i]=distance[i-1]+next.distanceTo(previous);previous=next;
        }
    }
    public static boolean finite(Vec3 p){return p!=null&&Double.isFinite(p.x)&&Double.isFinite(p.y)&&Double.isFinite(p.z);}
    public static FlightCurve straight(Vec3 start,Vec3 end){
        return new FlightCurve(start,start.lerp(end,1.0/3),start.lerp(end,2.0/3),end);
    }
    public Vec3 at(double t){
        return evaluate(points,Math.max(0,Math.min(1,t)));
    }
    public Vec3 derivative(double t){
        return evaluate(firstDerivative,t);
    }
    public Vec3 secondDerivative(double t){return evaluate(secondDerivative,t);}
    private static List<Vec3> differentiate(List<Vec3> points){var result=new ArrayList<Vec3>();int degree=points.size()-1;for(int i=0;i<degree;i++)result.add(points.get(i+1).subtract(points.get(i)).scale(degree));return result;}
    private static Vec3 evaluate(List<Vec3> points,double t){
        if(points.isEmpty())return Vec3.ZERO;
        var xyz=new double[points.size()*3];for(int i=0;i<points.size();i++){var p=points.get(i);xyz[i*3]=p.x;xyz[i*3+1]=p.y;xyz[i*3+2]=p.z;}
        for(int n=points.size()-1;n>0;n--)for(int i=0;i<n*3;i++)xyz[i]+=(xyz[i+3]-xyz[i])*t;
        return new Vec3(xyz[0],xyz[1],xyz[2]);
    }
    public double length(){return distance[SAMPLES];}
    /** Cached numerical display estimate, with local refinement of the largest sampled curvature. */
    public double minimumRadius(){
        if(!Double.isNaN(minimumRadius))return minimumRadius;
        if(length()<1e-9)return minimumRadius=0;
        double maximum=0;int peak=0;
        for(int i=0;i<=SAMPLES;i++){double c=curvature((double)i/SAMPLES);if(c>maximum){maximum=c;peak=i;}}
        if(Double.isInfinite(maximum))return minimumRadius=0;
        if(maximum<1e-12)return minimumRadius=Double.POSITIVE_INFINITY;
        double lo=Math.max(0,(peak-1d)/SAMPLES),hi=Math.min(1,(peak+1d)/SAMPLES);
        for(int i=0;i<24;i++){
            double a=lo+(hi-lo)/3,b=hi-(hi-lo)/3,ca=curvature(a),cb=curvature(b);
            maximum=Math.max(maximum,Math.max(ca,cb));if(ca<cb)lo=a;else hi=b;
        }
        return minimumRadius=1/maximum;
    }
    private double curvature(double t){
        var d=derivative(t);double speed=d.length();
        return speed<1e-10?Double.POSITIVE_INFINITY:d.cross(secondDerivative(t)).length()/(speed*speed*speed);
    }
    public double parameter(double blocks){
        if(!Double.isFinite(blocks))throw new IllegalArgumentException("airship.error.coordinates");
        if(blocks<=0||length()<1e-9)return 0;
        if(blocks>=length())return 1;
        int upper=Arrays.binarySearch(distance,blocks);
        if(upper>=0)return (double)upper/SAMPLES;
        upper=-upper-1;int lower=upper-1;
        double span=distance[upper]-distance[lower];
        return (lower+(span<1e-12?0:(blocks-distance[lower])/span))/SAMPLES;
    }
    public Vec3 position(double blocks){return at(parameter(blocks));}
    public Vec3 tangent(double blocks){
        double t=parameter(blocks);var v=derivative(t);
        if(v.lengthSqr()<1e-12)v=at(Math.min(1,t+.001)).subtract(at(Math.max(0,t-.001)));
        if(v.lengthSqr()<1e-12)v=end.subtract(start);
        return v.normalize();
    }
    public FlightCurve reverse(){if(reversed==null){var c=new ArrayList<>(controls);Collections.reverse(c);reversed=new FlightCurve(end,c,start);reversed.reversed=this;}return reversed;}
    /** Degree elevation adds an editable point without moving the curve. */
    public FlightCurve elevated(){
        if(controls.size()==16)throw new IllegalArgumentException("airship.error.control_count");
        int n=points.size()-1;var c=new ArrayList<Vec3>();
        for(int i=1;i<=n;i++)c.add(points.get(i).lerp(points.get(i-1),(double)i/(n+1)));
        return new FlightCurve(start,c,end);
    }
    /** C1-continuous local handoff. Caller reserves both route sections before committing it. */
    public static FlightCurve join(FlightCurve incoming,FlightCurve outgoing,double radius){
        double a=Math.min(radius,incoming.length()/3),b=Math.min(radius,outgoing.length()/3);
        Vec3 from=incoming.position(incoming.length()-a),to=outgoing.position(b);
        double handle=Math.max(.25,Math.min(a,b)*.75);
        return new FlightCurve(from,from.add(incoming.tangent(incoming.length()-a).scale(handle)),
            to.subtract(outgoing.tangent(b).scale(handle)),to);
    }
}
