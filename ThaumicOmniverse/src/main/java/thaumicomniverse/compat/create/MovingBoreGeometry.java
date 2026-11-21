package thaumicomniverse.compat.create;

import java.util.*;
import net.minecraft.world.phys.Vec3;

/** Build the drilling disk in the bore's rotated plane, never by snapping its axis to a Direction. */
public class MovingBoreGeometry {
    public record Ray(Vec3 from,Vec3 to){}
    public static List<Ray> rays(Vec3 origin,Vec3 facing,int radius){
        if(radius<0||radius>32||facing.lengthSqr()<1e-10)throw new IllegalArgumentException("Invalid bore geometry");
        var normal=facing.normalize();var guide=Math.abs(normal.y)>.9?new Vec3(1,0,0):new Vec3(0,1,0);
        var u=normal.cross(guide).normalize();var v=normal.cross(u).normalize();
        var result=new ArrayList<Ray>();
        for(int x=-radius;x<=radius;x++)for(int y=-radius;y<=radius;y++){
            if(x*x+y*y>radius*radius)continue;
            var from=origin.add(normal.scale(1.5)).add(u.scale(x)).add(v.scale(y));
            result.add(new Ray(from,from.add(normal.scale(32))));
        }return List.copyOf(result);
    }
}
