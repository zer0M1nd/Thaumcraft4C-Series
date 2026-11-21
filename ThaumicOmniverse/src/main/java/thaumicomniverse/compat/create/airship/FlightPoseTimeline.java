package thaumicomniverse.compat.create.airship;

import java.util.*;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Position and rotation share one server clock and one short interpolation buffer. */
public final class FlightPoseTimeline {
    public record Pose(long tick,Vec3 position,float yaw,double speed){}
    private final NavigableMap<Long,Pose> samples=new TreeMap<>();
    private double cursor=Double.NaN;
    public void accept(Pose pose){samples.put(pose.tick,pose);while(samples.size()>32)samples.pollFirstEntry();}
    public Pose next(){
        if(samples.isEmpty())return null;
        if(Double.isNaN(cursor)){if(samples.size()<3)return null;cursor=samples.lastKey()-2;}
        else cursor=Math.min(samples.lastKey(),cursor+(samples.lastKey()-cursor>6?1.25:1));
        var lower=samples.floorEntry((long)Math.floor(cursor));var upper=samples.ceilingEntry((long)Math.ceil(cursor));
        if(lower==null)lower=samples.firstEntry();if(upper==null)upper=samples.lastEntry();
        var a=lower.getValue();var b=upper.getValue();double t=a.tick==b.tick?0:Mth.clamp((cursor-a.tick)/(b.tick-a.tick),0,1);
        var result=new Pose((long)cursor,a.position.lerp(b.position,t),Mth.rotLerp((float)t,a.yaw,b.yaw),Mth.lerp(t,a.speed,b.speed));
        samples.headMap(lower.getKey(),false).clear();return result;
    }
}
