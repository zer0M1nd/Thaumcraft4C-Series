package thaumicomniverse.compat.create.airship;

import java.util.*;
import net.minecraft.world.phys.Vec3;

/** Off-route recovery uses a bounded, tangent-matched Bezier to a nearby route. */
public final class FlightReattachment {
    public record Entry(FlightGraph.Route route,UUID from,double distance,Vec3 point){}
    public static boolean detached(FlightState s){return s.atNode==null&&s.route==null&&s.transition==null;}
    public static void detach(FlightGraph graph,FlightState s,String reason){
        graph.release(s.id);s.atNode=s.route=s.fromNode=s.selectedExit=s.plannedExit=null;
        s.incomingRoute=s.choiceNode=s.transitionFromRoute=s.transitionToRoute=s.transitionToNode=null;
        s.portalExitNode=null;s.transition=null;s.transitionDistance=s.transitionEndDistance=0;
        s.alignmentTicks=0;s.aligned=s.selectionLocked=s.rejoining=s.rejoinAutomatic=false;
        s.awaitingThrottleRelease=s.dockSelected=s.reverseTravel=false;s.previousChoice=0;
        s.speed=0;s.paused=false;s.automatic=false;s.message=reason;
        s.activeBend=null;s.rotateAtJoin=s.pivotPass=false;s.motionDirection=null;s.dockDistanceTotal=0;
        s.rejoinRetryTicks=0;
    }
    public static Entry nearest(FlightGraph graph,FlightState s){
        Entry best=null;double bestDistance=Double.POSITIVE_INFINITY;
        for(var route:graph.routes()){
            var from=graph.node(route.from());if(from==null||!s.dimension.equals(from.dimension()))continue;
            var curve=route.curve();double distance=nearestDistance(curve,s.position);
            var point=curve.position(distance);double squared=point.distanceToSqr(s.position);
            if(squared>bestDistance+1e-8||Math.abs(squared-bestDistance)<=1e-8&&best!=null&&route.sequence()>best.route.sequence())continue;
            boolean reverse=FlightNavigation.forward(s.yaw).dot(curve.tangent(distance))<0;
            best=new Entry(route,reverse?route.to():route.from(),reverse?curve.length()-distance:distance,point);bestDistance=squared;
        }return best;
    }
    public static double nearestDistance(FlightCurve curve,Vec3 position){
        // Search every LUT interval, then refine around the closest sample. Distances
        // rather than Bezier parameters keep the tolerance uniform on long routes.
        double step=curve.length()/1024,best=0,squared=Double.POSITIVE_INFINITY;
        for(int i=0;i<=1024;i++){double d=i*step,score=curve.position(d).distanceToSqr(position);if(score<squared){squared=score;best=d;}}
        double lo=Math.max(0,best-step),hi=Math.min(curve.length(),best+step);
        for(int i=0;i<32;i++){double a=lo+(hi-lo)/3,b=hi-(hi-lo)/3;if(curve.position(a).distanceToSqr(position)<curve.position(b).distanceToSqr(position))hi=b;else lo=a;}
        return (lo+hi)/2;
    }
    public static boolean begin(FlightGraph graph,FlightState s,boolean automatic){
        for(int attempt=0;attempt<24;attempt++)if(begin(graph,s,automatic,Math.pow(.8,attempt)))return true;
        return false;
    }
    private static boolean begin(FlightGraph graph,FlightState s,boolean automatic,double radiusScale){
        var nearby=new ArrayList<Entry>();var heading=FlightNavigation.forward(s.yaw);
        for(var route:graph.routes()){
            var from=graph.node(route.from());if(from==null||!s.dimension.equals(from.dimension()))continue;
            double d=nearestDistance(route.curve(),s.position);var p=route.curve().position(d);
            if(p.distanceTo(s.position)>FlightManeuver.MAXIMUM_LENGTH)continue;
            boolean reverse=heading.dot(route.curve().tangent(d))<0;
            nearby.add(new Entry(route,reverse?route.to():route.from(),reverse?route.curve().length()-d:d,p));
        }
        nearby.sort(Comparator.<Entry>comparingDouble(e->e.point.distanceToSqr(s.position)).thenComparingInt(e->e.route.sequence()));
        Entry entry=null;FlightCurve best=null;boolean occupied=false;
        for(var near:nearby.stream().limit(8).toList()){
            var destination=graph.node(near.route.other(near.from));
            if(graph.routeOwner(near.route.id())!=null&&!s.id.equals(graph.routeOwner(near.route.id()))
                ||graph.stationOwner(destination.id())!=null&&!s.id.equals(graph.stationOwner(destination.id()))){occupied=true;continue;}
            var curve=near.route.leaving(near.from);double radius=FlightManeuver.defaultRadius()*radiusScale;
            for(double offset:new double[]{0,3,6,9,12,18,24,30,-3,-6,-12}){
                double distance=Math.max(0,Math.min(curve.length(),near.distance+offset));var point=curve.position(distance);
                var candidate=FlightManeuver.compact(s.position,heading,point,curve.tangent(distance),radius,best==null?FlightManeuver.MAXIMUM_LENGTH:best.length());
                if(candidate!=null){best=candidate;entry=new Entry(near.route,near.from,distance,point);}
                if(best!=null&&best.length()<=FlightManeuver.PREFERRED_LENGTH)break;
            }
            if(best!=null&&best.length()<=FlightManeuver.PREFERRED_LENGTH)break;
        }
        if(entry==null){s.message=occupied?"airship.wait.signal":nearby.isEmpty()?"airship.rejoin.no_route":"airship.rejoin.no_curve";return false;}
        if(!FlightNavigation.reserve(graph,s,entry.route,entry.from)){s.message="airship.wait.signal";return false;}
        s.transition=new FlightPath(List.of(best));s.motionDirection=heading;
        s.transitionDistance=0;s.transitionEndDistance=entry.distance;s.transitionToRoute=entry.route.id();s.transitionToNode=entry.from;
        s.transitionFromRoute=null;s.selectionLocked=true;s.rejoining=true;s.rejoinAutomatic=automatic;s.reverseTravel=false;
        s.message="airship.rejoin.returning";return true;
    }
    public static boolean invalid(FlightGraph graph,FlightState s){
        if(s.atNode!=null&&graph.node(s.atNode)==null)return true;
        for(var id:new UUID[]{s.route,s.transitionFromRoute,s.transitionToRoute})if(id!=null&&graph.route(id)==null)return true;
        return false;
    }
    private FlightReattachment(){}
}
