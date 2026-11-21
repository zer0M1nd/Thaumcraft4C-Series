package thaumicomniverse.compat.create.airship;

import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightPoseTimelineTest extends FlightTestConfig {
    private FlightPoseTimeline.Pose pose(long tick,float yaw){return new FlightPoseTimeline.Pose(tick,new Vec3(tick*.3,80,0),yaw,.3);}
    @Test void regularSnapshotsMoveExactlyOncePerClientTick(){
        var timeline=new FlightPoseTimeline();FlightPoseTimeline.Pose previous=null;
        for(int i=0;i<100;i++){timeline.accept(pose(i,i));var next=timeline.next();if(next!=null&&previous!=null){assertEquals(.3,next.position().x-previous.position().x,1e-8);assertEquals(1,next.yaw()-previous.yaw(),1e-5);}previous=next;}
    }
    @Test void delayedSnapshotsRemainMonotonicAndAngleUsesShortestArc(){
        var timeline=new FlightPoseTimeline();timeline.accept(pose(0,179));timeline.accept(pose(2,-179));timeline.accept(pose(4,-177));
        var previous=timeline.next();for(int i=0;i<4;i++){var next=timeline.next();assertTrue(next.position().x>=previous.position().x);assertTrue(Math.abs(Mth.wrapDegrees(next.yaw()-previous.yaw()))<5);previous=next;}
        timeline.accept(pose(1,180));var next=timeline.next();assertTrue(next.position().x>=previous.position().x,"late older packets cannot rewind");
        timeline.accept(pose(20,-160));next=timeline.next();assertTrue(next.position().x-previous.position().x<=.375001,"bounded catch-up");
    }
}
