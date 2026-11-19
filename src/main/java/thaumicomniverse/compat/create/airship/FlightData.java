package thaumicomniverse.compat.create.airship;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

/** One cross-dimensional saved ledger. Entities are projections; inventories have one authoritative owner. */
public final class FlightData extends SavedData {
    public final FlightGraph graph=new FlightGraph();
    public final Map<UUID,FlightState> ships=new LinkedHashMap<>();
    public CompoundTag portalCache=new CompoundTag();
    public CompoundTag quarantined=new CompoundTag();
    public static FlightData get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(FlightData::load,FlightData::new,"thaumicomniverse_airships");}
    public FlightData(){}
    public static CompoundTag vector(Vec3 v){var n=new CompoundTag();n.putDouble("X",v.x);n.putDouble("Y",v.y);n.putDouble("Z",v.z);return n;}
    public static Vec3 vector(CompoundTag n){var v=new Vec3(n.getDouble("X"),n.getDouble("Y"),n.getDouble("Z"));if(!FlightCurve.finite(v))throw new IllegalArgumentException("Invalid saved flight position");return v;}
    public static ListTag controls(java.util.List<Vec3> points){var list=new ListTag();for(var p:points)list.add(vector(p));return list;}
    public static java.util.List<Vec3> controls(CompoundTag n){
        if(!n.contains("Controls",Tag.TAG_LIST))return java.util.List.of(vector(n.getCompound("Control1")),vector(n.getCompound("Control2")));
        var list=n.getList("Controls",Tag.TAG_COMPOUND);if(list.size()>16)throw new IllegalArgumentException("airship.error.control_count");
        var points=new ArrayList<Vec3>();for(var p:list)points.add(vector((CompoundTag)p));return points;
    }
    public static CompoundTag curve(FlightCurve c){var n=new CompoundTag();n.put("Start",vector(c.start));n.put("Controls",controls(c.controls));n.put("End",vector(c.end));return n;}
    public static FlightCurve curve(CompoundTag n){return new FlightCurve(vector(n.getCompound("Start")),controls(n),vector(n.getCompound("End")));}
    public static CompoundTag bend(FlightBend b){
        var n=curve(b.curve());n.putUUID("Id",b.id());n.putUUID("Node",b.node());n.putUUID("Incoming",b.incoming());n.putUUID("Outgoing",b.outgoing());
        n.putBoolean("LockEndDirections",b.lockEndDirections());n.putBoolean("Passing",b.passing());n.putBoolean("Pivot",b.pivot());n.putDouble("Entry",b.entry());n.putDouble("Exit",b.exit());n.putDouble("Radius",b.radius());n.putString("Name",b.name());n.putInt("Color",b.color());return n;
    }
    public static FlightBend bend(CompoundTag n){return new FlightBend(n.getUUID("Id"),n.getUUID("Node"),n.getUUID("Incoming"),n.getUUID("Outgoing"),n.getBoolean("Passing"),n.getDouble("Entry"),n.getDouble("Exit"),n.getDouble("Radius"),n.getBoolean("Pivot"),n.getString("Name"),n.getInt("Color"),curve(n),!n.contains("LockEndDirections")||n.getBoolean("LockEndDirections"));}
    private static void uuid(CompoundTag n,String key,UUID id){if(id!=null)n.putUUID(key,id);}
    private static UUID uuid(CompoundTag n,String key){return n.hasUUID(key)?n.getUUID(key):null;}
    public static CompoundTag node(FlightGraph.Node node){
        var n=new CompoundTag();n.putUUID("Id",node.id());n.putString("Dimension",node.dimension().toString());
        n.putLong("Block",node.block().asLong());n.putLong("Anchor",node.anchor().asLong());n.putInt("Facing",node.facing().get3DDataValue());
        n.putBoolean("Station",node.station());n.putString("Name",node.name());n.putDouble("PassRadius",node.passRadius());n.putDouble("DepartureRadius",node.departureRadius());return n;
    }
    public static FlightGraph.Node node(CompoundTag n){return new FlightGraph.Node(n.getUUID("Id"),ResourceLocation.parse(n.getString("Dimension")),BlockPos.of(n.getLong("Block")),BlockPos.of(n.getLong("Anchor")),Direction.from3DDataValue(n.getInt("Facing")),n.getBoolean("Station"),n.getString("Name"),n.contains("PassRadius")?n.getDouble("PassRadius"):3,n.contains("DepartureRadius")?n.getDouble("DepartureRadius"):3);}
    public static CompoundTag route(FlightGraph.Route route){
        var n=curve(route.curve());n.putUUID("Id",route.id());n.putUUID("From",route.from());n.putUUID("To",route.to());
        n.putBoolean("LockEndDirections",route.lockEndDirections());n.putString("Name",route.name());n.putInt("Color",route.color());n.putInt("Sequence",route.sequence());return n;
    }
    public static FlightGraph.Route route(CompoundTag n){return new FlightGraph.Route(n.getUUID("Id"),n.getUUID("From"),n.getUUID("To"),n.getInt("Sequence"),n.getString("Name"),n.getInt("Color"),curve(n),!n.contains("LockEndDirections")||n.getBoolean("LockEndDirections"));}
    public static CompoundTag ship(FlightState s){
        var n=new CompoundTag();n.putUUID("Id",s.id);n.putString("Dimension",s.dimension.toString());n.put("Position",vector(s.position));
        n.putFloat("Yaw",s.yaw);n.putFloat("Pitch",s.pitch);n.putDouble("Speed",s.speed);n.putDouble("Distance",s.distance);
        n.putInt("InitialFacing",s.initialFacing.get3DDataValue());n.putDouble("Throttle",s.throttle);
        uuid(n,"ActiveBend",s.activeBend);if(s.motionDirection!=null)n.put("MotionDirection",vector(s.motionDirection));
        n.putBoolean("FreeControl",s.freeControl);n.putBoolean("RequireSpaceReturn",s.requireSpaceReturn);n.putBoolean("RotateAtJoin",s.rotateAtJoin);n.putBoolean("PivotPass",s.pivotPass);n.putDouble("DockDistanceTotal",s.dockDistanceTotal);
        uuid(n,"IncomingRoute",s.incomingRoute);uuid(n,"ChoiceNode",s.choiceNode);
        uuid(n,"PortalExitNode",s.portalExitNode);n.put("Passengers",s.passengers.copy());
        n.putInt("AlignmentTicks",s.alignmentTicks);n.putFloat("AlignmentStartYaw",s.alignmentStartYaw);n.putFloat("AlignmentTargetYaw",s.alignmentTargetYaw);
        uuid(n,"AtNode",s.atNode);uuid(n,"Route",s.route);uuid(n,"FromNode",s.fromNode);uuid(n,"SelectedExit",s.selectedExit);uuid(n,"PlannedExit",s.plannedExit);
        uuid(n,"TransitionFromRoute",s.transitionFromRoute);uuid(n,"TransitionToRoute",s.transitionToRoute);uuid(n,"TransitionToNode",s.transitionToNode);
        n.putDouble("TransitionDistance",s.transitionDistance);n.putDouble("TransitionEndDistance",s.transitionEndDistance);
        if(s.transition!=null){var list=new ListTag();s.transition.curves.forEach(c->list.add(curve(c)));n.put("Transition",list);}
        n.putBoolean("Rejoining",s.rejoining);n.putBoolean("RejoinAutomatic",s.rejoinAutomatic);n.putBoolean("DockSelected",s.dockSelected);n.putBoolean("SelectionLocked",s.selectionLocked);n.putBoolean("Aligned",s.aligned);n.putBoolean("Automatic",s.automatic);n.putBoolean("Paused",s.paused);
        n.putInt("FuelTicks",s.fuelTicks);n.putInt("ScheduleIndex",s.scheduleIndex);n.putInt("WaitTicks",s.waitTicks);n.putString("ScheduleTitle",s.scheduleTitle);
        n.put("Contraption",s.contraption.copy());n.put("Schedule",s.schedule.copy());n.put("WaitState",s.waitState.copy());n.putString("Message",s.message);return n;
    }
    public static FlightState ship(CompoundTag n){
        var s=new FlightState(n.getUUID("Id"),ResourceLocation.parse(n.getString("Dimension")),vector(n.getCompound("Position")));
        s.yaw=n.getFloat("Yaw");s.pitch=n.getFloat("Pitch");s.speed=n.getDouble("Speed");s.distance=n.getDouble("Distance");
        s.initialFacing=Direction.from3DDataValue(n.getInt("InitialFacing"));
        s.activeBend=uuid(n,"ActiveBend");s.motionDirection=n.contains("MotionDirection")?vector(n.getCompound("MotionDirection")):null;
        s.freeControl=n.getBoolean("FreeControl");s.requireSpaceReturn=n.getBoolean("RequireSpaceReturn");s.rotateAtJoin=n.getBoolean("RotateAtJoin");s.pivotPass=n.getBoolean("PivotPass");s.dockDistanceTotal=Math.max(0,n.getDouble("DockDistanceTotal"));
        s.throttle=n.contains("Throttle")?Math.max(0,Math.min(1,n.getDouble("Throttle"))):1;
        s.incomingRoute=uuid(n,"IncomingRoute");s.choiceNode=uuid(n,"ChoiceNode");
        s.portalExitNode=uuid(n,"PortalExitNode");s.passengers=n.getCompound("Passengers").copy();
        s.alignmentTicks=n.getInt("AlignmentTicks");s.alignmentStartYaw=n.getFloat("AlignmentStartYaw");s.alignmentTargetYaw=n.getFloat("AlignmentTargetYaw");
        s.atNode=uuid(n,"AtNode");s.route=uuid(n,"Route");s.fromNode=uuid(n,"FromNode");s.selectedExit=uuid(n,"SelectedExit");s.plannedExit=uuid(n,"PlannedExit");
        s.transitionFromRoute=uuid(n,"TransitionFromRoute");s.transitionToRoute=uuid(n,"TransitionToRoute");s.transitionToNode=uuid(n,"TransitionToNode");
        s.transitionDistance=n.getDouble("TransitionDistance");s.transitionEndDistance=n.getDouble("TransitionEndDistance");
        if(n.contains("Transition")){var curves=new ArrayList<FlightCurve>();for(var c:n.getList("Transition",Tag.TAG_COMPOUND))curves.add(curve((CompoundTag)c));s.transition=new FlightPath(curves);}
        s.rejoining=n.getBoolean("Rejoining");s.rejoinAutomatic=n.getBoolean("RejoinAutomatic");s.dockSelected=n.getBoolean("DockSelected");s.selectionLocked=n.getBoolean("SelectionLocked");s.aligned=n.getBoolean("Aligned");s.automatic=n.getBoolean("Automatic");s.paused=n.getBoolean("Paused");
        s.fuelTicks=Math.max(0,n.getInt("FuelTicks"));s.scheduleIndex=Math.max(0,n.getInt("ScheduleIndex"));s.waitTicks=Math.max(0,n.getInt("WaitTicks"));s.scheduleTitle=n.getString("ScheduleTitle");
        s.contraption=n.getCompound("Contraption").copy();s.schedule=n.getCompound("Schedule").copy();s.waitState=n.getCompound("WaitState").copy();s.message=n.getString("Message");
        if(!Float.isFinite(s.yaw)||!Float.isFinite(s.pitch)||!Double.isFinite(s.speed)||!Double.isFinite(s.distance)||!Double.isFinite(s.transitionDistance)
                ||!Double.isFinite(s.transitionEndDistance)||!Double.isFinite(s.throttle)||!Float.isFinite(s.alignmentStartYaw)||!Float.isFinite(s.alignmentTargetYaw)
                ||s.speed<0||s.distance<0||s.transitionDistance<0||s.transitionEndDistance<0||!s.initialFacing.getAxis().isHorizontal()||s.alignmentTicks<0||s.alignmentTicks>20)
            throw new IllegalArgumentException("Invalid saved flight state");
        return s;
    }
    @Override public CompoundTag save(CompoundTag root){
        root.putInt("Format",1);root.putInt("NextRoute",graph.nextSequence());
        root.put("PortalCache",portalCache.copy());root.put("Quarantined",quarantined.copy());
        var portals=new ListTag();for(var id:graph.portalNodes()){var p=new CompoundTag();p.putUUID("Node",id);uuid(p,"Destination",graph.portalDestination(id));portals.add(p);}root.put("Portals",portals);
        var nodes=new ListTag();graph.nodes().forEach(n->nodes.add(node(n)));root.put("Nodes",nodes);
        var routes=new ListTag();graph.routes().forEach(r->routes.add(route(r)));root.put("Routes",routes);
        var bends=new ListTag();graph.bends().forEach(b->bends.add(bend(b)));root.put("Bends",bends);
        var vehicles=new ListTag();ships.values().forEach(s->vehicles.add(ship(s)));root.put("Ships",vehicles);return root;
    }
    public static FlightData load(CompoundTag root){
        var data=new FlightData();
        data.quarantined=root.getCompound("Quarantined").copy();
        if(root.getInt("Format")!=1)throw new IllegalArgumentException("Unsupported airship save format");
        for(var n:root.getList("Nodes",Tag.TAG_COMPOUND))try{data.graph.add(node((CompoundTag)n));}catch(RuntimeException ex){data.quarantine("Nodes",(CompoundTag)n,ex);}
        data.portalCache=root.getCompound("PortalCache").copy();
        for(var p:root.getList("Portals",Tag.TAG_COMPOUND))try{var tag=(CompoundTag)p;UUID id=uuid(tag,"Node"),to=uuid(tag,"Destination");if(data.graph.node(id)==null)throw new IllegalArgumentException("Missing portal node");data.graph.markPortal(id);if(to!=null){if(data.graph.node(to)==null)throw new IllegalArgumentException("Missing portal destination");data.graph.linkPortal(id,to);}}catch(RuntimeException ex){data.quarantine("Portals",(CompoundTag)p,ex);}
        for(var n:root.getList("Routes",Tag.TAG_COMPOUND))try{data.graph.restore(route((CompoundTag)n));}catch(RuntimeException ex){data.quarantine("Routes",(CompoundTag)n,ex);}
        data.graph.restoreSequence(root.getInt("NextRoute"));
        for(var n:root.getList("Bends",Tag.TAG_COMPOUND))try{data.graph.restoreBend(bend((CompoundTag)n));}catch(RuntimeException ex){data.quarantine("Bends",(CompoundTag)n,ex);}
        data.graph.ensureBends();
        for(var n:root.getList("Ships",Tag.TAG_COMPOUND)){
            FlightState s;try{s=ship((CompoundTag)n);if(data.ships.putIfAbsent(s.id,s)!=null)throw new IllegalArgumentException("Duplicate airship identity");}
            catch(RuntimeException ex){data.quarantine("Ships",(CompoundTag)n,ex);continue;}
            if(FlightReattachment.invalid(data.graph,s))FlightReattachment.detach(data.graph,s,"airship.rejoin.detached");
            boolean valid=true;
            try{
            for(UUID id:new UUID[]{s.route,s.transitionFromRoute,s.transitionToRoute})if(id!=null)
                valid&=data.graph.route(id)!=null&&data.graph.reserveRoute(id,s.id);
            if(s.atNode!=null){var at=data.graph.node(s.atNode);valid&=at!=null&&data.graph.reserveStation(s.atNode,s.id);}
            // Departures and passing bends still occupy the junction until their merge is complete.
            if(s.transition!=null&&!s.rejoining&&s.transitionToNode!=null)
                valid&=data.graph.node(s.transitionToNode)!=null&&data.graph.reserveStation(s.transitionToNode,s.id);
            // Every active segment reserves its destination before entering it, including a pending turn.
            if(s.route!=null&&data.graph.route(s.route)!=null&&s.fromNode!=null){var dest=data.graph.node(data.graph.route(s.route).other(s.fromNode));if(dest!=null)valid&=data.graph.reserveStation(dest.id(),s.id);}
            if(s.transitionToRoute!=null&&data.graph.route(s.transitionToRoute)!=null&&s.transitionToNode!=null){var dest=data.graph.node(data.graph.route(s.transitionToRoute).other(s.transitionToNode));if(dest!=null)valid&=data.graph.reserveStation(dest.id(),s.id);}
            }catch(IllegalArgumentException ex){valid=false;}
            if(!valid){data.graph.release(s.id);s.stop("airship.error.restore_conflict");s.paused=true;}
        }
        return data;
    }
    private void quarantine(String category,CompoundTag raw,RuntimeException error){
        var list=quarantined.getList(category,Tag.TAG_COMPOUND);list.add(raw.copy());quarantined.put(category,list);
        com.mojang.logging.LogUtils.getLogger().error("Preserved malformed airship {} record in Quarantined; remaining vehicles continue loading",category,error);
    }
}
