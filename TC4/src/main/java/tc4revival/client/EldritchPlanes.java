package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Fixed-function object/eye texgen expressed as projective UVs on axis-aligned planes. */
public  class EldritchPlanes {
    private static final ResourceLocation TUNNEL=texture("tunnel"),FIELD=texture("particlefield"),DISTANT=texture("particlefield32");
    private EldritchPlanes() {}
    private static ResourceLocation texture(String name) { return ResourceLocation.parse("thaumcraft:textures/misc/"+name+".png"); }
    public static void draw(BlockPos block,Direction facing,float offset,float minAcross,float maxAcross,float minY,float maxY,
                            boolean near,float partial,PoseStack pose,MultiBufferSource buffers) {
        boolean alongY=facing.getAxis()==Direction.Axis.Y,alongZ=facing.getAxis()==Direction.Axis.Z,positive=facing.getAxisDirection()==Direction.AxisDirection.POSITIVE;
        float[][] points;
        if (alongY) points=positive ? new float[][]{{minAcross,offset,minY},{minAcross,offset,maxY},{maxAcross,offset,maxY},{maxAcross,offset,minY}}
                : new float[][]{{minAcross,offset,maxY},{minAcross,offset,minY},{maxAcross,offset,minY},{maxAcross,offset,maxY}};
        else if (alongZ) points=positive ? new float[][]{{minAcross,maxY,offset},{minAcross,minY,offset},{maxAcross,minY,offset},{maxAcross,maxY,offset}}
                : new float[][]{{minAcross,minY,offset},{minAcross,maxY,offset},{maxAcross,maxY,offset},{maxAcross,minY,offset}};
        else points=positive ? new float[][]{{offset,maxY,minAcross},{offset,maxY,maxAcross},{offset,minY,maxAcross},{offset,minY,minAcross}}
                : new float[][]{{offset,minY,minAcross},{offset,minY,maxAcross},{offset,maxY,maxAcross},{offset,maxY,minAcross}};
        var camera=Minecraft.getInstance().gameRenderer.getMainCamera(); var viewer=Minecraft.getInstance().getCameraEntity();
        if (viewer==null) near=false;
        if (!near) {
            var out=TCRenderTypes.buffer(buffers,TCRenderTypes.worldTranslucent(DISTANT)); float[][] uv={{1,1},{1,0},{0,0},{0,1}};
            for (int i=0;i<4;i++) out.vertex(pose.last().pose(),points[i][0],points[i][1],points[i][2]).color(.5f,.5f,.5f,1).uv(uv[i][0],uv[i][1]).uv2(180).endVertex();
            return;
        }
        Vec3 feet=viewer.getPosition(partial),eye=camera.getPosition().subtract(feet).add(camera.getNearPlane().getPointOnPlane(0,0));
        double plane=(alongY ? block.getY()-feet.y : alongZ ? block.getZ()-feet.z : block.getX()-feet.x)+offset;
        double normalEye=alongY ? eye.y : alongZ ? eye.z : eye.x;
        double distance=positive ? normalEye-plane : plane-normalEye;
        // The old projection divided by zero on the viewing plane. Do not emit non-finite UVs.
        if (Math.abs(distance)<1e-6) return;
        var random=new Random(31100); float scroll=(System.currentTimeMillis()%700000L)/250000f;
        for (int layer=0;layer<16;layer++) {
            double depth=layer==0 ? 65 : 16-layer,scale=layer==0 ? .125 : layer==1 ? .5 : .0625;
            double q=-distance/(distance+depth); if (!Double.isFinite(q) || Math.abs(q)<1e-6) continue;
            float weight=layer==0 ? .1f : 1f/(17-layer);
            float red=random.nextFloat()*.5f+.1f,green=random.nextFloat()*.5f+.4f,blue=random.nextFloat()*.5f+.5f;
            if (layer==0) red=green=blue=1;
            double angle=Math.toRadians((float)((layer*layer*4321+layer*9)*2)),cos=Math.cos(angle),sin=Math.sin(angle);
            var type=layer==0 ? TCRenderTypes.worldTranslucent(TUNNEL) : TCRenderTypes.lens(FIELD);
            var out=TCRenderTypes.buffer(buffers,type);
            for (float[] point:points) {
                double across=alongY || alongZ ? block.getX()+point[0]-feet.x : block.getZ()+point[2]-feet.z;
                double s=across/q-(alongY || alongZ ? feet.x : feet.z)+(alongY || alongZ ? eye.x : eye.z)*depth/distance-.5;
                double other=alongY ? block.getZ()+point[2]-feet.z : block.getY()+point[1]-feet.y;
                double t=other/q-(alongY ? feet.z : feet.y)+(alongY ? eye.z : eye.y)*depth/distance-.5;
                float u=(float)((cos*s-sin*t+.5)*scale),v=(float)((sin*s+cos*t+.5)*scale+scroll);
                out.vertex(pose.last().pose(),point[0],point[1],point[2]).color(red*weight,green*weight,blue*weight,1).uv(u,v).uv2(180).endVertex();
            }
        }
    }
}
