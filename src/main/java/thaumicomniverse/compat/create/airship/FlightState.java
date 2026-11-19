package thaumicomniverse.compat.create.airship;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Authoritative logical vehicle; its render/collision entity may be absent in unloaded chunks. */
public final class FlightState {
    public final UUID id;
    public ResourceLocation dimension;
    public Vec3 position;
    public float yaw,pitch;
    public Direction initialFacing=Direction.SOUTH;
    public double speed,distance;
    /** Blocks/tick squared; refreshed from the matching Create train configuration each tick. */
    public transient double acceleration=3d/1600;
    public UUID atNode,route,fromNode,selectedExit,plannedExit;
    public UUID incomingRoute,choiceNode;
    public UUID transitionFromRoute,transitionToRoute,transitionToNode;
    public UUID activeBend;
    public Vec3 motionDirection;
    public boolean freeControl,requireSpaceReturn,rotateAtJoin,pivotPass;
    public double dockDistanceTotal;
    public transient boolean leftHeld,rightHeld,downHeld,modeGestureLatched;
    public transient int modeGestureTicks;
    public transient int rejoinRetryTicks;
    public double transitionDistance,transitionEndDistance;
    public FlightPath transition;
    public boolean dockSelected,selectionLocked,aligned,automatic,paused;
    public boolean rejoining,rejoinAutomatic;
    public int fuelTicks,scheduleIndex,waitTicks;
    public int alignmentTicks;
    public float alignmentStartYaw,alignmentTargetYaw;
    public double throttle=1;
    public CompoundTag contraption=new CompoundTag(),schedule=new CompoundTag(),waitState=new CompoundTag();
    public String message="";
    public String scheduleTitle="";
    public CompoundTag passengers=new CompoundTag();
    public UUID portalExitNode;
    // Input is deliberately not persisted: no stale key can accelerate an unattended vehicle after restart.
    public transient boolean forward,reverse,dockHeld,awaitingThrottleRelease,reverseTravel;
    public transient long lastInputTick;
    public transient int previousChoice;
    public FlightState(UUID id,ResourceLocation dimension,Vec3 position){this.id=id;this.dimension=dimension;this.position=position;}
    public void stop(String translation){speed=0;forward=reverse=false;message=translation;}
}
