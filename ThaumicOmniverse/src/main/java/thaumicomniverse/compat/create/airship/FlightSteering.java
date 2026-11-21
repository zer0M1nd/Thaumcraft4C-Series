package thaumicomniverse.compat.create.airship;

import java.util.function.DoubleFunction;
import net.minecraft.world.phys.Vec3;

/** The angular budget is in 3D radians per tick, independent of powered linear speed. */
public final class FlightSteering {
    private FlightSteering(){}
    public static double angle(Vec3 a,Vec3 b){return Math.acos(Math.max(-1,Math.min(1,a.normalize().dot(b.normalize()))));}
    public static boolean sameDirection(Vec3 a,Vec3 b){return a.lengthSqr()>1e-12&&b.lengthSqr()>1e-12&&a.normalize().dot(b.normalize())>=1-1e-12;}
    public static Vec3 rotate(Vec3 from,Vec3 to,double radians){
        from=from.normalize();to=to.normalize();double angle=angle(from,to);
        if(angle<=radians+1e-10)return to;
        Vec3 axis=from.cross(to).normalize();
        if(axis.lengthSqr()<.1){axis=from.cross(new Vec3(0,1,0)).normalize();if(axis.lengthSqr()<.1)axis=new Vec3(1,0,0);}
        return from.scale(Math.cos(radians)).add(axis.cross(from).scale(Math.sin(radians))).add(axis.scale(axis.dot(from)*(1-Math.cos(radians)))).normalize();
    }
    public static double advance(DoubleFunction<Vec3> tangent,double start,double wanted,double radians){
        if(wanted<=0)return 0;
        // Integrate total turn rather than just comparing endpoints (an S bend can end in the same direction).
        int steps=Math.max(8,Math.min(512,(int)Math.ceil(wanted/.025)));double last=0,used=0;Vec3 direction=tangent.apply(start);
        for(int i=1;i<=steps;i++){
            double next=wanted*i/steps;Vec3 v=tangent.apply(start+next);double turn=angle(direction,v);
            if(used+turn>radians){
                double lo=last,hi=next;
                for(int k=0;k<20;k++){double mid=(lo+hi)/2;if(used+angle(direction,tangent.apply(start+mid))<=radians)lo=mid;else hi=mid;}
                return lo;
            }used+=turn;last=next;direction=v;
        }return wanted;
    }
}
