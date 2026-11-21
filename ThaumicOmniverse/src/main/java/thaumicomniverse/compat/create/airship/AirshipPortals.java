package thaumicomniverse.compat.create.airship;

import java.util.*;
import net.minecraft.BlockUtil;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import com.simibubi.create.content.contraptions.ContraptionWorld;

/** Vanilla portal lookup, validated apertures and short-lived portal tickets; no route chunk forcing. */
public final class AirshipPortals {
    private AirshipPortals(){}
    record Aperture(BlockPos min,Direction.Axis axis,int width,int height){
        boolean contains(BlockPos p){return p.getY()>=min.getY()&&p.getY()<min.getY()+height&&p.get(axis)>=min.get(axis)&&p.get(axis)<min.get(axis)+width&&p.get(axis==Direction.Axis.X?Direction.Axis.Z:Direction.Axis.X)==min.get(axis==Direction.Axis.X?Direction.Axis.Z:Direction.Axis.X);}
        CompoundTag save(long time){var n=new CompoundTag();n.putLong("Min",min.asLong());n.putString("Axis",axis.getName());n.putInt("Width",width);n.putInt("Height",height);n.putLong("Checked",time);return n;}
    }
    private static ServerLevel level(MinecraftServer server,FlightGraph.Node node){return server.getLevel(ResourceKey.create(Registries.DIMENSION,node.dimension()));}
    private static Aperture aperture(ServerLevel level,BlockPos anchor){
        var state=level.getBlockState(anchor);if(!state.is(Blocks.NETHER_PORTAL))return null;
        var axis=state.getValue(NetherPortalBlock.AXIS);if(!new PortalShape(level,anchor,axis).isComplete())return null;
        var rect=BlockUtil.getLargestRectangleAround(anchor,axis,21,Direction.Axis.Y,21,p->level.getBlockState(p)==state);
        return new Aperture(rect.minCorner,axis,rect.axis1Size,rect.axis2Size);
    }
    /** Loaded blocks invalidate cached topology immediately; expensive counterpart lookup is budgeted. */
    public static void tick(MinecraftServer server,FlightData data){
        if(server.getTickCount()%20!=0)return;int refreshed=0;
        for(var node:data.graph.nodes()){
            if(node.station()){data.graph.clearPortal(node.id());data.portalCache.remove(node.id().toString());continue;}
            var level=level(server,node);if(level==null||!level.hasChunkAt(node.anchor()))continue;
            boolean portal=level.getBlockState(node.anchor()).is(Blocks.NETHER_PORTAL);
            if(portal)data.graph.markPortal(node.id());
            if(!data.graph.portal(node.id()))continue;
            if(!portal){data.graph.clearPortal(node.id());data.portalCache.remove(node.id().toString());continue;}
            var cached=data.portalCache.getCompound(node.id().toString());
            if(refreshed<2&&(!cached.contains("PairChecked")||server.overworld().getGameTime()-cached.getLong("PairChecked")>=600)){refresh(server,data,node);refreshed++;}
        }
    }
    private static FlightGraph.Node refresh(MinecraftServer server,FlightData data,FlightGraph.Node source){
        if(source.station())return null;
        var from=level(server,source);if(from==null)return null;
        var shape=aperture(from,source.anchor());
        if(shape==null){data.graph.unlinkPortal(source.id());data.portalCache.remove(source.id().toString());return null;}
        long time=server.overworld().getGameTime();var sourceCache=shape.save(time);sourceCache.putLong("PairChecked",time);data.portalCache.put(source.id().toString(),sourceCache);data.graph.markPortal(source.id());
        var destinationKey=from.dimension().equals(Level.OVERWORLD)?Level.NETHER:from.dimension().equals(Level.NETHER)?Level.OVERWORLD:null;
        var to=destinationKey==null?null:server.getLevel(destinationKey);if(to==null){data.graph.unlinkPortal(source.id());return null;}
        double scale=DimensionType.getTeleportationScale(from.dimensionType(),to.dimensionType());
        var search=to.getWorldBorder().clampToBounds(source.point().x*scale,source.point().y,source.point().z*scale);
        // Same POI ordering/radii as vanilla PortalForcer, without its travel ticket side effect.
        // Background topology checks must not continuously renew tickets for idle portal pairs.
        var poi=to.getPoiManager();int radius=to.dimension().equals(Level.NETHER)?16:128;poi.ensureLoadedAndValid(to,search,radius);
        var found=poi.getInSquare(t->t.is(PoiTypes.NETHER_PORTAL),search,radius,PoiManager.Occupancy.ANY)
            .filter(p->to.getWorldBorder().isWithinBounds(p.getPos()))
            .sorted(Comparator.<PoiRecord>comparingDouble(p->p.getPos().distSqr(search)).thenComparingInt(p->p.getPos().getY()))
            .filter(p->to.getBlockState(p.getPos()).hasProperty(BlockStateProperties.HORIZONTAL_AXIS)).findFirst();
        if(found.isEmpty()){data.graph.unlinkPortal(source.id());return null;}
        var targetShape=aperture(to,found.get().getPos());if(targetShape==null){data.graph.unlinkPortal(source.id());return null;}
        // Match nodes inside the actual vanilla-selected rectangle, independent of local anchor cell.
        var target=data.graph.nodes().stream().filter(n->!n.station()&&n.dimension().equals(to.dimension().location())&&targetShape.contains(n.anchor())).min(Comparator.comparing(FlightGraph.Node::id)).orElse(null);
        if(target==null){data.graph.unlinkPortal(source.id());return null;}
        data.graph.linkPortal(source.id(),target.id());var targetCache=targetShape.save(time);var previous=data.portalCache.getCompound(target.id().toString());if(previous.contains("PairChecked"))targetCache.putLong("PairChecked",previous.getLong("PairChecked"));data.portalCache.put(target.id().toString(),targetCache);data.setDirty();return target;
    }
    private static void ticket(ServerLevel level,BlockPos pos){level.getChunkSource().addRegionTicket(TicketType.PORTAL,new ChunkPos(pos),3,pos);level.getChunkAt(pos);}
    private static boolean fits(AirshipEntity ship,Aperture portal){
        var world=new ContraptionWorld(ship.level(),ship.getContraption());
        for(var block:ship.getContraption().getBlocks().values()){
            var boxes=block.state().getShape(world,block.pos()).toAabbs();
            for(var box:boxes)for(int i=0;i<8;i++){
                var point=ship.toGlobalVector(new Vec3((i&1)==0?box.minX:box.maxX,(i&2)==0?box.minY:box.maxY,(i&4)==0?box.minZ:box.maxZ).add(Vec3.atLowerCornerOf(block.pos())),1);
                if(point.y<portal.min.getY()-1e-5||point.y>portal.min.getY()+portal.height+1e-5||point.get(portal.axis)<portal.min.get(portal.axis)-1e-5||point.get(portal.axis)>portal.min.get(portal.axis)+portal.width+1e-5)return false;
            }
        }return true;
    }
    /** Crossing is the selected enter-waypoint action; ordinary passing routes remain available. */
    public static boolean cross(MinecraftServer server,FlightData data,FlightState state,AirshipEntity ship){
        if(state.atNode==null||!state.dockSelected||!data.graph.portal(state.atNode)||state.atNode.equals(state.portalExitNode))return false;
        if(server.getTickCount()%20!=0)return true;
        var source=data.graph.node(state.atNode);var from=level(server,source);from.getChunkAt(source.anchor());
        var target=refresh(server,data,source);
        if(target==null){state.stop("airship.error.portal_pair");return true;}
        var to=level(server,target);to.getChunkAt(target.anchor());
        var a=aperture(from,source.anchor());var b=aperture(to,target.anchor());
        if(a==null||b==null){state.stop("airship.error.portal_invalid");return true;}
        if(!fits(ship,a)){state.stop("airship.error.portal_size");return true;}
        if(data.graph.stationOwner(target.id())!=null&&!state.id.equals(data.graph.stationOwner(target.id()))){state.stop("airship.wait.signal");return true;}
        float yaw=state.yaw+(a.axis==b.axis?0:90);
        state.message="airship.error.portal_spawn";
        if(!AirshipManager.transfer(server,state,ship,to,target,yaw,e->{if(!fits(e,b)){state.message="airship.error.portal_size";return false;}if(!thaumicomniverse.compat.create.RelocationSessions.clear(e,Vec3.ZERO,1e-5)){state.message="airship.error.portal_exit_obstructed";return false;}return true;})){state.stop(state.message);return true;}
        ticket(to,target.anchor());return true;
    }
}
