package thaumicomniverse.compat.create;

import java.util.*;
import com.simibubi.create.content.contraptions.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import tc4revival.content.CastingWandItem;

public class ArcaneMobileEntity extends OrientedContraptionEntity {
    private List<Vec3> path=List.of();
    private double progress;
    private double flightTicks;
    private Vec3 clientTarget;
    private int clientSteps;
    private float speed=16;
    private int easing;
    private boolean place,travelling;
    private BlockPos controller,pending,pendingCasing;
    private float pendingSpeed;
    private UUID pilot;
    private Vec3 viewOffset=Vec3.ZERO;
    private final ArrayList<Vec3> recorded=new ArrayList<>();
    public ArcaneMobileEntity(EntityType<?> type,Level level){super(type,level);}
    public static ArcaneMobileEntity assemble(ServerLevel level,BlockPos root,boolean single,BlockPos excluded)throws AssemblyException{
        var structure=new ArcaneMovingStructure(single,excluded);if(!structure.assemble(level,root))return null;
        var entity=new ArcaneMobileEntity(ArcaneCreateContent.MOBILE.get(),level);
        entity.setInitialOrientation(Direction.SOUTH);entity.startAtInitialYaw();
        entity.setContraption(structure);entity.setPos(root.getX()+.5,root.getY(),root.getZ()+.5);
        structure.removeBlocksFromWorld(level,BlockPos.ZERO);
        if(!level.addFreshEntity(entity)){entity.disassemble();return null;}
        return entity;
    }
    public boolean travelling(){return travelling||pilot!=null;}
    public BlockPos controller(){return controller;}
    public float pullSpeed(){return controller!=null&&level().getBlockEntity(controller) instanceof ArcaneMagnet.Tile magnet?Math.abs(magnet.getSpeed()):speed;}
    public boolean canRequestMagnet(BlockPos source,float rpm){return pilot==null&&(controller==null||controller.equals(source)||rpm>pullSpeed());}
    public boolean requestMagnet(BlockPos source,Vec3 destination,float rpm,int easing,boolean place){
        return requestMagnet(source,destination,rpm,easing,place,BlockPos.ZERO);
    }
    public boolean requestMagnet(BlockPos source,Vec3 destination,float rpm,int easing,boolean place,BlockPos local){
        if(!canRequestMagnet(source,rpm))return false;
        if(travelling){if(!source.equals(controller)&&(pending==null||source.equals(pending)||rpm>pendingSpeed)){pending=source;pendingSpeed=rpm;pendingCasing=local.immutable();return true;}return source.equals(controller);}
        controller=source;speed=rpm;this.easing=easing;this.place=place;path=List.of(position(),destination);progress=0;travelling=position().distanceToSqr(destination)>1e-10;return true;
    }
    public void route(List<Vec3> route){path=ArcaneFlightPath.fit(route);flightTicks=ArcaneFlightPath.ejectorTicks(path);progress=0;speed=32;place=true;travelling=path.size()>1;controller=null;}
    @Override public void lerpTo(double x,double y,double z,float yaw,float pitch,int steps,boolean teleport){
        if(teleport){super.lerpTo(x,y,z,yaw,pitch,steps,true);clientSteps=0;return;}
        clientTarget=new Vec3(x,y,z);clientSteps=Math.max(1,steps);
    }
    public void control(Player player){
        pilot=player.getUUID();controller=null;travelling=false;recorded.clear();recorded.add(position());
        var forward=player.getLookAngle();var right=right(player);var up=right.cross(forward).normalize();var delta=position().subtract(player.getEyePosition());
        viewOffset=new Vec3(delta.dot(right),delta.dot(up),delta.dot(forward));
    }
    public void prefixRecording(List<Vec3> route){recorded.clear();recorded.addAll(route);if(recorded.size()<1024)recorded.add(position());}
    private static Vec3 right(Player player){double yaw=Math.toRadians(player.getYRot());return new Vec3(Math.cos(yaw),0,Math.sin(yaw));}
    @Override protected void tickContraption(){
        prevYaw=yaw;prevPitch=pitch;
        if(level().isClientSide&&clientSteps>0){var next=position().lerp(clientTarget,1d/clientSteps--);setContraptionMotion(next.subtract(position()));setPos(next);}
        tickActors();
        if(level().isClientSide)return;
        setContraptionMotion(Vec3.ZERO);
        if(controller!=null&&travelling&&(!level().hasChunkAt(controller)||!(level().getBlockEntity(controller) instanceof ArcaneMagnet.Tile magnet)||magnet.getSpeed()==0)){
            // A lost drive releases the assembly even when an actor had stalled it.
            travelling=false;pending=null;disassemble();return;
        }
        if(!ArcaneContraptions.enabled()||isStalled())return;
        if(pilot!=null){
            var player=level().getPlayerByUUID(pilot);
            if(player==null||!player.isAlive()||!player.isUsingItem()||!(player.getUseItem().getItem() instanceof CastingWandItem)||!player.getUseItem().hasTag()||!getUUID().equals(player.getUseItem().getTag().hasUUID("OmniFlyingDepot")?player.getUseItem().getTag().getUUID("OmniFlyingDepot"):null)){finishControl();return;}
            var right=right(player);var forward=player.getLookAngle();var up=right.cross(forward).normalize();
            var target=player.getEyePosition().add(right.scale(viewOffset.x)).add(up.scale(viewOffset.y)).add(forward.scale(viewOffset.z));
            var delta=target.subtract(position());if(delta.length()>1)delta=delta.normalize();
            if(moveSafely(delta)&&recorded.size()<1024&&recorded.get(recorded.size()-1).distanceToSqr(position())>.04)recorded.add(position());
            return;
        }
        if(!travelling)return;
        if(controller!=null)speed=pullSpeed();
        double length=Math.max(.01,ArcaneFlightPath.length(path));
        double step=Math.min(256,Math.max(1,speed))/512d/Math.max(1,length);
        if(controller==null){
            boolean powered=tc4revival.content.VisNetwork.drain(level(),blockPosition(),tc4revival.aspect.TCAspects.catalog().require("aer"),1)==1;
            step=1/(Math.max(3,flightTicks)*(powered?.5:1.5));
        }
        double next=Math.min(1,progress+step);
        var target=ArcaneFlightPath.sample(path,ArcaneFlightPath.ease(next,easing));
        if(!moveSafely(target.subtract(position())))return;
        progress=next;
        if(progress<1)return;
        travelling=false;
        if(pending!=null){var nextController=pending;var local=pendingCasing;pending=null;pendingCasing=null;pendingSpeed=0;if(level().hasChunkAt(nextController)&&level().getBlockEntity(nextController) instanceof ArcaneMagnet.Tile magnet&&(local==null?magnet.attract(this):magnet.attract(this,local)))return;}
        if(place)disassemble();
    }
    private boolean moveSafely(Vec3 delta){
        if(delta.lengthSqr()<1e-10)return true;
        int steps=Math.max(1,(int)Math.ceil(delta.length()*8));
        for(int i=1;i<=steps;i++)if(!clearAt(position().add(delta.scale(i/(double)steps))))return false;
        setContraptionMotion(delta);setPos(position().add(delta));return true;
    }
    public boolean clearAt(Vec3 target){
        var anchor=target.subtract(.5,0,.5);
        for(var block:contraption.getBlocks().values()){
            var pos=BlockPos.containing(Vec3.atLowerCornerOf(block.pos()).add(anchor));
            if(level().isOutsideBuildHeight(pos)||!level().hasChunkAt(pos))return false;
        }return true;
    }
    public void finishControl(){
        if(pilot==null)return;var player=level().getPlayerByUUID(pilot);pilot=null;
        if(player!=null){for(var hand:net.minecraft.world.InteractionHand.values()){var stack=player.getItemInHand(hand);if(stack.hasTag()){stack.getTag().remove("OmniFlyingDepot");stack.getTag().remove("OmniFlyingUse");}}}
        var pos=BlockPos.containing(getAnchorVec().add(.5,.5,.5));var snapped=Vec3.atBottomCenterOf(pos);
        if(!clearAt(snapped)){travelling=false;return;}
        setPos(snapped);recorded.add(snapped);
        var info=contraption.getBlocks().get(BlockPos.ZERO);
        if(info!=null&&info.nbt()!=null){var tag=info.nbt();if(!tag.contains("Origin"))tag.putLong("Origin",contraption.anchor.asLong());tag.putLong("Destination",pos.asLong());tag.put("FlightPath",ArcaneFlightPath.save(ArcaneFlightPath.smooth(recorded)));}
        disassemble();
    }
    @Override protected void writeAdditional(CompoundTag tag,boolean spawn){
        super.writeAdditional(tag,spawn);tag.put("ArcanePath",ArcaneFlightPath.save(path));tag.putDouble("ArcaneProgress",progress);tag.putDouble("ArcaneFlightTicks",flightTicks);tag.putFloat("ArcaneSpeed",speed);tag.putInt("ArcaneEasing",easing);tag.putBoolean("ArcanePlace",place);tag.putBoolean("ArcaneTravel",travelling);
        if(controller!=null)tag.putLong("ArcaneController",controller.asLong());if(pending!=null){tag.putLong("ArcanePending",pending.asLong());tag.putFloat("ArcanePendingSpeed",pendingSpeed);if(pendingCasing!=null)tag.putLong("ArcanePendingCasing",pendingCasing.asLong());}
        if(pilot!=null){tag.putUUID("ArcanePilot",pilot);tag.put("ArcaneRecording",ArcaneFlightPath.save(recorded));tag.put("ArcaneView",ArcaneFlightPath.save(List.of(viewOffset)));}
    }
    @Override protected void readAdditional(CompoundTag tag,boolean spawn){
        super.readAdditional(tag,spawn);path=ArcaneFlightPath.load(tag.getList("ArcanePath",Tag.TAG_COMPOUND));progress=Math.max(0,Math.min(1,tag.getDouble("ArcaneProgress")));speed=Math.max(1,Math.min(256,tag.getFloat("ArcaneSpeed")));easing=tag.getInt("ArcaneEasing");place=tag.getBoolean("ArcanePlace");travelling=tag.getBoolean("ArcaneTravel")&&path.size()>1;
        controller=tag.contains("ArcaneController")?BlockPos.of(tag.getLong("ArcaneController")):null;pending=tag.contains("ArcanePending")?BlockPos.of(tag.getLong("ArcanePending")):null;pendingSpeed=tag.getFloat("ArcanePendingSpeed");
        pendingCasing=tag.contains("ArcanePendingCasing")?BlockPos.of(tag.getLong("ArcanePendingCasing")):null;
        flightTicks=tag.contains("ArcaneFlightTicks")?Math.max(3,tag.getDouble("ArcaneFlightTicks")):ArcaneFlightPath.ejectorTicks(path);
        pilot=tag.hasUUID("ArcanePilot")?tag.getUUID("ArcanePilot"):null;recorded.clear();recorded.addAll(ArcaneFlightPath.load(tag.getList("ArcaneRecording",Tag.TAG_COMPOUND)));var offsets=ArcaneFlightPath.load(tag.getList("ArcaneView",Tag.TAG_COMPOUND));viewOffset=offsets.isEmpty()?Vec3.ZERO:offsets.get(0);
    }
}
