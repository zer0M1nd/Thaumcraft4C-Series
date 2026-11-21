package thaumicomniverse.compat.create.airship;

import java.util.List;
import java.util.ArrayList;
import net.minecraft.world.phys.Vec3;

/** A connector is always one polynomial Bezier; extra degrees avoid cusps in hairpins. */
final class FlightManeuver {
    private FlightManeuver(){}
    static final double PREFERRED_LENGTH=20,MAXIMUM_LENGTH=40;
    static double defaultRadius(){return thaumicomniverse.RevivalFeatures.AIRSHIP_DEFAULT_BEND_RADIUS.get();}
    /** Finite compact search: never replace an impossible connector with a huge loop. */
    static FlightCurve compact(Vec3 start,Vec3 a,Vec3 end,Vec3 b,double radius,double maximum){
        a=a.normalize();b=b.normalize();
        if(a.lengthSqr()<.5||b.lengthSqr()<.5||start.distanceTo(end)>maximum||radius*FlightSteering.angle(a,b)>maximum)return null;
        var chord=end.subtract(start);double distance=chord.length();
        if(distance>1e-8&&a.dot(chord.normalize())>1-1e-9&&b.dot(chord.normalize())>1-1e-9)return FlightCurve.straight(start,end);
        FlightCurve best=null;double h=Math.max(.1,Math.max(radius,distance/3));
        for(double f:new double[]{.5,.75,1,1.4,2,3})for(double g:new double[]{.5,.75,1,1.4,2,3}){
            var c=new FlightCurve(start,start.add(a.scale(h*f)),end.subtract(b.scale(h*g)),end);
            if(c.length()<=maximum&&(best==null||c.length()<best.length())&&gentle(c,radius))best=c;
        }
        if(best!=null)return best;
        var side=a.cross(new Vec3(0,1,0)).normalize();if(side.lengthSqr()<.1)side=new Vec3(1,0,0);
        // Degree-11 polynomial fit of a three-arc return loop, normalized to
        // radius >= 1. Every transformed candidate is independently certified.
        if(a.dot(b)<-.9){
            double[][] loop={{.73856883036745,0},{3.3127689991933,.27064210539912},{-3.13326455318106,4.12662062718911},
                {8.78190510419545,.2088969743507},{2.74323001058838,3.04129424972098},{2.74323001058838,-3.04129424972098},
                {8.78190510419545,-.2088969743507},{-3.13326455318106,-4.12662062718911},{3.3127689991933,-.27064210539912},{.73856883036745,0}};
            for(double scale:new double[]{1,1.05,1.1,1.2,1.35,1.5}){
                var controls=new ArrayList<Vec3>();
                for(int i=1;i<=10;i++){
                    double smooth=3d*i*(i-1)/(11*10)-2d*i*(i-1)*(i-2)/(11*10*9);
                    double x=loop[i-1][0]*radius*scale,y=loop[i-1][1]*radius*scale;
                    controls.add(start.add(a.scale(x)).add(side.scale(y)).add(chord.scale(smooth)).add(b.scale(-1).subtract(a).scale(x*smooth)));
                }
                var c=new FlightCurve(start,controls,end);
                if(c.length()<=maximum&&(best==null||c.length()<best.length())&&gentle(c,radius))best=c;
            }
            if(best!=null)return best;
        }
        double[][] shapes={{1.65,1.1,3,6.5},{1.1,1.65,6.5,3},{2.7,1.4,.9,12},{1.4,2.7,12,.9},
            {3.3,1.5,.6,-12},{1.5,3.3,-12,.6},{2,2,3,3},{1,1,4,4}};
        for(double scale:new double[]{.65,1,1.35,1.8})for(var shape:shapes)for(int sign:new int[]{1,-1}){
            double h1=shape[0]*Math.max(.1,radius)*scale,h2=shape[1]*Math.max(.1,radius)*scale;
            var s1=side.scale(shape[2]*Math.max(.1,radius)*scale*sign);var s2=side.scale(shape[3]*Math.max(.1,radius)*scale*sign);
            var c=new FlightCurve(start,List.of(start.add(a.scale(h1)),start.add(a.scale(h1*2)),start.add(a.scale(h1*3)).add(s1),
                end.subtract(b.scale(h2*3)).add(s2),end.subtract(b.scale(h2*2)),end.subtract(b.scale(h2))),end);
            if(c.length()<=maximum&&(best==null||c.length()<best.length())&&gentle(c,radius))best=c;
        }
        return best;
    }
    record Departure(FlightCurve curve,double exit){}
    static Departure departure(Vec3 start,Vec3 heading,FlightCurve route,double requestedRadius){
        for(int attempt=0;attempt<24;attempt++){
            double radius=Math.max(.05,requestedRadius)*Math.pow(.8,attempt);FlightCurve best=null;double exit=0;
            for(double distance:new double[]{.5,1,2,3,4,6,9,12,16,20,26,32,38}){
                double at=Math.min(route.length(),distance);
                var candidate=compact(start,heading,route.position(at),route.tangent(at),radius,best==null?MAXIMUM_LENGTH:best.length());
                if(candidate!=null){best=candidate;exit=at;}
                if(best!=null&&best.length()<=PREFERRED_LENGTH)break;
                if(at==route.length())break;
            }
            if(best!=null)return new Departure(best,exit);
        }
        return null;
    }
    static boolean gentle(FlightCurve curve,double radius){
        if(radius<=0)return true;
        if(curve.length()<1e-8)return false;
        Vec3 previous=null;
        for(int i=0;i<=64;i++){
            double t=i/64d;var d=curve.derivative(t);double speed=d.length();
            if(speed<1e-7)return false;
            if(previous!=null&&previous.dot(d)<0)return false;previous=d;
            double curvature=d.cross(curve.secondDerivative(t)).length()/(speed*speed*speed);
            if(curvature>1/radius)return false;
        }
        var points=new ArrayList<Vec3>();points.add(curve.start);points.addAll(curve.controls);points.add(curve.end);
        var velocity=differentiate(points);
        return bounded(velocity,differentiate(velocity),radius,0);
    }
    private static List<Vec3> differentiate(List<Vec3> p){
        var d=new ArrayList<Vec3>();for(int i=1;i<p.size();i++)d.add(p.get(i).subtract(p.get(i-1)).scale(p.size()-1));return d;
    }
    // Bezier derivatives stay inside their control hulls. A projection gives a
    // lower speed bound; the pairwise cross products bound v x a everywhere.
    // Subdivision certifies the whole interval, including between sample points.
    private static boolean bounded(List<Vec3> v,List<Vec3> a,double radius,int depth){
        Vec3 axis=Vec3.ZERO;for(var p:v)axis=axis.add(p);axis=axis.normalize();
        double speed=Double.POSITIVE_INFINITY,cross=0;
        for(var p:v){speed=Math.min(speed,p.dot(axis));for(var q:a)cross=Math.max(cross,p.cross(q).length());}
        if(speed>1e-9&&cross*radius<=speed*speed*speed*(1-1e-12))return true;
        if(depth>=18)return false;
        var vl=new ArrayList<Vec3>();var vr=new ArrayList<Vec3>();split(v,vl,vr);
        var al=new ArrayList<Vec3>();var ar=new ArrayList<Vec3>();split(a,al,ar);
        return bounded(vl,al,radius,depth+1)&&bounded(vr,ar,radius,depth+1);
    }
    private static void split(List<Vec3> points,List<Vec3> left,List<Vec3> right){
        var p=new ArrayList<>(points);while(!p.isEmpty()){
            left.add(p.get(0));right.add(0,p.get(p.size()-1));
            for(int i=0;i<p.size()-1;i++)p.set(i,p.get(i).lerp(p.get(i+1),.5));p.remove(p.size()-1);
        }
    }
    static FlightCurve correct(FlightCurve edited,Vec3 incoming,Vec3 outgoing,double radius,double maximumLength){
        incoming=incoming.normalize();outgoing=outgoing.normalize();
        if(incoming.lengthSqr()<.5||outgoing.lengthSqr()<.5)throw new IllegalArgumentException("airship.error.manoeuvre");
        while(edited.controls.size()<2)edited=edited.elevated();
        var controls=new ArrayList<>(edited.controls);
        double h1=Math.max(.001,controls.get(0).distanceTo(edited.start));
        double h2=Math.max(.001,controls.get(controls.size()-1).distanceTo(edited.end));
        controls.set(0,edited.start.add(incoming.scale(h1)));
        controls.set(controls.size()-1,edited.end.subtract(outgoing.scale(h2)));
        var aligned=new FlightCurve(edited.start,controls,edited.end);
        if(aligned.length()<=maximumLength&&gentle(aligned,radius))return aligned;
        // Blend toward a certified connector, retaining as much of the edit as
        // possible. Degree elevation preserves both geometry and endpoint rays.
        var safe=Double.isFinite(maximumLength)?compact(edited.start,incoming,edited.end,outgoing,radius,maximumLength):connect(edited.start,incoming,edited.end,outgoing,radius);
        if(safe==null)throw new IllegalArgumentException("airship.error.manoeuvre");
        while(safe.controls.size()<aligned.controls.size())safe=safe.elevated();
        while(aligned.controls.size()<safe.controls.size())aligned=aligned.elevated();
        FlightCurve result=null;double lo=0,hi=1;
        for(int step=1;step<=32;step++){
            double amount=step/32d;var candidate=blend(aligned,safe,amount);
            if(candidate.length()<=maximumLength&&gentle(candidate,radius)){result=candidate;hi=amount;lo=(step-1)/32d;break;}
        }
        if(result==null)throw new IllegalArgumentException("airship.error.manoeuvre");
        for(int i=0;i<12;i++){double mid=(lo+hi)/2;var c=blend(aligned,safe,mid);
            if(c.length()<=maximumLength&&gentle(c,radius)){hi=mid;result=c;}else lo=mid;}
        return result;
    }
    private static FlightCurve blend(FlightCurve a,FlightCurve b,double amount){
        var controls=new ArrayList<Vec3>();for(int i=0;i<a.controls.size();i++)controls.add(a.controls.get(i).lerp(b.controls.get(i),amount));
        return new FlightCurve(a.start,controls,a.end);
    }
    static FlightCurve connect(Vec3 start,Vec3 a,Vec3 end,Vec3 b,double radius){
        a=a.normalize();b=b.normalize();double handle=Math.max(radius,start.distanceTo(end)/3);
        var chord=end.subtract(start);if(chord.lengthSqr()>1e-10&&a.dot(chord.normalize())>1-1e-10&&b.dot(chord.normalize())>1-1e-10)return FlightCurve.straight(start,end);
        double angle=Math.acos(Math.max(-1,Math.min(1,a.dot(b))));
        if(angle<Math.PI*.9){
            double circular=angle<1e-5?chord.length()/3:chord.length()*2/3*Math.tan(angle/4)/Math.sin(angle/2);
            for(double factor:new double[]{1,.95,1.05,.9,1.1}){
                var local=new FlightCurve(start,start.add(a.scale(circular*factor)),end.subtract(b.scale(circular*factor)),end);
                if(gentle(local,radius))return local;
            }
        }
        for(int i=0;i<5;i++){
            var direct=new FlightCurve(start,start.add(a.scale(handle)),end.subtract(b.scale(handle)),end);
            if(gentle(direct,radius))return direct;handle*=1.25;
        }
        // Search compact asymmetric control polygons before enlarging a symmetric loop.
        // A symmetric S can need hundreds of blocks to remove its central cusp even
        // when a short single Bezier with the same endpoint tangents exists.
        var side=a.cross(new Vec3(0,1,0)).normalize();if(side.lengthSqr()<.1)side=new Vec3(1,0,0);
        FlightCurve best=null;
        double[][] shapes={{1.65,1.1,3,6.5},{1.1,1.65,6.5,3},{2.7,1.4,.9,12},{1.4,2.7,12,.9},
            {3.3,1.5,.6,-12},{1.5,3.3,-12,.6},{2,2,3,3},{1,1,4,4}};
        for(double scale:new double[]{1,1.35,1.8,2.5,3.5})for(var shape:shapes)for(int sign:new int[]{1,-1}){
            double h1=shape[0]*radius*scale,h2=shape[1]*radius*scale;
            var s1=side.scale(shape[2]*radius*scale*sign);var s2=side.scale(shape[3]*radius*scale*sign);
            var candidate=new FlightCurve(start,List.of(start.add(a.scale(h1)),start.add(a.scale(h1*2)),start.add(a.scale(h1*3)).add(s1),
                end.subtract(b.scale(h2*3)).add(s2),end.subtract(b.scale(h2*2)),end.subtract(b.scale(h2))),end);
            if((best==null||candidate.length()<best.length())&&gentle(candidate,radius))best=candidate;
        }
        if(best!=null)return best;
        // Rare near-degenerate 3D endpoints still have a bounded search fallback.
        handle=Math.max(.5,Math.max(radius,start.distanceTo(end)/4));
        for(int i=0;i<24;i++){
            double h=handle;var offset=side.scale(h*3);
            var curve=new FlightCurve(start,List.of(start.add(a.scale(h)),start.add(a.scale(h*2)),start.add(a.scale(h*3)).add(offset),
                end.subtract(b.scale(h*3)).subtract(offset),end.subtract(b.scale(h*2)),end.subtract(b.scale(h))),end);
            if(gentle(curve,radius))return curve;handle*=1.5;
        }
        throw new IllegalArgumentException("airship.error.manoeuvre");
    }
}
