package thaumicomniverse.compat.create.airship;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

/** Bounded editor snapshots and server-validated actions; the client never supplies a vehicle state. */
public final class AirshipNetwork {
    private static final Map<AirshipEntity,String> displayKeys=new WeakHashMap<>();
    private record Nearby(long revision,long tick,UUID route){}
    private static final Map<AirshipEntity,Nearby> nearestRoutes=new WeakHashMap<>();
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(ResourceLocation.parse("thaumicomniverse:airship"),()->"6","6"::equals,"6"::equals);
    public record View(BlockPos pos,CompoundTag data,boolean open,boolean highlight){}
    public record Action(BlockPos pos,String action,CompoundTag data){}
    public record Status(int entity,CompoundTag data){}
    public record Pose(int entity,long tick,net.minecraft.world.phys.Vec3 position,float yaw,double speed){}
    public record ScheduleStations(CompoundTag data){}
    public record ResumeControls(int entity,UUID ship,ResourceLocation dimension,BlockPos local){}
    public static void register(){
        CHANNEL.messageBuilder(View.class,0,NetworkDirection.PLAY_TO_CLIENT).encoder((p,b)->{b.writeBlockPos(p.pos);b.writeNbt(p.data);b.writeBoolean(p.open);b.writeBoolean(p.highlight);})
            .decoder(b->new View(b.readBlockPos(),b.readNbt(),b.readBoolean(),b.readBoolean())).consumerMainThread((p,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->AirshipClient.view(p))).add();
        CHANNEL.messageBuilder(Action.class,1,NetworkDirection.PLAY_TO_SERVER).encoder((p,b)->{b.writeBlockPos(p.pos);b.writeUtf(p.action,32);b.writeNbt(p.data);})
            .decoder(b->new Action(b.readBlockPos(),b.readUtf(32),b.readNbt())).consumerMainThread((p,c)->act(c.get().getSender(),p)).add();
        CHANNEL.messageBuilder(Status.class,2,NetworkDirection.PLAY_TO_CLIENT).encoder((p,b)->{b.writeVarInt(p.entity);b.writeNbt(p.data);})
            .decoder(b->new Status(b.readVarInt(),b.readNbt())).consumerMainThread((p,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->AirshipClient.status(p))).add();
        CHANNEL.messageBuilder(Pose.class,3,NetworkDirection.PLAY_TO_CLIENT).encoder((p,b)->{b.writeVarInt(p.entity);b.writeLong(p.tick);b.writeDouble(p.position.x);b.writeDouble(p.position.y);b.writeDouble(p.position.z);b.writeFloat(p.yaw);b.writeDouble(p.speed);})
            .decoder(b->new Pose(b.readVarInt(),b.readLong(),new net.minecraft.world.phys.Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readFloat(),b.readDouble()))
            .consumerMainThread((p,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->AirshipClient.pose(p))).add();
        CHANNEL.messageBuilder(ScheduleStations.class,4,NetworkDirection.PLAY_TO_CLIENT).encoder((p,b)->b.writeNbt(p.data)).decoder(b->new ScheduleStations(b.readNbt()))
            .consumerMainThread((p,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->AirshipClient.scheduleStations(p.data))).add();
        CHANNEL.messageBuilder(ResumeControls.class,5,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((p,b)->{b.writeVarInt(p.entity);b.writeUUID(p.ship);b.writeResourceLocation(p.dimension);b.writeBlockPos(p.local);})
            .decoder(b->new ResumeControls(b.readVarInt(),b.readUUID(),b.readResourceLocation(),b.readBlockPos()))
            .consumerMainThread((p,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->AirshipClient.resumeControls(p))).add();
    }
    public static void resumeControls(ServerPlayer player,AirshipEntity entity,BlockPos local){
        CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new ResumeControls(entity.getId(),entity.getUUID(),entity.level().dimension().location(),local));
    }
    public static void scheduleStations(ServerPlayer player){
        var nodes=new ListTag();var names=new HashSet<String>();
        for(var node:FlightData.get(player.server).graph.nodes())if(node.station()&&!node.name().isBlank()&&names.add(node.name())&&nodes.size()<4096){var n=new CompoundTag();n.putString("Name",node.name());n.putInt("Distance",node.dimension().equals(player.level().dimension().location())?(int)node.point().distanceTo(player.position()):Integer.MAX_VALUE);nodes.add(n);}
        var data=new CompoundTag();data.put("Stations",nodes);CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new ScheduleStations(data));
    }
    public static void pose(AirshipEntity entity,FlightState s){CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->entity),new Pose(entity.getId(),entity.level().getGameTime(),s.position,s.yaw,s.speed));}
    public static void send(BlockPos pos,String action,CompoundTag data){CHANNEL.sendToServer(new Action(pos,action,data));}
    private static boolean inRange(FlightGraph.Node n,AirshipBlocks.NodeTile tile){return n.dimension().equals(tile.getLevel().dimension().location())&&Math.abs((long)n.anchor().getX()-tile.getBlockPos().getX())<=256&&Math.abs((long)n.anchor().getZ()-tile.getBlockPos().getZ())<=256;}
    private static CompoundTag snapshot(ServerPlayer player,AirshipBlocks.NodeTile tile){
        var out=tile.getUpdateTag();out.putBoolean("Station",tile.station());out.putBoolean("AssemblyAvailable",tile.assemblyAvailable());var data=FlightData.get(player.server);var graph=data.graph;
        var nodes=new ListTag();var ids=new HashSet<UUID>();
        for(var n:graph.nodes())if(inRange(n,tile)&&nodes.size()<1024){nodes.add(FlightData.node(n));ids.add(n.id());}out.put("Nodes",nodes);
        var routes=new ListTag();for(var r:graph.routes())if((ids.contains(r.from())||ids.contains(r.to()))&&routes.size()<4096){var tag=FlightData.route(r);tag.putBoolean("Occupied",graph.routeOwner(r.id())!=null);routes.add(tag);}out.put("Routes",routes);
        var bends=new ListTag();for(var b:graph.bends())if(b.node().equals(tile.nodeId)){var tag=FlightData.bend(b);tag.putBoolean("Occupied",graph.routeOwner(b.incoming())!=null||graph.routeOwner(b.outgoing())!=null);bends.add(tag);}out.put("Bends",bends);
        var ship=AirshipManager.docked(data,tile.nodeId);if(ship!=null){var status=new CompoundTag();status.putUUID("Id",ship.id);status.putBoolean("Aligned",ship.aligned);status.putBoolean("Aligning",ship.alignmentTicks>0);status.putBoolean("Automatic",ship.automatic);status.putBoolean("Paused",ship.paused);status.putBoolean("Schedule",!ship.schedule.isEmpty());out.put("Ship",status);}
        return out;
    }
    public static void open(ServerPlayer player,AirshipBlocks.NodeTile tile,boolean highlight){CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new View(tile.getBlockPos(),snapshot(player,tile),true,highlight));}
    private static void checkNode(FlightGraph graph,UUID id,AirshipBlocks.NodeTile tile){var n=graph.node(id);if(n==null||!inRange(n,tile))throw new IllegalArgumentException("airship.error.range");}
    private static void act(ServerPlayer player,Action packet){
        if(player==null||packet.data==null||player.isSpectator()||player.distanceToSqr(packet.pos.getCenter())>64||!player.level().hasChunkAt(packet.pos)
            ||!player.level().mayInteract(player,packet.pos)||!(player.level().getBlockEntity(packet.pos) instanceof AirshipBlocks.NodeTile tile))return;
        var data=FlightData.get(player.server);String error="";
        try{
            if(!thaumicomniverse.RevivalFeatures.Group.CREATE.enabled())throw new IllegalStateException("airship.error.disabled");
            var arg=packet.data;
            switch(packet.action){
                case "refresh" -> {}
                case "highlight" -> {open(player,tile,true);return;}
                case "configure" -> {
                    if(tile.confirmed)throw new IllegalStateException("airship.error.anchor_locked");
                    var anchor=BlockPos.of(arg.getLong("Anchor"));var facing=Direction.from3DDataValue(arg.getInt("Facing"));
                    if(!facing.getAxis().isHorizontal()||Math.max(Math.abs((long)anchor.getX()-packet.pos.getX()),Math.max(Math.abs((long)anchor.getY()-packet.pos.getY()),Math.abs((long)anchor.getZ()-packet.pos.getZ())))>256
                        ||player.level().isOutsideBuildHeight(anchor)||!player.level().getWorldBorder().isWithinBounds(anchor))throw new IllegalArgumentException("airship.error.anchor_range");
                    String name=FlightGraph.checkedName(arg.getString("Name"));
                    if(arg.getBoolean("Confirm")){
                        if(!player.level().hasChunkAt(anchor))throw new IllegalStateException("airship.error.anchor_unloaded");
                        if(data.graph.nodes().stream().anyMatch(n->n.dimension().equals(tile.getLevel().dimension().location())&&n.anchor().equals(anchor)))throw new IllegalStateException("airship.error.anchor_used");
                        tile.anchor=anchor;tile.facing=facing;tile.name=name;data.graph.add(tile.node());tile.confirmed=true;
                    }else{tile.anchor=anchor;tile.facing=facing;tile.name=name;}
                }
                case "edit_bend" -> {
                    var bend=data.graph.bend(arg.getUUID("Id"));if(bend==null||!bend.node().equals(tile.nodeId))throw new IllegalArgumentException("airship.error.range");
                    data.graph.editBend(bend.id(),arg.getString("Name"),arg.getInt("Color"),FlightData.controls(arg),thaumicomniverse.RevivalFeatures.AIRSHIP_BEND_LENGTH.get(),!arg.contains("LockEndDirections")||arg.getBoolean("LockEndDirections"),arg.contains("Entry")?arg.getDouble("Entry"):bend.entry(),arg.contains("Exit")?arg.getDouble("Exit"):bend.exit());
                }
                case "doors" -> {var modes=com.simibubi.create.content.decoration.slidingDoor.DoorControl.values();tile.doorControl=modes[Math.floorMod(arg.getInt("Mode"),modes.length)];}
                case "rename_node" -> {if(!tile.confirmed)throw new IllegalStateException("airship.error.confirm_first");data.graph.renameNode(tile.nodeId,arg.getString("Name"));tile.name=data.graph.node(tile.nodeId).name();}
                case "connect" -> {checkNode(data.graph,arg.getUUID("From"),tile);checkNode(data.graph,arg.getUUID("To"),tile);data.graph.connect(arg.getUUID("From"),arg.getUUID("To"));}
                case "edit_route","delete_route" -> {
                    var route=data.graph.route(arg.getUUID("Id"));if(route==null)throw new IllegalStateException("airship.error.missing_route");
                    if(!tile.nodeId.equals(route.from())&&!tile.nodeId.equals(route.to()))throw new IllegalStateException("airship.error.range");
                    if(packet.action.equals("delete_route"))data.graph.deleteRoute(route.id());
                    else data.graph.edit(route.id(),arg.getString("Name"),arg.getInt("Color"),FlightData.controls(arg),!arg.contains("LockEndDirections")||arg.getBoolean("LockEndDirections"));
                }
                case "assembly_mode" -> {if(!tile.confirmed||!tile.station())throw new IllegalStateException("airship.error.confirm_first");tile.assembling=!tile.assembling;}
                case "assemble" -> AirshipManager.assemble(player,tile);
                case "align","disassemble","pause" -> {
                    var ship=AirshipManager.docked(data,tile.nodeId);if(ship==null)throw new IllegalStateException("airship.error.not_docked");
                    if(packet.action.equals("align"))AirshipManager.align(player.server,ship);
                    else if(packet.action.equals("disassemble"))AirshipManager.disassemble(player.server,ship);
                    else{ship.paused=!ship.paused;ship.automatic=!ship.schedule.isEmpty();}
                }
                default -> throw new IllegalArgumentException("airship.error.action");
            }
            tile.changed();data.setDirty();
        }catch(com.simibubi.create.content.contraptions.AssemblyException ex){error="airship.error.assembly";player.displayClientMessage(ex.component,true);}
        catch(IllegalStateException|IllegalArgumentException ex){error=ex.getMessage()!=null&&ex.getMessage().startsWith("airship.")?ex.getMessage():"airship.error.action";player.displayClientMessage(Component.translatable(error),true);}
        var response=snapshot(player,tile);response.putString("Error",error);CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new View(packet.pos,response,false,false));
    }
    private static String nodeName(FlightGraph.Node node){return node==null?"":node.name().isEmpty()?node.anchor().toShortString():node.name();}
    private static UUID nearestRoute(AirshipEntity entity,FlightState s,FlightGraph graph){
        var cached=nearestRoutes.get(entity);long tick=entity.level().getGameTime();
        if(cached==null||cached.revision()!=graph.revision()||tick-cached.tick()>=10){
            var nearest=FlightReattachment.nearest(graph,s);cached=new Nearby(graph.revision(),tick,nearest==null?null:nearest.route().id());nearestRoutes.put(entity,cached);
        }
        return cached.route();
    }
    private static void endpoints(FlightGraph graph,UUID id,Set<UUID> nodes){var route=graph.route(id);if(route!=null){nodes.add(route.from());nodes.add(route.to());}}
    public static void status(AirshipEntity entity,FlightState s,FlightGraph graph){
        var n=new CompoundTag();n.putDouble("Speed",s.speed*20);n.putInt("Fuel",s.fuelTicks);n.putBoolean("Locked",s.selectionLocked);n.putBoolean("Dock",s.dockSelected);n.putBoolean("Automatic",s.automatic);n.putBoolean("Paused",s.paused);n.putString("Message",s.message);n.putBoolean("Detached",FlightReattachment.detached(s));n.putBoolean("Rejoining",s.rejoining);n.putDouble("MaxSpeed",(s.fuelTicks>0?com.simibubi.create.infrastructure.config.AllConfigs.server().trains.poweredTrainTopSpeed.getF():com.simibubi.create.infrastructure.config.AllConfigs.server().trains.trainTopSpeed.getF())*(s.fuelTicks>0?thaumicomniverse.RevivalFeatures.AIRSHIP_POWERED_SPEED_MULTIPLIER:thaumicomniverse.RevivalFeatures.AIRSHIP_SPEED_MULTIPLIER).get());
        n.putBoolean("Free",s.freeControl);n.putBoolean("Approach",FlightNavigation.canApproach(graph,s));n.putBoolean("DockHeld",s.dockHeld);
        n.putInt("ModeTicks",s.modeGestureTicks);n.putBoolean("ModeLatched",s.modeGestureLatched);n.putDouble("Acceleration",s.acceleration*400);
        n.putDouble("DockRemaining",s.atNode!=null?0:FlightNavigation.remaining(graph,s));n.putDouble("DockTotal",s.dockDistanceTotal);
        var at=graph.node(FlightNavigation.approaching(graph,s));n.putString("Node",nodeName(at));
        boolean portal=at!=null&&!at.station()&&graph.portal(at.id());
        String enterKey=portal?"airship.choice.portal":at!=null&&!at.station()?"airship.choice.waypoint":"airship.choice.dock";
        String enterDestination=nodeName(portal?graph.node(graph.portalDestination(at.id())):at);
        n.putString("EnterKey",enterKey);n.putString("EnterDestination",enterDestination);
        var choices=new ListTag();
        if(FlightNavigation.dockingOption(graph,s)){var r=new CompoundTag();r.putBoolean("Docking",true);r.putBoolean("Selected",s.dockSelected);r.putString("LabelKey",enterKey);r.putString("Destination",enterDestination);choices.add(r);}
        UUID arriving=s.atNode!=null?s.incomingRoute:s.transition!=null?s.transitionToRoute:s.route;
        if(!s.freeControl)for(var route:FlightNavigation.choices(graph,s)){var r=new CompoundTag();r.putInt("Sequence",route.sequence());r.putString("Name",route.name());var dest=graph.node(route.other(at.id()));r.putString("Destination",nodeName(dest));r.putBoolean("UTurn",route.id().equals(arriving));r.putBoolean("Disabled",!FlightNavigation.choiceAvailable(graph,s,route.id()));r.putBoolean("Selected",!s.dockSelected&&route.id().equals(s.selectedExit));choices.add(r);}n.put("Choices",choices);
        if(at!=null)n.putBoolean("Station",at.station()&&s.atNode==null);
        UUID current=s.transition!=null?s.transitionFromRoute:s.route,next=s.selectedExit;
        UUID joining=s.transition!=null?s.transitionToRoute:null;
        if(s.transition!=null||s.dockSelected||next!=null&&!FlightNavigation.choiceAvailable(graph,s,next))next=null;
        var relatedNodes=new LinkedHashSet<UUID>();
        if(s.freeControl){current=nearestRoute(entity,s,graph);joining=next=null;endpoints(graph,current,relatedNodes);}
        else if(s.atNode!=null)relatedNodes.add(s.atNode);
        else endpoints(graph,joining!=null?joining:current,relatedNodes);
        FlightBend bend=s.activeBend==null?null:graph.bend(s.activeBend);
        if(bend==null&&!s.freeControl&&!s.rejoining&&!s.dockSelected&&next!=null){
            if(s.atNode!=null)bend=FlightNavigation.departureBend(graph,s,next);
            else if(at!=null&&current!=null)bend=graph.bend(at.id(),current,next,true);
        }
        String key=graph.revision()+":"+s.dimension+":"+relatedNodes+":"+current+":"+joining+":"+next+":"+(bend==null?null:bend.id())+":"+s.freeControl+":"+s.rejoining+":"+s.transition+":"+entity.getControllingPlayer();
        if(!key.equals(displayKeys.put(entity,key))){
            var routes=new ListTag();
            for(var route:graph.routes()){
                var origin=graph.node(route.from());if(origin==null||!origin.dimension().equals(s.dimension))continue;
                boolean emphasized=route.id().equals(current)||route.id().equals(joining)||route.id().equals(next);
                // Alternatives at either endpoint of the current / nearest route are shown dimly.
                boolean related=relatedNodes.contains(route.from())||relatedNodes.contains(route.to());
                if(!emphasized&&!related)continue;
                var tag=FlightData.route(route);tag.putBoolean("Emphasized",emphasized);routes.add(tag);
            }
            n.put("DisplayRoutes",routes);var bends=new ListTag();
            // Unrelated temporary bends stay hidden; recovery takes precedence over a graph bend.
            if(!s.freeControl&&!s.rejoining&&bend!=null&&!bend.pivot()){
                var tag=FlightData.bend(bend);tag.putBoolean("Emphasized",true);bends.add(tag);
            }
            if(!s.freeControl&&s.transition!=null&&(s.rejoining||bend==null)){var tag=FlightData.bend(new FlightBend(s.id,s.id,s.id,s.id,true,0,0,0,false,"",10,s.transition.curves.get(0)));tag.putBoolean("Emphasized",true);bends.add(tag);}
            n.put("DisplayBends",bends);
        }
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->entity),new Status(entity.getId(),n));
    }
}
