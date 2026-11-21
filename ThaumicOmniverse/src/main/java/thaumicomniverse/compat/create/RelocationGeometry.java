package thaumicomniverse.compat.create;

import java.util.*;
import net.minecraft.world.phys.*;

/** Oriented collision and minimum axis separation, independent of a live world. */
public abstract class RelocationGeometry {
    protected static final Vec3[] WORLD={new Vec3(1,0,0),new Vec3(0,1,0),new Vec3(0,0,1)};
    protected static final double EPS=1e-7;
    public record Box(Vec3 center,Vec3[] axes,double[] half,AABB bounds) {
        public Box move(Vec3 v){return new Box(center.add(v),axes,half,bounds.move(v));}
        double radius(Vec3 n){double r=0;for(int i=0;i<3;i++)r+=half[i]*Math.abs(axes[i].dot(n));return r;}
    }
    public static Box axisAligned(AABB b){return new Box(b.getCenter(),WORLD,new double[]{b.getXsize()/2,b.getYsize()/2,b.getZsize()/2},b);}
    private static List<Vec3> normals(Box a,Box b){
        var normals=new ArrayList<Vec3>(15);Collections.addAll(normals,a.axes);Collections.addAll(normals,b.axes);
        for(var x:a.axes)for(var y:b.axes){var n=x.cross(y);if(n.lengthSqr()>1e-12)normals.add(n.normalize());}return normals;
    }
    public static boolean overlaps(Box a,Box b){
        if(!a.bounds.intersects(b.bounds))return false;
        var delta=b.center.subtract(a.center);
        for(var n:normals(a,b))if(Math.abs(delta.dot(n))>=a.radius(n)+b.radius(n)-EPS)return false;
        return true;
    }
    /** Signed translations along each world axis, to the first separating plane. */
    public static List<Vec3> exits(Box a,Box b){
        var result=new ArrayList<Vec3>();var delta=b.center.subtract(a.center);var normals=normals(a,b);
        for(var axis:WORLD)for(int sign:new int[]{-1,1}){
            var direction=axis.scale(sign);double distance=Double.POSITIVE_INFINITY;
            for(var n:normals){double speed=direction.dot(n);if(Math.abs(speed)<1e-10)continue;
                double radius=a.radius(n)+b.radius(n),offset=delta.dot(n);
                double exit=(offset+Math.copySign(radius,speed))/speed;
                if(exit>=0)distance=Math.min(distance,exit);
            }
            // Exact contact is valid. Padding the exit would penetrate the opposite
            // wall of a one-block opening and make an otherwise valid fit impossible.
            if(Double.isFinite(distance))result.add(direction.scale(distance));
        }return result;
    }
    public record Contact(Box moving,Box obstacle){}
    public static Vec3 correction(List<Box> moving,List<Box> obstacles){
        return solve(offset->{for(var original:moving){var box=original.move(offset);for(var obstacle:obstacles)if(overlaps(box,obstacle))return new Contact(box,obstacle);}return null;});
    }
    protected static Vec3 solve(java.util.function.Function<Vec3,Contact> contact){
        var candidates=new PriorityQueue<Vec3>(Comparator.comparingDouble(Vec3::lengthSqr));candidates.add(Vec3.ZERO);
        var visited=new HashSet<String>();
        for(int work=0;work<2048&&!candidates.isEmpty();work++){
            var offset=candidates.remove();String key=Math.round(offset.x*1e7)+":"+Math.round(offset.y*1e7)+":"+Math.round(offset.z*1e7);
            if(!visited.add(key))continue;var hit=contact.apply(offset);if(hit==null)return offset;
            for(var exit:exits(hit.moving,hit.obstacle)){
                var next=offset.add(exit);if(next.lengthSqr()<=16)candidates.add(next);
            }
        }return Vec3.ZERO;
    }
    protected RelocationGeometry(){}
}
