package thaumicomniverse.compat.create.airship;

import java.util.*;
import java.util.function.DoubleFunction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Server-owned motion, connector reservations and three-dimensional angular budget. */
public final class FlightNavigation {
    public static final double ACCELERATION=3d/1600;
    public static final double JUNCTION_RADIUS=6;
    private FlightNavigation(){}
    public static void clearInput(FlightState s){s.forward=s.reverse=s.dockHeld=s.leftHeld=s.rightHeld=s.downHeld=false;s.previousChoice=s.modeGestureTicks=0;s.modeGestureLatched=false;}
    public static void manualInput(FlightState s,boolean forward,boolean reverse,boolean dock){
        if(!forward&&!reverse)s.awaitingThrottleRelease=false;
        s.forward=forward&&!s.awaitingThrottleRelease;s.reverse=reverse&&!s.awaitingThrottleRelease;s.dockHeld=dock;
    }
    private static double approach(FlightState s,double target){return s.speed<target?Math.min(target,s.speed+s.acceleration):Math.max(target,s.speed-s.acceleration);}
    public static Vec3 forward(float yaw){double angle=Math.toRadians(yaw);return new Vec3(-Math.sin(angle),0,Math.cos(angle));}
    public static void face(FlightState s,Vec3 tangent){
        if(tangent.horizontalDistanceSqr()>1e-9)s.yaw+=(float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(-tangent.x,tangent.z))-s.yaw);
        s.pitch=0;
    }
    private static boolean orient(FlightState s,Vec3 direction,double budget){
        if(direction.lengthSqr()<1e-10)return true;
        if(s.motionDirection==null)s.motionDirection=forward(s.yaw);
        double angle=FlightSteering.angle(s.motionDirection,direction);
        if(angle>1e-6)s.aligned=false;
        s.motionDirection=FlightSteering.rotate(s.motionDirection,direction,budget);face(s,s.motionDirection);
        if(angle>budget+1e-8){s.speed=0;return false;}return true;
    }
    private static double step(FlightState s,DoubleFunction<Vec3> tangent,double at,double desired,double budget){
        Vec3 start=tangent.apply(at);
        if(s.motionDirection==null)s.motionDirection=forward(s.yaw);
        double initial=FlightSteering.angle(s.motionDirection,start);
        if(initial>1e-5){orient(s,start,budget);s.speed=0;return 0;}
        double moved=FlightSteering.advance(tangent,at,desired,budget);
        s.motionDirection=tangent.apply(at+moved);face(s,s.motionDirection);s.speed=moved;return moved;
    }
    public static UUID approaching(FlightGraph graph,FlightState s){
        if(s.atNode!=null)return s.atNode;
        if(s.transition!=null){var exit=graph.route(s.transitionToRoute);return exit==null||s.transitionToNode==null?null:exit.other(s.transitionToNode);}
        var route=graph.route(s.route);return route==null||s.fromNode==null?null:route.other(s.fromNode);
    }
    public static List<FlightGraph.Route> choices(FlightGraph graph,FlightState s){UUID at=approaching(graph,s);return at==null?List.of():graph.exits(at,null);}
    public static boolean choiceAvailable(FlightGraph graph,FlightState s,UUID outgoing){
        UUID at=approaching(graph,s);if(at==null)return false;
        var selected=graph.route(outgoing);
        if(selected==null||!selected.from().equals(at)&&!selected.to().equals(at))return false;
        if(s.atNode!=null)return true;
        UUID incoming=s.transition!=null?s.transitionToRoute:s.route;
        var route=graph.route(incoming);if(route==null)return false;
        var bend=graph.bend(at,incoming,outgoing,true);
        double distance=s.transition!=null?s.transitionEndDistance:s.distance;
        return bend==null||bend.pivot()||distance<=route.curve().length()-bend.entry()+1e-6;
    }
    public static boolean canEnter(FlightGraph graph,FlightGraph.Node node){
        return node!=null&&(node.station()||graph.portal(node.id())||graph.exits(node.id(),null).size()==1);
    }
    public static boolean dockingOption(FlightGraph graph,FlightState s){return canEnter(graph,graph.node(approaching(graph,s)))&&s.atNode==null&&!s.freeControl;}
    public static void prepareChoice(FlightGraph graph,FlightState s){
        UUID at=approaching(graph,s);if(at==null)return;
        if(Objects.equals(at,s.choiceNode)){
            if(!dockingOption(graph,s))s.dockSelected=false;
            var selected=graph.route(s.selectedExit);
            if(s.dockSelected||selected!=null&&(selected.from().equals(at)||selected.to().equals(at)))return;
        }
        s.choiceNode=at;s.selectedExit=null;s.dockSelected=false;s.dockDistanceTotal=0;
        if(!s.automatic&&dockingOption(graph,s)){s.dockSelected=true;return;}
        UUID incoming=s.atNode!=null?null:s.transition!=null?s.transitionToRoute:s.route;
        var arriving=graph.route(incoming);
        Vec3 direction=arriving==null||!arriving.from().equals(at)&&!arriving.to().equals(at)?forward(s.yaw):arriving.leaving(at).tangent(0).scale(-1);
        s.selectedExit=choices(graph,s).stream().filter(r->choiceAvailable(graph,s,r.id()))
            .max(Comparator.<FlightGraph.Route>comparingDouble(r->direction.dot(r.leaving(at).tangent(0)))
            .thenComparing(Comparator.comparingInt(FlightGraph.Route::sequence).reversed())).map(FlightGraph.Route::id).orElse(null);
    }
    public static void choose(FlightGraph graph,FlightState s,int step){
        if(s.selectionLocked||step==0||s.freeControl)return;prepareChoice(graph,s);
        var candidates=choices(graph,s).stream().filter(r->choiceAvailable(graph,s,r.id())).toList();boolean dock=dockingOption(graph,s);int offset=dock?1:0;
        int count=candidates.size()+offset;if(count==0)return;int index=s.dockSelected&&dock?0:-1;
        for(int i=0;i<candidates.size();i++)if(candidates.get(i).id().equals(s.selectedExit))index=i+offset;
        index=Math.floorMod(index+step,count);s.dockSelected=dock&&index==0;
        s.selectedExit=s.dockSelected?null:candidates.get(index-offset).id();s.dockDistanceTotal=0;
        if(s.atNode!=null){s.plannedExit=null;s.activeBend=null;}
    }
    static boolean reserve(FlightGraph g,FlightState s,FlightGraph.Route route,UUID from){
        var destination=g.node(route.other(from));
        if(destination==null||g.routeOwner(route.id())!=null&&!s.id.equals(g.routeOwner(route.id())))return false;
        if(g.stationOwner(destination.id())!=null&&!s.id.equals(g.stationOwner(destination.id())))return false;
        if(!g.reserveRoute(route.id(),s.id))return false;
        if(!g.reserveStation(destination.id(),s.id)){g.releaseRoute(route.id(),s.id);return false;}return true;
    }
    public static FlightBend departureBend(FlightGraph g,FlightState s,UUID outgoing){
        if(s.atNode==null||outgoing==null)return null;
        if(outgoing.equals(s.plannedExit))return g.bend(s.activeBend);
        var target=g.route(outgoing);if(target==null)return null;Vec3 heading=forward(s.yaw);
        if(heading.dot(target.leaving(s.atNode).tangent(0))>.999)return null;
        var arrival=g.exits(s.atNode,null).stream().filter(r->heading.dot(r.leaving(s.atNode).tangent(0).scale(-1))>.999)
            .min(Comparator.comparingInt(r->Objects.equals(r.id(),s.incomingRoute)?0:1)).orElse(null);
        return arrival==null?null:g.bend(s.atNode,arrival.id(),outgoing,false);
    }
    private static void startTransition(FlightState s,FlightBend bend,UUID from,UUID to,UUID node){
        s.transition=new FlightPath(List.of(bend.curve()));s.activeBend=bend.id();
        s.transitionDistance=0;s.transitionEndDistance=bend.exit();s.transitionFromRoute=from;s.transitionToRoute=to;s.transitionToNode=node;
        s.atNode=null;s.aligned=false;s.selectionLocked=true;s.message="";
    }
    public static boolean depart(FlightGraph g,FlightState s){return depart(g,s,Math.toRadians(10)/20);}
    private static boolean depart(FlightGraph g,FlightState s,double budget){
        prepareChoice(g,s);var route=g.route(s.selectedExit);var at=g.node(s.atNode);
        if(at==null||route==null){s.message="airship.error.no_exit";return false;}
        if(!route.id().equals(s.plannedExit)){var chosen=s.pivotPass?null:departureBend(g,s,route.id());s.activeBend=chosen==null?null:chosen.id();s.plannedExit=route.id();}
        var bend=g.bend(s.activeBend);
        var direction=bend!=null&&!bend.pivot()?bend.curve().tangent(0):route.leaving(at.id()).tangent(0);
        var heading=forward(s.yaw);FlightManeuver.Departure temporary=null;
        if(FlightSteering.angle(heading,direction)>1e-5){
            if(s.rejoinRetryTicks-->0)return false;
            temporary=FlightManeuver.departure(s.position,heading,route.leaving(at.id()),FlightManeuver.defaultRadius());
            if(temporary==null){s.rejoinRetryTicks=20;s.message="airship.rejoin.no_curve";return false;}
        }
        if(!reserve(g,s,route,at.id())){s.message="airship.wait.signal";return false;}
        s.pivotPass=false;s.dockDistanceTotal=0;s.plannedExit=null;s.rejoinRetryTicks=0;s.motionDirection=heading;
        if(temporary!=null){
            s.transition=new FlightPath(List.of(temporary.curve()));s.activeBend=null;s.motionDirection=heading;
            s.transitionDistance=0;s.transitionEndDistance=temporary.exit();s.transitionFromRoute=null;s.transitionToRoute=route.id();s.transitionToNode=at.id();
            s.route=s.fromNode=s.atNode=null;s.aligned=false;s.selectionLocked=true;s.choiceNode=null;
        }else if(bend!=null&&!bend.pivot())startTransition(s,bend,null,route.id(),at.id());
        else{s.route=route.id();s.fromNode=at.id();s.distance=0;s.atNode=null;s.aligned=false;s.choiceNode=null;g.releaseStation(at.id(),s.id);}
        s.message="";return true;
    }
    public static double remaining(FlightGraph g,FlightState s){
        if(s.route==null||s.fromNode==null)return Double.POSITIVE_INFINITY;var r=g.route(s.route);
        return r==null?Double.POSITIVE_INFINITY:Math.max(0,r.curve().length()-s.distance);
    }
    public static boolean canApproach(FlightGraph g,FlightState s){
        if(!s.dockSelected||s.freeControl||s.rejoining||s.transition!=null||s.atNode!=null)return false;
        var n=g.node(approaching(g,s));if(!canEnter(g,n))return false;
        double brake=s.speed*s.speed/(2*Math.max(1e-8,s.acceleration)),distance=remaining(g,s);
        return s.distance>.75&&distance<=Math.max(32,1.5*brake);
    }
    public static void tick(FlightGraph g,FlightState s,double max){tick(g,s,max,10);}
    public static void tick(FlightGraph g,FlightState s,double max,double degreesPerSecond){
        double budget=Math.toRadians(degreesPerSecond)/20;
        if(s.leftHeld&&s.rightHeld){
            if(!s.modeGestureLatched&&++s.modeGestureTicks>=40){
                boolean next=!s.freeControl;FlightReattachment.detach(g,s,next?"airship.mode.free":"airship.mode.route");
                s.freeControl=next;s.requireSpaceReturn=!next;s.modeGestureLatched=true;s.modeGestureTicks=40;
            }
        }else{s.modeGestureTicks=0;s.modeGestureLatched=false;}
        if(s.freeControl){free(s,max,budget);return;}
        if(FlightReattachment.invalid(g,s))FlightReattachment.detach(g,s,"airship.rejoin.detached");
        if(s.paused){s.speed=0;return;}
        if(FlightReattachment.detached(s)){
            s.speed=0;if(s.dockHeld||s.forward&&!s.requireSpaceReturn){if(s.rejoinRetryTicks--<=0){s.rejoinRetryTicks=20;FlightReattachment.begin(g,s,s.forward&&!s.requireSpaceReturn);}}
            else s.message="airship.rejoin.detached";return;
        }
        if(s.alignmentTicks>0){
            s.speed=0;s.pitch=0;double delta=Mth.wrapDegrees(s.alignmentTargetYaw-s.yaw),step=degreesPerSecond/20;
            s.yaw+=(float)Math.copySign(Math.min(Math.abs(delta),step),delta);s.motionDirection=forward(s.yaw);
            if(Math.abs(delta)<=step+1e-6){s.yaw=s.alignmentTargetYaw;s.motionDirection=forward(s.yaw);s.alignmentTicks=0;s.aligned=true;}return;
        }
        prepareChoice(g,s);
        boolean braking=!s.automatic&&(s.reverseTravel?s.forward:s.reverse);
        if(s.atNode!=null){
            s.speed=0;if(g.portal(s.atNode)&&s.dockSelected&&!s.atNode.equals(s.portalExitNode))return;
            if(s.awaitingThrottleRelease&&!s.automatic)return;
            if(s.forward||s.reverse||s.pivotPass){if(depart(g,s,budget))s.reverseTravel=!s.automatic&&s.reverse&&!s.forward;}return;
        }
        boolean driving=s.automatic?s.forward:(s.reverseTravel?s.reverse:s.forward);
        if(s.transition!=null){
            if(s.rejoining&&braking)s.rejoinAutomatic=false;driving|=s.rejoining&&(s.dockHeld||s.rejoinAutomatic);
            double target=driving&&!braking?max*s.throttle:0,distance=s.transition.length()-s.transitionDistance;
            target=plannedSpeed(s,s.transition::tangent,s.transitionDistance,s.transition.length(),target,budget,false);
            double velocity=approach(s,target),wanted=Math.min(distance,velocity);
            double spent=turn(s.transition::tangent,s.transitionDistance,wanted);
            double moved=step(s,s.transition::tangent,s.transitionDistance,wanted,budget);
            s.speed=moved;s.transitionDistance+=moved;s.position=s.transition.position(s.transitionDistance);
            if(s.transitionDistance>=s.transition.length()-1e-8){
                finishTransition(g,s);
                s.speed=moved+advanceSeam(g,s,Math.max(0,velocity-moved),Math.max(0,budget-spent));
            }return;
        }
        var route=g.route(s.route);if(route==null||s.fromNode==null){FlightReattachment.detach(g,s,"airship.rejoin.detached");return;}
        var curve=route.leaving(s.fromNode);UUID destination=route.other(s.fromNode);var node=g.node(destination);
        if(node==null){FlightReattachment.detach(g,s,"airship.rejoin.detached");return;}
        if(!s.id.equals(g.routeOwner(route.id()))){s.stop("airship.wait.signal");s.paused=true;return;}
        if(braking&&s.speed<1e-5){
            if(!g.reserveStation(s.fromNode,s.id)){s.message="airship.wait.signal";return;}
            g.releaseStation(destination,s.id);s.fromNode=destination;s.distance=curve.length()-s.distance;s.reverseTravel=!s.reverseTravel;
            s.choiceNode=null;s.dockDistanceTotal=0;s.speed=0;return;
        }
        if(!s.automatic){
            if(s.dockHeld&&s.dockSelected&&(canApproach(g,s)||s.dockDistanceTotal>0)){if(s.dockDistanceTotal==0)s.dockDistanceTotal=remaining(g,s);}
            else s.dockDistanceTotal=0;
        }
        driving|=s.dockHeld&&s.dockSelected&&s.dockDistanceTotal>0;
        boolean portal=g.portal(destination);var next=s.dockSelected||!choiceAvailable(g,s,s.selectedExit)?null:g.route(s.selectedExit);
        var bend=next==null?null:g.bend(destination,route.id(),next.id(),true);
        boolean direct=next!=null&&!FlightBend.needed(node,route,next);
        var onward=next==null?null:next.leaving(destination);
        boolean stop=next==null;double limit=stop||bend==null||bend.pivot()?curve.length():curve.length()-bend.entry();
        // A choice made after its merge point must not teleport the ship backwards.
        // Continue to the anchor and turn there before taking the late-selected route.
        if(s.distance>limit+1e-6){bend=null;limit=curve.length();}
        boolean available=next!=null&&(g.routeOwner(next.id())==null||s.id.equals(g.routeOwner(next.id())));
        if(available){var dest=g.node(next.other(destination));available=dest!=null&&(g.stationOwner(dest.id())==null||s.id.equals(g.stationOwner(dest.id())));}
        double target=driving&&!braking?max*s.throttle:0;
        boolean stopping=stop||!available||!direct&&(bend==null||bend.pivot());
        var connector=bend;double boundary=limit;
        DoubleFunction<Vec3> future=d->d<=boundary?curve.tangent(d):direct?onward.tangent(d-boundary):connector==null||connector.pivot()?curve.tangent(d):connector.curve().tangent(d-boundary);
        target=plannedSpeed(s,future,s.distance,stopping?limit:limit+(direct?onward.length():bend.curve().length()),target,budget,stopping);
        double velocity=approach(s,target),wanted=Math.min(Math.max(0,limit-s.distance),velocity);
        double spent=turn(curve::tangent,s.distance,wanted);
        double moved=step(s,curve::tangent,s.distance,wanted,budget);s.distance+=moved;s.position=curve.position(s.distance);
        if(s.distance>=limit-1e-7){
            if(!stop){
                if(!reserve(g,s,next,destination)){s.speed=0;s.message="airship.wait.signal";return;}
                if(direct){
                    if(!route.id().equals(next.id()))g.releaseRoute(route.id(),s.id);g.releaseStation(destination,s.id);
                    s.route=next.id();s.fromNode=destination;s.distance=0;s.incomingRoute=route.id();s.choiceNode=s.activeBend=s.plannedExit=s.portalExitNode=null;
                    s.motionDirection=onward.tangent(0);s.aligned=false;s.message="";
                    s.speed=moved+advanceSeam(g,s,Math.max(0,velocity-moved),Math.max(0,budget-spent));return;
                }
                if(bend!=null&&!bend.pivot()){startTransition(s,bend,route.id(),next.id(),destination);s.speed=moved+advanceSeam(g,s,Math.max(0,velocity-moved),Math.max(0,budget-spent));return;}
            }
            s.position=node.point();s.atNode=destination;s.portalExitNode=null;s.incomingRoute=route.id();s.route=null;s.fromNode=null;s.choiceNode=null;s.plannedExit=null;s.activeBend=null;s.speed=0;s.aligned=false;
            g.releaseRoute(route.id(),s.id);s.pivotPass=!stop||!node.station()&&!(s.dockSelected&&canEnter(g,node));
            if(s.pivotPass){s.choiceNode=next==null?null:destination;s.selectedExit=next==null?null:next.id();s.awaitingThrottleRelease=false;s.message="airship.turning";}
            else{s.message=node.station()?"airship.docked":"airship.waypoint.entered";s.awaitingThrottleRelease=!s.automatic;s.forward=s.reverse=false;}
        }else s.message=!available&&!stop?"airship.wait.signal":"";
    }
    private static void finishTransition(FlightGraph g,FlightState s){
        if(s.transitionFromRoute!=null&&!s.transitionFromRoute.equals(s.transitionToRoute))g.releaseRoute(s.transitionFromRoute,s.id);
        g.releaseStation(s.transitionToNode,s.id);s.route=s.transitionToRoute;s.fromNode=s.transitionToNode;s.distance=s.transitionEndDistance;
        s.transition=null;s.activeBend=null;s.transitionFromRoute=s.transitionToRoute=s.transitionToNode=null;s.selectionLocked=false;s.choiceNode=null;
        s.rejoining=s.rejoinAutomatic=s.rotateAtJoin=s.requireSpaceReturn=false;s.message="";
    }
    /** Spend this tick's remaining travel across a tangent-matched seam instead of braking for a LUT endpoint. */
    private static double advanceSeam(FlightGraph g,FlightState s,double remaining,double budget){
        if(remaining<1e-9)return 0;
        if(s.transition!=null){
            double moved=step(s,s.transition::tangent,s.transitionDistance,Math.min(remaining,s.transition.length()-s.transitionDistance),budget);
            s.transitionDistance+=moved;s.position=s.transition.position(s.transitionDistance);
            if(s.transitionDistance>=s.transition.length()-1e-8)finishTransition(g,s);
            return moved;
        }
        var route=g.route(s.route);if(route==null||s.fromNode==null)return 0;
        prepareChoice(g,s);var curve=route.leaving(s.fromNode);double limit=curve.length();
        var bend=s.selectedExit==null?null:g.bend(route.other(s.fromNode),route.id(),s.selectedExit,true);
        if(bend!=null&&!bend.pivot())limit=Math.max(s.distance,limit-bend.entry());
        double moved=step(s,curve::tangent,s.distance,Math.min(remaining,Math.max(0,limit-s.distance)),budget);
        s.distance+=moved;s.position=curve.position(s.distance);return moved;
    }
    private static double turn(DoubleFunction<Vec3> tangent,double start,double distance){
        double angle=0;var previous=tangent.apply(start);
        for(int i=1;i<=8;i++){var next=tangent.apply(start+distance*i/8);angle+=FlightSteering.angle(previous,next);previous=next;}
        return angle;
    }
    private static void free(FlightState s,double max,double budget){
        s.yaw+=(float)Math.toDegrees(budget*((s.rightHeld?1:0)-(s.leftHeld?1:0)));
        Vec3 desired=forward(s.yaw).scale((s.forward?1:0)-(s.reverse?1:0)).add(0,(s.dockHeld?1:0)-(s.downHeld?1:0),0);
        boolean reversing=desired.lengthSqr()>0&&s.motionDirection!=null&&s.motionDirection.dot(desired.normalize())<-.9;
        s.speed=approach(s,desired.lengthSqr()>0&&!reversing?max*s.throttle:0);
        if(desired.lengthSqr()>0){
            if(s.motionDirection==null||s.speed<=s.acceleration)s.motionDirection=desired.normalize();
            else s.motionDirection=FlightSteering.rotate(s.motionDirection,desired,budget);
        }
        if(s.motionDirection!=null)s.position=s.position.add(s.motionDirection.scale(s.speed));s.message="airship.mode.free";
    }
    /** Anticipate turn speed and stopping distance; signal changes still take safety precedence. */
    private static double plannedSpeed(FlightState s,DoubleFunction<Vec3> tangent,double at,double end,double target,double budget,boolean stop){
        double a=Math.max(1e-8,s.acceleration),remaining=Math.max(0,end-at);
        if(stop)target=Math.min(target,Math.max(0,Math.sqrt(a*a+2*a*remaining)-a));
        double horizon=Math.min(remaining,Math.max(s.speed,target)*Math.max(s.speed,target)/(2*a)+2);
        int samples=Math.max(1,Math.min(128,(int)Math.ceil(horizon/.5)));
        for(int i=0;i<=samples;i++){
            double ahead=horizon*i/samples,d=at+ahead,lo=Math.max(at,d-.125),hi=Math.min(end,d+.125);
            if(hi-lo<1e-8)continue;
            double curvature=FlightSteering.angle(tangent.apply(lo),tangent.apply(hi))/(hi-lo);
            if(curvature<1e-8)continue;
            double turnSpeed=budget*.85/curvature;
            target=Math.min(target,Math.sqrt(turnSpeed*turnSpeed+2*a*Math.max(0,ahead-.5)));
        }
        return target;
    }
}
