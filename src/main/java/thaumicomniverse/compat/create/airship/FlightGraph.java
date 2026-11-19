package thaumicomniverse.compat.create.airship;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Server-owned route graph. UUID identity survives renaming, parallel routes and chunk unloading. */
public final class FlightGraph {
    public record Node(UUID id,ResourceLocation dimension,BlockPos block,BlockPos anchor,Direction facing,boolean station,String name,double passRadius,double departureRadius) {
        public Node(UUID id,ResourceLocation dimension,BlockPos block,BlockPos anchor,Direction facing,boolean station,String name){this(id,dimension,block,anchor,facing,station,name,3,3);}
        public Node {
            Objects.requireNonNull(id);Objects.requireNonNull(dimension);Objects.requireNonNull(block);Objects.requireNonNull(anchor);
            Objects.requireNonNull(facing);name=checkedName(name);block=block.immutable();anchor=anchor.immutable();
            if(!facing.getAxis().isHorizontal())throw new IllegalArgumentException("airship.error.direction");
            if(!Double.isFinite(passRadius)||!Double.isFinite(departureRadius)||passRadius<0||departureRadius<0||passRadius>128||departureRadius>128)throw new IllegalArgumentException("airship.error.radius");
        }
        public Vec3 point(){return Vec3.atLowerCornerOf(anchor).add(.5,0,.5);}
    }
    public record Route(UUID id,UUID from,UUID to,int sequence,String name,int color,FlightCurve curve,boolean lockEndDirections) {
        public Route(UUID id,UUID from,UUID to,int sequence,String name,int color,FlightCurve curve){this(id,from,to,sequence,name,color,curve,true);}
        public Route {
            Objects.requireNonNull(id);Objects.requireNonNull(from);Objects.requireNonNull(to);Objects.requireNonNull(curve);
            name=checkedName(name);
            if(sequence<1||color<0||color>15||from.equals(to))throw new IllegalArgumentException("airship.error.route");
            if(curve.length()<.01||curve.length()>4096)throw new IllegalArgumentException("airship.error.length");
        }
        public UUID other(UUID node){if(from.equals(node))return to;if(to.equals(node))return from;throw new IllegalArgumentException("airship.error.endpoint");}
        public FlightCurve leaving(UUID node){if(from.equals(node))return curve;if(to.equals(node))return curve.reverse();throw new IllegalArgumentException("airship.error.endpoint");}
    }
    private final Map<UUID,Node> nodes=new LinkedHashMap<>();
    private final Map<UUID,Route> routes=new LinkedHashMap<>();
    private final Map<UUID,FlightBend> bends=new LinkedHashMap<>();
    public Collection<FlightBend> bends(){return List.copyOf(bends.values());}
    public FlightBend bend(UUID id){return bends.get(id);}
    public FlightBend bend(UUID node,UUID incoming,UUID outgoing,boolean pass){
        if(incoming==null||outgoing==null)return null;
        for(var b:bends.values())if(b.matches(node,incoming,outgoing,pass))return b.incoming().equals(incoming)?b:b.reversed();
        return null;
    }
    public void ensureBends(){
        if(bends.values().removeIf(b->!FlightBend.needed(requireNode(b.node()),requireRoute(b.incoming()),requireRoute(b.outgoing()))))revision++;
        for(var n:nodes.values()){
            var connected=exits(n.id,null);
            for(int i=0;i<connected.size();i++)for(int j=i;j<connected.size();j++){
                var a=connected.get(i);var b=connected.get(j);
                if(!FlightBend.needed(n,a,b))continue;
                if(bend(n.id,a.id,b.id,true)==null)restoreBend(FlightBend.generate(n,a,b,true));
                if(bend(n.id,a.id,b.id,false)==null)restoreBend(FlightBend.generate(n,a,b,false));
                if(i!=j&&bend(n.id,b.id,a.id,false)==null)restoreBend(FlightBend.generate(n,b,a,false));
            }
        }
    }
    public void restoreBend(FlightBend b){
        if(!FlightBend.needed(requireNode(b.node()),requireRoute(b.incoming()),requireRoute(b.outgoing())))return;
        var n=requireNode(b.node());var a=requireRoute(b.incoming()).leaving(n.id);var exit=requireRoute(b.outgoing()).leaving(n.id);
        if(b.entry()>a.length()||b.exit()>exit.length()||b.curve().start.distanceToSqr(b.passing()?a.position(b.entry()):n.point())>1e-6||b.curve().end.distanceToSqr(exit.position(b.exit()))>1e-6)
            throw new IllegalArgumentException("airship.error.endpoint");
        if(bend(b.node(),b.incoming(),b.outgoing(),b.passing())!=null||bends.putIfAbsent(b.id(),b)!=null)throw new IllegalArgumentException("airship.error.duplicate_route");
        revision++;
    }
    public void editBend(UUID id,String name,int color,List<Vec3> controls,double maximumLength){
        var old=bends.get(id);if(old==null)throw new IllegalArgumentException("airship.error.missing_route");
        editBend(id,name,color,controls,maximumLength,old.lockEndDirections(),old.entry(),old.exit());
    }
    public void editBend(UUID id,String name,int color,List<Vec3> controls,double maximumLength,boolean lockEndDirections,double entry,double exit){
        maximumLength=Math.min(maximumLength,FlightManeuver.MAXIMUM_LENGTH);
        var old=bends.get(id);if(old==null)throw new IllegalArgumentException("airship.error.missing_route");
        if(routeOwners.containsKey(old.incoming())||routeOwners.containsKey(old.outgoing()))throw new IllegalStateException("airship.error.route_occupied");
        var n=requireNode(old.node());for(var p:controls)validateControl(n,n,p);
        var in=requireRoute(old.incoming()).leaving(n.id);var out=requireRoute(old.outgoing()).leaving(n.id);
        if(!Double.isFinite(entry)||!Double.isFinite(exit)||entry<0||exit<0||entry>in.length()||exit>out.length()
                ||!old.passing()&&entry!=0||old.pivot()&&(entry!=0||exit!=0))throw new IllegalArgumentException("airship.error.endpoint");
        var curve=new FlightCurve(old.passing()?in.position(entry):n.point(),controls,out.position(exit));
        if(curve.length()>maximumLength)throw new IllegalArgumentException("airship.error.bend_length");
        if(old.pivot())curve=FlightCurve.straight(n.point(),n.point());
        else if(lockEndDirections)curve=FlightEditing.align(curve,in.tangent(entry).scale(-1),out.tangent(exit));
        if(curve.length()>maximumLength)throw new IllegalArgumentException("airship.error.bend_length");
        bends.put(id,new FlightBend(id,old.node(),old.incoming(),old.outgoing(),old.passing(),entry,exit,old.radius(),old.pivot(),name,color,curve,lockEndDirections));revision++;
    }
    public void radii(UUID id,double pass,double departure){var n=requireNode(id);nodes.put(id,new Node(n.id,n.dimension,n.block,n.anchor,n.facing,n.station,n.name,pass,departure));revision++;}
    private final Map<UUID,UUID> routeOwners=new HashMap<>(),stationOwners=new HashMap<>();
    private final Set<UUID> portalNodes=new HashSet<>();
    private final Map<UUID,UUID> portalLinks=new HashMap<>();
    public boolean portal(UUID node){return portalNodes.contains(node);}
    public UUID portalDestination(UUID node){return portalLinks.get(node);}
    public Set<UUID> portalNodes(){return Set.copyOf(portalNodes);}
    public Map<UUID,UUID> portalLinks(){return Map.copyOf(portalLinks);}
    public void markPortal(UUID node){if(requireNode(node).station())throw new IllegalArgumentException("airship.error.portal_waypoint");if(portalNodes.add(node))revision++;}
    public void clearPortal(UUID node){
        boolean changed=portalNodes.remove(node);changed|=portalLinks.remove(node)!=null;changed|=portalLinks.values().removeIf(node::equals);
        if(changed)revision++;
    }
    public void linkPortal(UUID from,UUID to){
        var a=requireNode(from);var b=requireNode(to);if(a.station||b.station||a.dimension.equals(b.dimension))throw new IllegalArgumentException("airship.error.portal_pair");
        markPortal(from);markPortal(to);if(!Objects.equals(portalLinks.put(from,to),to))revision++;
    }
    public void unlinkPortal(UUID node){if(portalLinks.remove(node)!=null)revision++;}
    public double stepLength(UUID step){var route=routes.get(step);return route!=null?route.curve.length():portalLinks.containsKey(step)?1:Double.POSITIVE_INFINITY;}
    private int nextSequence=1;
    private long revision;
    public Collection<Node> nodes(){return List.copyOf(nodes.values());}
    public Collection<Route> routes(){return List.copyOf(routes.values());}
    public Node node(UUID id){return nodes.get(id);}
    public Route route(UUID id){return routes.get(id);}
    public long revision(){return revision;}
    public int nextSequence(){return nextSequence;}
    public void restoreSequence(int value){nextSequence=Math.max(nextSequence,value);}
    public static String checkedName(String name){
        if(name==null||name.length()>64||name.codePoints().anyMatch(Character::isISOControl))throw new IllegalArgumentException("airship.error.name");
        return name.strip();
    }
    public void add(Node node){
        if(nodes.containsKey(node.id))throw new IllegalArgumentException("airship.error.duplicate_node");
        if(nodes.values().stream().anyMatch(n->n.dimension.equals(node.dimension)&&n.block.equals(node.block)))
            throw new IllegalArgumentException("airship.error.duplicate_node");
        nodes.put(node.id,node);revision++;
    }
    public void renameNode(UUID id,String name){
        var n=requireNode(id);nodes.put(id,new Node(n.id,n.dimension,n.block,n.anchor,n.facing,n.station,name,n.passRadius,n.departureRadius));revision++;
    }
    public static boolean canConnect(Node a,Node b){
        return !a.id.equals(b.id)&&a.dimension.equals(b.dimension)&&a.anchor.distManhattan(b.anchor)>0
            &&Math.max(Math.abs((long)a.anchor.getX()-b.anchor.getX()),Math.max(Math.abs((long)a.anchor.getY()-b.anchor.getY()),Math.abs((long)a.anchor.getZ()-b.anchor.getZ())))<=256;
    }
    public Route connect(UUID a,UUID b){
        var from=requireNode(a);var to=requireNode(b);
        if(!canConnect(from,to))throw new IllegalArgumentException("airship.error.range");
        long parallel=routes.values().stream().filter(r->r.from.equals(a)&&r.to.equals(b)||r.to.equals(a)&&r.from.equals(b)).count();
        if(parallel>=32)throw new IllegalArgumentException("airship.error.route_limit");
        var start=from.point();var end=to.point();var delta=end.subtract(start);
        var side=new Vec3(-delta.z,0,delta.x).normalize();if(side.lengthSqr()<.1)side=new Vec3(1,0,0);
        var offset=parallel==0?Vec3.ZERO:side.scale((parallel%2==1?1:-1)*8*((parallel+1)/2));
        int previousSequence=nextSequence;long previousRevision=revision;var previousBends=new HashSet<>(bends.keySet());
        var generated=FlightEditing.alignAxes(new FlightCurve(start,start.lerp(end,1.0/3),start.lerp(end,2.0/3),end),
            FlightEditing.direction(from),FlightEditing.direction(to));
        if(parallel>0){
            // Offset interior handles only; endpoints must obey the same lock before and after editing.
            while(generated.controls.size()<4)generated=generated.elevated();
            var handles=new ArrayList<>(generated.controls);
            for(int i=1;i<handles.size()-1;i++)handles.set(i,handles.get(i).add(offset));
            generated=new FlightCurve(start,handles,end);
        }
        var route=new Route(UUID.randomUUID(),a,b,nextSequence++,"",0,generated);
        try{restore(route);ensureBends();return route;}
        catch(RuntimeException failure){routes.remove(route.id());bends.keySet().removeIf(id->!previousBends.contains(id));nextSequence=previousSequence;revision=previousRevision;throw failure;}
    }
    public void restore(Route route){
        var a=requireNode(route.from);var b=requireNode(route.to);
        if(!canConnect(a,b)||a.point().distanceToSqr(route.curve.start)>1e-8||b.point().distanceToSqr(route.curve.end)>1e-8)
            throw new IllegalArgumentException("airship.error.endpoint");
        for(var p:route.curve.controls)validateControl(a,b,p);
        if(routes.putIfAbsent(route.id,route)!=null)throw new IllegalArgumentException("airship.error.duplicate_route");
        nextSequence=Math.max(nextSequence,route.sequence+1);revision++;
    }
    private static void validateControl(Node a,Node b,Vec3 p){
        if(!FlightCurve.finite(p)||p.distanceTo(a.point())>768||p.distanceTo(b.point())>768)
            throw new IllegalArgumentException("airship.error.control_range");
    }
    public void edit(UUID id,String name,int color,Vec3 c1,Vec3 c2){
        edit(id,name,color,List.of(c1,c2));
    }
    public void edit(UUID id,String name,int color,List<Vec3> controls){
        edit(id,name,color,controls,requireRoute(id).lockEndDirections());
    }
    public void edit(UUID id,String name,int color,List<Vec3> controls,boolean lockEndDirections){
        var old=requireRoute(id);
        if(routeOwners.containsKey(id))throw new IllegalStateException("airship.error.route_occupied");
        for(var p:controls)validateControl(requireNode(old.from),requireNode(old.to),p);
        var edited=new FlightCurve(old.curve.start,controls,old.curve.end);
        if(lockEndDirections)edited=FlightEditing.alignAxes(edited,FlightEditing.direction(requireNode(old.from)),FlightEditing.direction(requireNode(old.to)));
        for(var p:edited.controls)validateControl(requireNode(old.from),requireNode(old.to),p);
        boolean same=edited.controls.size()==old.curve.controls.size();
        for(int i=0;same&&i<edited.controls.size();i++)same=edited.controls.get(i).distanceToSqr(old.curve.controls.get(i))<1e-16;
        var replacement=new Route(id,old.from,old.to,old.sequence,name,color,same?old.curve:edited,lockEndDirections);
        if(same){routes.put(id,replacement);revision++;return;}
        var updated=new LinkedHashMap<UUID,FlightBend>();
        // Route geometry changed: retain user controls, re-anchor connector endpoints onto the edited routes.
        for(var b:List.copyOf(bends.values()))if(b.incoming().equals(id)||b.outgoing().equals(id)){
            if(routeOwners.containsKey(b.incoming())||routeOwners.containsKey(b.outgoing()))throw new IllegalStateException("airship.error.route_occupied");
            var n=requireNode(b.node());var inRoute=b.incoming().equals(id)?replacement:requireRoute(b.incoming());var outRoute=b.outgoing().equals(id)?replacement:requireRoute(b.outgoing());
            if(!FlightBend.needed(n,inRoute,outRoute))continue;
            var a=inRoute.leaving(n.id);var out=outRoute.leaving(n.id);
            double entry=Math.min(b.entry(),a.length()),exit=Math.min(b.exit(),out.length());
            var start=b.passing()?a.position(entry):n.point();var end=out.position(exit);
            var controlsMoved=new ArrayList<Vec3>();int count=b.curve().controls.size();
            for(int i=0;i<count;i++){
                double weight=(i+1d)/(count+1d);
                controlsMoved.add(b.curve().controls.get(i).add(start.subtract(b.curve().start).lerp(end.subtract(b.curve().end),weight)));
            }
            FlightCurve curve;
            try{
                curve=new FlightCurve(start,controlsMoved,end);
                if(!b.pivot()&&b.lockEndDirections())curve=FlightEditing.align(curve,a.tangent(entry).scale(-1),out.tangent(exit));
                if(curve.length()>FlightManeuver.MAXIMUM_LENGTH)throw new IllegalArgumentException("airship.error.manoeuvre");
                updated.put(b.id(),new FlightBend(b.id(),b.node(),b.incoming(),b.outgoing(),b.passing(),entry,exit,b.radius(),b.pivot(),b.name(),b.color(),curve,b.lockEndDirections()));
            }catch(IllegalArgumentException incompatible){
                // An edited route may make the old connector's fixed endpoints impossible.
                // Regenerate just that connector, retaining its identity and presentation.
                var generated=FlightBend.generate(n,b.incoming().equals(id)?replacement:requireRoute(b.incoming()),b.outgoing().equals(id)?replacement:requireRoute(b.outgoing()),b.passing());
                updated.put(b.id(),new FlightBend(b.id(),b.node(),b.incoming(),b.outgoing(),b.passing(),generated.entry(),generated.exit(),generated.radius(),generated.pivot(),b.name(),b.color(),generated.curve(),b.lockEndDirections()));
            }
        }
        var previousBends=new LinkedHashMap<>(bends);long previousRevision=revision;
        try{routes.put(id,replacement);bends.putAll(updated);ensureBends();revision++;}
        catch(RuntimeException failure){routes.put(id,old);bends.clear();bends.putAll(previousBends);revision=previousRevision;throw failure;}
    }
    public void deleteRoute(UUID id){
        bends.values().removeIf(b->b.incoming().equals(id)||b.outgoing().equals(id));
        routeOwners.remove(id);
        if(routes.remove(id)!=null)revision++;
    }
    /** Removing the real block invalidates links; the flight manager stops affected vehicles safely. */
    public Set<UUID> removeNode(UUID id){
        var removed=new LinkedHashSet<UUID>();
        for(var route:List.copyOf(routes.values()))if(route.from.equals(id)||route.to.equals(id)){
            removed.add(route.id);routes.remove(route.id);routeOwners.remove(route.id);
        }
        bends.values().removeIf(b->b.node().equals(id)||removed.contains(b.incoming())||removed.contains(b.outgoing()));
        nodes.remove(id);stationOwners.remove(id);portalNodes.remove(id);portalLinks.remove(id);portalLinks.values().removeIf(id::equals);revision++;return removed;
    }
    public List<Route> exits(UUID at,UUID incoming){
        return routes.values().stream().filter(r->(r.from.equals(at)||r.to.equals(at))&&!r.id.equals(incoming))
            .sorted(Comparator.comparingInt(Route::sequence).thenComparing(Route::id)).toList();
    }
    public Optional<Route> straightest(UUID at,UUID incoming,Vec3 arrivalDirection){
        return exits(at,incoming).stream().max(Comparator.<Route>comparingDouble(r->arrivalDirection.normalize().dot(r.leaving(at).tangent(0)))
            .thenComparing(Comparator.comparingInt(Route::sequence).reversed()));
    }
    public List<UUID> shortestPath(UUID from,UUID destination,UUID ship){return shortestPath(from,destination,ship,null);}
    public List<UUID> shortestPath(UUID from,UUID destination,UUID ship,UUID exitedPortal){
        requireNode(from);requireNode(destination);
        record State(UUID node,boolean portalExit){}
        record Visit(State state,double cost){}
        record Step(State previous,UUID id){}
        var queue=new PriorityQueue<Visit>(Comparator.comparingDouble(Visit::cost).thenComparing(v->v.state.node));
        var cost=new HashMap<State,Double>();var via=new HashMap<State,Step>();
        var origin=new State(from,from.equals(exitedPortal));cost.put(origin,0d);queue.add(new Visit(origin,0));State finish=null;
        while(!queue.isEmpty()){
            var current=queue.remove();var at=current.state;
            if(current.cost>cost.getOrDefault(at,Double.POSITIVE_INFINITY))continue;
            if(at.node.equals(destination)){finish=at;break;}
            for(var route:exits(at.node,null)){
                if(!available(routeOwners,route.id,ship))continue;
                var next=new State(route.other(at.node),false);double value=current.cost+route.curve.length();
                if(value<cost.getOrDefault(next,Double.POSITIVE_INFINITY)){
                    cost.put(next,value);via.put(next,new Step(at,route.id));queue.add(new Visit(next,value));
                }
            }
            UUID exit=portal(at.node)&&!at.portalExit?portalLinks.get(at.node):null;
            if(exit!=null&&available(stationOwners,exit,ship)){
                var next=new State(exit,true);double value=current.cost+1;
                if(value<cost.getOrDefault(next,Double.POSITIVE_INFINITY)){cost.put(next,value);via.put(next,new Step(at,at.node));queue.add(new Visit(next,value));}
            }
        }
        if(finish==null)return List.of();
        var path=new ArrayList<UUID>();State cursor=finish;
        while(!cursor.equals(origin)){var step=via.get(cursor);path.add(step.id);cursor=step.previous;}
        Collections.reverse(path);return List.copyOf(path);
    }
    private static boolean available(Map<UUID,UUID> map,UUID key,UUID ship){var owner=map.get(key);return owner==null||owner.equals(ship);}
    public boolean reserveRoute(UUID route,UUID ship){requireRoute(route);Objects.requireNonNull(ship);if(!available(routeOwners,route,ship))return false;routeOwners.put(route,ship);return true;}
    public boolean reserveStation(UUID node,UUID ship){requireNode(node);Objects.requireNonNull(ship);if(!available(stationOwners,node,ship))return false;stationOwners.put(node,ship);return true;}
    public UUID routeOwner(UUID route){return routeOwners.get(route);}
    public UUID stationOwner(UUID node){return stationOwners.get(node);}
    public void releaseRoute(UUID route,UUID ship){routeOwners.remove(route,ship);}
    public void releaseStation(UUID node,UUID ship){stationOwners.remove(node,ship);}
    public void release(UUID ship){routeOwners.values().removeIf(ship::equals);stationOwners.values().removeIf(ship::equals);}
    private Node requireNode(UUID id){var n=nodes.get(id);if(n==null)throw new IllegalArgumentException("airship.error.missing_node");return n;}
    private Route requireRoute(UUID id){var r=routes.get(id);if(r==null)throw new IllegalArgumentException("airship.error.missing_route");return r;}
}
