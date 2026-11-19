package thaumicomniverse.compat.create;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.world.phys.Vec3;
/** Bounded, persistent route with distance-based speed and monotonic ease-in/ease-out. */
public class ArcaneFlightPath {
    public static double ease(double value,int kind){
        double t=Math.max(0,Math.min(1,value));
        return switch(Math.floorMod(kind,3)){case 1->(1-Math.cos(Math.PI*t))/2;case 2->t*t*t*(t*(t*6-15)+10);default->t*t*(3-2*t);};
    }
    public static List<Vec3> smooth(List<Vec3> input){
        return fit(input);
    }
    /** Least-squares cubic Bezier with fixed endpoints and chord-length parameters. */
    public static List<Vec3> fit(List<Vec3> input){
        if(input.size()<3)return List.copyOf(input);
        var a=input.get(0);var d=input.get(input.size()-1);double total=length(input);
        if(total<1e-8)return List.of(a,d);
        double aa=0,ab=0,bb=0,distance=0;Vec3 ar=Vec3.ZERO,br=Vec3.ZERO;
        for(int i=1;i+1<input.size();i++){
            distance+=input.get(i).distanceTo(input.get(i-1));double t=distance/total,s=1-t;
            double u=3*s*s*t,v=3*s*t*t;
            var r=input.get(i).subtract(a.scale(s*s*s)).subtract(d.scale(t*t*t));
            aa+=u*u;ab+=u*v;bb+=v*v;ar=ar.add(r.scale(u));br=br.add(r.scale(v));
        }
        double det=aa*bb-ab*ab;Vec3 b,c;
        if(det>1e-9){b=ar.scale(bb).subtract(br.scale(ab)).scale(1/det);c=br.scale(aa).subtract(ar.scale(ab)).scale(1/det);}
        else { // Three samples determine a quadratic; express it as a cubic.
            var q=input.get(input.size()/2).scale(2).subtract(a.add(d).scale(.5));
            b=a.lerp(q,2d/3);c=d.lerp(q,2d/3);
        }
        var result=new ArrayList<Vec3>(65);
        for(int i=0;i<=64;i++){double t=i/64d,s=1-t;result.add(a.scale(s*s*s).add(b.scale(3*s*s*t)).add(c.scale(3*s*t*t)).add(d.scale(t*t*t)));}
        return List.copyOf(result);
    }
    public static double ejectorTicks(List<Vec3> path){
        if(path.size()<2)return 3;
        var delta=path.get(path.size()-1).subtract(path.get(0));
        return Math.max(3,new com.simibubi.create.content.logistics.depot.EntityLauncher((int)Math.round(delta.horizontalDistance()),(int)Math.round(delta.y)).getTotalFlyingTicks());
    }
    public static double length(List<Vec3> path){double n=0;for(int i=1;i<path.size();i++)n+=path.get(i-1).distanceTo(path.get(i));return n;}
    public static Vec3 sample(List<Vec3> path,double progress){
        if(path.isEmpty())return Vec3.ZERO;double distance=length(path)*Math.max(0,Math.min(1,progress));
        for(int i=1;i<path.size();i++){double d=path.get(i-1).distanceTo(path.get(i));if(distance<=d)return path.get(i-1).lerp(path.get(i),d<1e-8?0:distance/d);distance-=d;}return path.get(path.size()-1);
    }
    public static ListTag save(List<Vec3> points){var list=new ListTag();for(var point:points){var entry=new CompoundTag();entry.putDouble("X",point.x);entry.putDouble("Y",point.y);entry.putDouble("Z",point.z);list.add(entry);}return list;}
    public static List<Vec3> load(ListTag list){var result=new ArrayList<Vec3>();for(int i=0;i<Math.min(2048,list.size());i++){var p=list.getCompound(i);double x=p.getDouble("X"),y=p.getDouble("Y"),z=p.getDouble("Z");if(Double.isFinite(x)&&Double.isFinite(y)&&Double.isFinite(z)&&Math.abs(x)<30000000&&Math.abs(z)<30000000&&Math.abs(y)<2048)result.add(new Vec3(x,y,z));}return List.copyOf(result);}
}
