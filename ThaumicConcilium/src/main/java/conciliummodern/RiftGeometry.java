package conciliummodern;

import java.util.*;
import net.minecraft.world.phys.Vec3;

/** Original seeded, two-sided random walk; no legacy OpenGL tessellator dependency. */
public record RiftGeometry(List<Vec3> points,List<Float> widths) {
    public static RiftGeometry create(int size,int seed) {
        var points=new ArrayList<Vec3>();var widths=new ArrayList<Float>();var random=new Random(seed);
        var right=new Vec3(random.nextGaussian(),random.nextGaussian(),random.nextGaussian()).normalize();var left=right.scale(-1);
        var r=Vec3.ZERO;var l=Vec3.ZERO;int steps=(int)Math.ceil(size/3f);float girth=size/300f;
        float decrement=steps==0?0:girth/steps;
        for(int i=0;i<steps;i++) {
            girth-=decrement;right=right.xRot((float)(random.nextGaussian()*.33)).yRot((float)(random.nextGaussian()*.33));
            r=r.add(right.scale(.2));points.add(r);widths.add(Math.max(0,girth));
            left=left.xRot((float)(random.nextGaussian()*.33)).yRot((float)(random.nextGaussian()*.33));
            l=l.add(left.scale(.2));points.add(0,l);widths.add(0,Math.max(0,girth));
        }
        points.add(r.add(right.scale(.1)));widths.add(0f);points.add(0,l.add(left.scale(.1)));widths.add(0,0f);
        return new RiftGeometry(List.copyOf(points),List.copyOf(widths));
    }
}
