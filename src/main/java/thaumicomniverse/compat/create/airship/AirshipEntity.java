package thaumicomniverse.compat.create.airship;

import java.util.Collection;
import com.simibubi.create.content.contraptions.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Ephemeral world projection of the saved vehicle. Never separately saved in a chunk. */
public final class AirshipEntity extends OrientedContraptionEntity {
    private static final EntityDataAccessor<Float> TURN=SynchedEntityData.defineId(AirshipEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SPEED=SynchedEntityData.defineId(AirshipEntity.class,EntityDataSerializers.FLOAT);
    private Vec3 clientTarget;private int clientSteps;
    private final FlightPoseTimeline poses=new FlightPoseTimeline();
    private boolean receivedPose;
    private double clientSpeed;
    public double displayedSpeed(){return clientSpeed;}
    // Authoritative airship poses contain yaw only; never retain a native contraption pitch during alignment.
    public void receivePose(FlightPoseTimeline.Pose pose){pitch=prevPitch=0;receivedPose=true;clientSpeed=pose.speed();poses.accept(pose);}
    public void detachForRelocation(){var s=state();if(s!=null){FlightReattachment.detach(FlightData.get(((ServerLevel)level()).getServer()).graph,s,"airship.rejoin.detached");s.forward=s.reverse=s.dockHeld=false;}}
    public void relocationPose(){var s=state();if(s!=null){s.position=position();s.yaw=yaw;project(s);}}
    public boolean turn(float degrees){float before=yaw;int steps=Math.max(1,(int)Math.ceil(Math.abs(degrees)/3));
        for(int i=1;i<=steps;i++){yaw=before+degrees*i/steps;if(!thaumicomniverse.compat.create.RelocationSessions.clear(this,Vec3.ZERO)){yaw=before;return false;}}
        relocationPose();return true;
    }
    public boolean doorsOpen(com.simibubi.create.content.contraptions.behaviour.MovementContext context){
        var s=state();if(s==null)return false;var graph=FlightData.get(((ServerLevel)level()).getServer()).graph;
        if(!AirshipManager.isDocked(graph,s)||s.alignmentTicks>0)return false;
        var node=graph.node(s.atNode);var at=level().getBlockEntity(node.block());
        var mode=at instanceof AirshipBlocks.NodeTile tile?tile.doorControl:com.simibubi.create.content.decoration.slidingDoor.DoorControl.ALL;
        if(mode==com.simibubi.create.content.decoration.slidingDoor.DoorControl.ALL)return true;
        if(mode==com.simibubi.create.content.decoration.slidingDoor.DoorControl.NONE)return false;
        var facing=context.state.getValue(net.minecraft.world.level.block.DoorBlock.FACING);
        var direction=(Vec3)context.rotation.apply(Vec3.atLowerCornerOf(facing.getNormal()));
        return mode.matches(net.minecraft.core.Direction.getNearest(direction.x,direction.y,direction.z));
    }
    boolean suspendingPassengers;
    private BlockPos controllingPosition;
    public BlockPos controllingPosition(){return controllingPosition;}
    public AirshipEntity(EntityType<?> type,Level level){
        super(type,level);
        // The client advances authoritative pose snapshots in tickContraption.
        // Entity Culling respects this vanilla flag: skipping an off-camera tick
        // would leave the moving ship at an old position indefinitely.
        noCulling=true;
    }
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(TURN,0f);entityData.define(SPEED,0f);}
    public void attach(Contraption structure,FlightState s){setUUID(s.id);setInitialOrientation(s.initialFacing);startAtYaw(s.yaw);setContraption(structure);setPos(s.position);entityData.set(TURN,s.yaw);}
    public FlightState state(){return level() instanceof ServerLevel sl?FlightData.get(sl.getServer()).ships.get(getUUID()):null;}
    public void project(FlightState s){var motion=s.position.subtract(position());setContraptionMotion(motion);if(motion.lengthSqr()>0)setPos(s.position);prevYaw=yaw;yaw=s.yaw;entityData.set(TURN,yaw);entityData.set(SPEED,(float)s.speed);if(isAddedToWorld())AirshipNetwork.pose(this,s);}
    @Override public void lerpTo(double x,double y,double z,float yaw,float pitch,int steps,boolean teleport){clientTarget=new Vec3(x,y,z);clientSteps=Math.max(1,steps);}
    @Override protected void tickContraption(){
        if(level().isClientSide){
            prevYaw=yaw;
            var pose=poses.next();
            if(pose!=null){
                var next=FlightInterpolation.next(position(),pose.position(),1,pose.speed());
                yaw=pose.yaw();setContraptionMotion(next.subtract(position()));setPos(next);
            }else if(!receivedPose&&clientTarget!=null){
                yaw=Mth.rotLerp(1f/Math.max(1,clientSteps),yaw,entityData.get(TURN));
                // A render-thread stall can deliver several seconds of movement together.
                // Jumping to it in three ticks moves the deck out from under Create's
                // surface collider. Continue bounded catch-up even after the packet's
                // interpolation window ends; normal configured speeds remain attainable.
                var next=FlightInterpolation.next(position(),clientTarget,clientSteps,entityData.get(SPEED));
                clientSteps=Math.max(1,clientSteps-1);setContraptionMotion(next.subtract(position()));setPos(next);
            }else setContraptionMotion(Vec3.ZERO);
        }
        tickActors();
    }
    @Override public boolean startControlling(BlockPos local,Player player){
        if(player==null||player.isSpectator()||getContraption()==null||!validControl(local,player))return false;
        controllingPosition=local.immutable();
        if(!level().isClientSide){var s=state();if(s==null)return false;s.automatic=false;s.paused=false;s.message="";if(!s.selectionLocked)s.choiceNode=null;s.lastInputTick=level().getGameTime();}
        return true;
    }
    private boolean validControl(BlockPos local,Player player){var info=getContraption().getBlocks().get(local);return info!=null&&info.state().is(AirshipContent.CONTROLS.get())&&toGlobalVector(local.getCenter(),1).distanceToSqr(player.position())<=64;}
    @Override public boolean handlePlayerInteraction(Player player,BlockPos local,net.minecraft.core.Direction side,net.minecraft.world.InteractionHand hand){
        var block=getContraption().getBlocks().get(local);
        if(block!=null&&block.state().is(thaumicomniverse.compat.create.ArcaneCreateContent.BURNER.get())&&toGlobalVector(local.getCenter(),1).distanceToSqr(player.position())<=64&&AirshipSchedule.interact(this,player,hand))return true;
        return super.handlePlayerInteraction(player,local,side,hand);
    }
    @Override public boolean control(BlockPos local,Collection<Integer> keys,Player player){
        if(level().isClientSide)return true;
        if(player.isSpectator()||!getControllingPlayer().filter(player.getUUID()::equals).isPresent()||!validControl(local,player))return false;
        controllingPosition=local.immutable();var s=state();if(s==null)return false;FlightNavigation.manualInput(s,keys.contains(0),keys.contains(1),keys.contains(4));s.lastInputTick=level().getGameTime();
        s.leftHeld=keys.contains(2);s.rightHeld=keys.contains(3);s.downHeld=keys.contains(5);
        int choice=(keys.contains(3)?1:0)-(keys.contains(2)?1:0);
        if(choice!=0&&choice!=s.previousChoice)FlightNavigation.choose(FlightData.get(((ServerLevel)level()).getServer()).graph,s,choice);
        s.previousChoice=choice;return true;
    }
    @Override public void stopControlling(BlockPos p){controllingPosition=null;super.stopControlling(p);var s=state();if(s!=null)FlightNavigation.clearInput(s);}
    public boolean canDisassemble(){
        var transform=makeStructureTransform();
        for(var info:getContraption().getBlocks().values()){
            var pos=transform.apply(info.pos());
            if(!level().hasChunkAt(pos)||level().isOutsideBuildHeight(pos)||!level().getWorldBorder().isWithinBounds(pos)||!level().getBlockState(pos).canBeReplaced()||level().getBlockEntity(pos)!=null)return false;
        }return true;
    }
    @Override protected void readAdditional(CompoundTag tag,boolean spawn){super.readAdditional(tag,spawn);entityData.set(TURN,yaw);}
    @Override public void remove(RemovalReason reason){
        if(!level().isClientSide&&getContraption()!=null&&!getContraption().disassembled){var s=state();if(s!=null){AirshipPassengers.suspend(this,s);s.contraption=getContraption().writeNBT(false);}}
        super.remove(reason);
    }
    @Override protected void removePassenger(Entity passenger){
        var s=state();if(s!=null&&!suspendingPassengers&&!s.passengers.getCompound(passenger.getStringUUID()).getBoolean("Offline"))s.passengers.remove(passenger.getStringUUID());
        super.removePassenger(passenger);
    }
    @Override public boolean canChangeDimensions(){return false;}
}
