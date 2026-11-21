package thaumicomniverse.compat.create.airship;

import java.util.*;
import com.simibubi.create.content.contraptions.*;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import thaumicomniverse.compat.create.*;

/** Global ledger advances even when no route chunks are loaded. It never forces route chunks. */
public final class AirshipManager {
    private static final Map<MinecraftServer,Map<UUID,AirshipEntity>> projections=new IdentityHashMap<>();
    private AirshipManager(){}
    private static Map<UUID,AirshipEntity> projections(MinecraftServer s){return projections.computeIfAbsent(s,k->new HashMap<>());}
    public static AirshipEntity entity(MinecraftServer server,UUID id){return projections(server).get(id);}
    /** Called only after a replacement owns the complete structure and saved passengers. */
    public static void retireForRelocation(AirshipEntity entity){
        var server=((ServerLevel)entity.level()).getServer();var data=FlightData.get(server);
        data.ships.remove(entity.getUUID());data.graph.release(entity.getUUID());
        projections(server).remove(entity.getUUID());data.setDirty();
    }
    public static void removeNode(MinecraftServer server,UUID id){
        var data=FlightData.get(server);var removed=data.graph.removeNode(id);
        for(var s:data.ships.values())if(id.equals(s.atNode)||removed.contains(s.route)||removed.contains(s.transitionFromRoute)||removed.contains(s.transitionToRoute)){
            FlightReattachment.detach(data.graph,s,"airship.rejoin.detached");
        }data.setDirty();
    }
    public static boolean isDocked(FlightGraph graph,FlightState s){var node=graph.node(s.atNode);return node!=null&&node.station()&&!s.pivotPass&&s.speed<1e-7&&s.position.distanceToSqr(node.point())<1e-6;}
    public static FlightState docked(FlightData data,UUID node){return data.ships.values().stream().filter(s->node.equals(s.atNode)&&isDocked(data.graph,s)).findFirst().orElse(null);}
    public static FlightState assemble(ServerPlayer player,AirshipBlocks.NodeTile tile)throws AssemblyException{
        var level=player.serverLevel();var data=FlightData.get(level.getServer());
        if(!tile.station()||!tile.confirmed||!tile.assemblyAvailable())throw new IllegalStateException("airship.error.assembly_mode");
        if(data.graph.stationOwner(tile.nodeId)!=null)throw new IllegalStateException("airship.error.station_occupied");
        var old=level.getBlockState(tile.anchor);boolean converted=old.is(AirshipContent.CASING.get());
        if(converted)tile.ensureCore();
        boolean removed=false;AirshipEntity entity=null;
        var rollback=new LinkedHashMap<BlockPos,net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo>();
        try{
            var structure=new AirshipStructure();if(!structure.assemble(level,tile.anchor))throw new IllegalStateException("airship.error.core_at_anchor");
            for(var local:structure.getBlocks().keySet())if(!level.mayInteract(player,local.offset(structure.anchor))||!player.mayUseItemAt(local.offset(structure.anchor),Direction.UP,player.getMainHandItem()))throw new IllegalStateException("airship.error.permission");
            for(var local:structure.getBlocks().keySet()){var pos=local.offset(structure.anchor);var be=level.getBlockEntity(pos);rollback.put(pos,new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo(pos,level.getBlockState(pos),be==null?null:be.saveWithFullMetadata()));}
            var s=new FlightState(UUID.randomUUID(),level.dimension().location(),tile.node().point());s.atNode=tile.nodeId;s.initialFacing=tile.facing;s.yaw=tile.facing.toYRot();s.aligned=true;
            entity=new AirshipEntity(AirshipContent.ENTITY.get(),level);entity.attach(structure,s);
            s.contraption=structure.writeNBT(false);
            // The UUID and reservation exist before the projection can receive an interaction.
            data.ships.put(s.id,s);data.graph.reserveStation(tile.nodeId,s.id);
            if(!level.addFreshEntity(entity))throw new IllegalStateException("airship.error.spawn");
            removed=true;structure.removeBlocksFromWorld(level,BlockPos.ZERO);
            projections(level.getServer()).put(s.id,entity);tile.assembling=false;tile.changed();data.setDirty();return s;
        }catch(RuntimeException|AssemblyException failure){
            if(entity!=null){data.ships.remove(entity.getUUID());data.graph.release(entity.getUUID());entity.discard();}
            if(removed)for(var block:rollback.values()){level.setBlock(block.pos(),block.state(),3);var be=level.getBlockEntity(block.pos());if(be!=null&&block.nbt()!=null){be.load(block.nbt());be.setChanged();}}
            if(converted)level.setBlock(tile.anchor,old,3);
            throw failure;
        }
    }
    public static void align(MinecraftServer server,FlightState s){
        var data=FlightData.get(server);var node=data.graph.node(s.atNode);var entity=entity(server,s.id);
        if(!isDocked(data.graph,s)||entity==null||!entity.isAddedToWorld())throw new IllegalStateException("airship.error.not_docked");
        s.pitch=0;s.motionDirection=FlightNavigation.forward(s.yaw);entity.pitch=entity.prevPitch=0;
        float old=entity.yaw;float target=node.facing().toYRot();
        try{for(int i=1;i<=36;i++){entity.yaw=old+net.minecraft.util.Mth.wrapDegrees(target-old)*i/36f;if(!RelocationSessions.clear(entity,Vec3.ZERO,1e-5))throw new IllegalStateException("airship.error.alignment_obstructed");}}
        finally{entity.yaw=old;}
        s.alignmentStartYaw=s.yaw;s.alignmentTargetYaw=target;s.alignmentTicks=20;s.aligned=false;s.automatic=false;s.forward=s.reverse=false;s.paused=false;data.setDirty();
    }
    public static void disassemble(MinecraftServer server,FlightState s){
        var entity=entity(server,s.id);var data=FlightData.get(server);
        if(!isDocked(data.graph,s)||!s.aligned||s.alignmentTicks!=0||entity==null||!entity.isAddedToWorld())throw new IllegalStateException("airship.error.align_first");
        if(!entity.canDisassemble())throw new IllegalStateException("airship.error.placement_obstructed");
        if(!s.schedule.isEmpty())throw new IllegalStateException("airship.error.remove_schedule");
        entity.disassemble();data.ships.remove(s.id);data.graph.release(s.id);projections(server).remove(s.id);data.setDirty();
        var node=data.graph.node(s.atNode);if(entity.level().getBlockEntity(node.block()) instanceof AirshipBlocks.NodeTile tile){tile.assembling=true;tile.changed();}
    }
    private static AirshipEntity projection(MinecraftServer server,FlightState s){
        var map=projections(server);var current=map.get(s.id);
        if(current!=null&&!current.isRemoved()&&current.level().dimension().location().equals(s.dimension))return current;
        var level=server.getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,s.dimension));if(level==null)return null;
        if(s.contraption.isEmpty())return null;
        var entity=new AirshipEntity(AirshipContent.ENTITY.get(),level);entity.attach(Contraption.fromNBT(level,s.contraption.copy(),false),s);map.put(s.id,entity);return entity;
    }
    static boolean transfer(MinecraftServer server,FlightState s,AirshipEntity old,ServerLevel target,FlightGraph.Node node,float yaw,java.util.function.Predicate<AirshipEntity> validate){
        var data=FlightData.get(server);var snapshot=old.getContraption().writeNBT(false);var nextState=FlightData.ship(FlightData.ship(s));
        var driver=old.getControllingPlayer().map(server.getPlayerList()::getPlayer).orElse(null);
        var control=old.controllingPosition();
        var driverLocal=driver==null?null:old.toLocalVector(driver.position(),1);
        float driverYaw=driver==null?0:driver.getYRot()+net.minecraft.util.Mth.wrapDegrees(yaw-old.yaw);
        nextState.position=node.point();nextState.dimension=node.dimension();nextState.yaw=yaw;
        var next=new AirshipEntity(AirshipContent.ENTITY.get(),target);next.attach(Contraption.fromNBT(target,snapshot,false),nextState);
        // A hull may straddle the portal's chunk. Load only its actual exit footprint,
        // not the route, and immediately relinquish the temporary tickets on failure.
        var exitChunks=new LinkedHashSet<net.minecraft.world.level.ChunkPos>();
        for(var block:next.getContraption().getBlocks().values()){
            net.minecraft.world.phys.AABB box=null;
            for(int i=0;i<8;i++){var p=next.toGlobalVector(Vec3.atLowerCornerOf(block.pos()).add((i&1)==0?0:1,(i&2)==0?0:1,(i&4)==0?0:1),1);box=box==null?new net.minecraft.world.phys.AABB(p,p):box.minmax(new net.minecraft.world.phys.AABB(p,p));}
            for(int x=net.minecraft.util.Mth.floor(box.minX)>>4;x<=net.minecraft.util.Mth.floor(box.maxX)>>4;x++)for(int z=net.minecraft.util.Mth.floor(box.minZ)>>4;z<=net.minecraft.util.Mth.floor(box.maxZ)>>4;z++)exitChunks.add(new net.minecraft.world.level.ChunkPos(x,z));
        }
        for(var chunk:exitChunks){target.getChunkSource().addRegionTicket(TicketType.PORTAL,chunk,2,chunk.getMiddleBlockPosition(0));target.getChunk(chunk.x,chunk.z);}
        boolean committed=false;
        try{
        if(!validate.test(next))return false;
        // Both projections exist only within this server-thread transaction, before either can tick.
        if(!target.addFreshEntity(next))return false;
        AirshipPassengers.suspend(old,s);old.discard();
        data.graph.release(s.id);data.graph.reserveStation(node.id(),s.id);
        s.dimension=node.dimension();s.position=node.point();s.yaw=yaw;s.atNode=node.id();s.portalExitNode=node.id();s.choiceNode=s.incomingRoute=null;
        s.route=s.fromNode=s.selectedExit=s.plannedExit=s.activeBend=null;s.transition=null;s.transitionFromRoute=s.transitionToRoute=s.transitionToNode=null;
        s.distance=s.transitionDistance=s.transitionEndDistance=s.dockDistanceTotal=s.speed=0;s.motionDirection=FlightNavigation.forward(yaw);
        s.rejoining=s.rejoinAutomatic=s.requireSpaceReturn=s.pivotPass=s.awaitingThrottleRelease=false;FlightNavigation.clearInput(s);
        s.aligned=false;s.selectionLocked=false;s.message="airship.portal.arrived";s.contraption=next.getContraption().writeNBT(false);
        projections(server).put(s.id,next);AirshipPassengers.restore(next,s);
        if(driver!=null&&!driver.hasDisconnected()&&control!=null&&driverLocal!=null){
            var position=next.toGlobalVector(driverLocal,1);
            if(driver.getVehicle()==next)driver.setPos(position); // seat packets own the final rider placement
            else driver.teleportTo(target,position.x,position.y,position.z,driverYaw,driver.getXRot());
            driver.setPortalCooldown();
            if(next.startControlling(control,driver)){
                next.setControllingPlayer(driver.getUUID());
                AirshipNetwork.resumeControls(driver,next,control);
            }
        }
        data.setDirty();committed=true;return true;
        }finally{if(!committed)for(var chunk:exitChunks)target.getChunkSource().removeRegionTicket(TicketType.PORTAL,chunk,2,chunk.getMiddleBlockPosition(0));}
    }
    public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;var server=e.getServer();var data=FlightData.get(server);
        AirshipPortals.tick(server,data);
        for(var s:List.copyOf(data.ships.values())){
            var entity=projection(server,s);if(entity==null){s.stop("airship.error.dimension");continue;}
            var level=(ServerLevel)entity.level();
            var wandMotion=RelocationSessions.motion(entity);
            if(wandMotion!=null){
                var before=entity.position();RelocationSessions.moveFree(entity,wandMotion);s.position=entity.position();s.yaw=entity.yaw;entity.project(s);
                entity.setContraptionMotion(s.position.subtract(before));data.setDirty();continue;
            }
            if(level.getGameTime()-s.lastInputTick>30)FlightNavigation.clearInput(s);
            if(!thaumicomniverse.RevivalFeatures.Group.CREATE.enabled()){s.stop("airship.error.disabled");continue;}
            if(AirshipPortals.cross(server,data,s,entity))continue;
            AirshipSchedule.tick(data,s,entity);
            if(s.fuelTicks>0&&s.speed>0)s.fuelTicks--;
            if(s.fuelTicks==0&&(s.forward||s.reverse||s.dockHeld||s.automatic||s.rejoinAutomatic)&&!s.paused)refuel(s,entity);
            double multiplier=(s.fuelTicks>0?thaumicomniverse.RevivalFeatures.AIRSHIP_POWERED_SPEED_MULTIPLIER:thaumicomniverse.RevivalFeatures.AIRSHIP_SPEED_MULTIPLIER).get();
            double maximum=(s.fuelTicks>0?AllConfigs.server().trains.poweredTrainTopSpeed.getF():AllConfigs.server().trains.trainTopSpeed.getF())*multiplier/20d;
            s.acceleration=(s.fuelTicks>0?AllConfigs.server().trains.poweredTrainAcceleration.getF():AllConfigs.server().trains.trainAcceleration.getF())*multiplier/400d;
            if(entity.getPassengers().stream().anyMatch(p->p instanceof ServerPlayer)&&!level.hasChunkAt(BlockPos.containing(s.position.add(FlightNavigation.forward(s.yaw).scale(Math.max(4,maximum*8)))))){s.message="airship.wait.chunks";continue;}
            FlightNavigation.tick(data.graph,s,maximum,thaumicomniverse.RevivalFeatures.AIRSHIP_TURN_SPEED.get());
            // Entity lifecycle follows loaded chunks; the ledger keeps travelling after projection removal.
            if(!entity.isAddedToWorld()&&AirshipPassengers.hasOnlinePassenger(level,s)){
                var pos=BlockPos.containing(s.position);level.getChunkSource().addRegionTicket(TicketType.PORTAL,new net.minecraft.world.level.ChunkPos(pos),2,pos);level.getChunkAt(pos);
            }
            // Forge setPos renews a FULL/UNKNOWN ticket even for an unchanged position.
            // A FULL check would let the projection keep itself loaded forever after
            // players leave. Only an independently entity-ticking chunk hosts it.
            boolean loaded=level.isPositionEntityTicking(BlockPos.containing(s.position));
            if(!entity.isAddedToWorld()&&loaded){entity.project(s);level.addFreshEntity(entity);}
            else if(entity.isAddedToWorld()&&!loaded&&entity.getPassengers().stream().noneMatch(p->p instanceof ServerPlayer)){
                s.contraption=entity.getContraption().writeNBT(false);entity.discard();
            }
            if(!entity.isRemoved())entity.project(s);
            AirshipPassengers.restore(entity,s);
            if(server.getTickCount()%2==0&&entity.isAddedToWorld())AirshipNetwork.status(entity,s,data.graph);
        }
        if(!data.ships.isEmpty())data.setDirty();
    }
    private static void refuel(FlightState s,AirshipEntity entity){
        var structure=entity.getContraption();var power=tc4revival.aspect.TCAspects.catalog().require("potentia");
        var interfaces=structure.getBlocks().values().stream().filter(b->b.state().is(AirshipContent.FUEL.get())).map(b->b.pos().above()).distinct().sorted(Comparator.comparingLong(BlockPos::asLong)).toList();
        for(var pos:interfaces){var entry=MountedArcaneStorage.of(structure).at(pos);if(entry==null||entry.storage.extract(power,1,true)!=1)continue;
            if(entry.storage.extract(power,1,false)==1){s.fuelTicks=thaumicomniverse.RevivalFeatures.AIRSHIP_FUEL_TICKS.get();return;}
        }
    }
    public static void save(LevelEvent.Save event){
        if(!(event.getLevel() instanceof ServerLevel level))return;var data=FlightData.get(level.getServer());
        for(var entity:projections(level.getServer()).values())if(!entity.isRemoved()&&entity.getContraption()!=null){var s=data.ships.get(entity.getUUID());if(s!=null){AirshipPassengers.snapshot(entity,s);s.contraption=entity.getContraption().writeNBT(false);}}
        data.setDirty();
    }
    public static void stopped(ServerStoppedEvent e){projections.remove(e.getServer());}
}
