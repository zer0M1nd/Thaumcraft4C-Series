package automagy.client;

import automagy.content.MawBlock;
import automagy.content.MawBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import tc4revival.client.MirrorPortalLayers;
import tc4revival.client.TCRenderTypes;

/** Legacy texgen portal: a dark tunnel followed by fifteen additive particle fields. */
public class MawRenderer implements BlockEntityRenderer<MawBlockEntity> {
    private static final ResourceLocation TUNNEL=new ResourceLocation("thaumcraft","textures/misc/tunnel.png");
    private static final ResourceLocation FIELD=new ResourceLocation("thaumcraft","textures/misc/particlefield.png");
    public MawRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(MawBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var facing=tile.getBlockState().getValue(MawBlock.FACING);
        var camera=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        var relative=Vec3.atLowerCornerOf(tile.getBlockPos()).subtract(camera);
        var near=new org.joml.Matrix4f(pose.last().pose()).invert().transformPosition(new org.joml.Vector3f(0,0,-.05f));
        var eye=new Vec3(near.x+relative.x,near.y+relative.y,near.z+relative.z);
        var axis=facing.getAxis();boolean positive=facing.getAxisDirection()==Direction.AxisDirection.NEGATIVE;
        double offset=positive?.99:.01,plane=coordinate(relative,axis)+offset;
        var sAxis=axis==Direction.Axis.X?Direction.Axis.Z:Direction.Axis.X;
        var tAxis=axis==Direction.Axis.Y?Direction.Axis.Z:Direction.Axis.Y;
        // The maw scrolls slower than TC4 mirrors: one cycle per 700000 ms.
        long textureTime=(System.currentTimeMillis()%700000L)*250000L/700000L;
        for(var layer:MirrorPortalLayers.layers()) {
            var projection=MirrorPortalLayers.projection(layer,plane,coordinate(eye,axis),coordinate(eye,sAxis),coordinate(eye,tAxis),
                    coordinate(camera,sAxis),coordinate(camera,tAxis),positive);
            if(projection==null)continue;
            var type=layer.additive()?TCRenderTypes.softParticles(FIELD):TCRenderTypes.softTranslucent(TUNNEL);
            var out=buffers.getBuffer(type);float dim=layer.index()==0?.5f:1f;
            for(int corner=0;corner<4;corner++) {
                double s=corner==0||corner==3?.355:.645,t=corner<2?.355:.645;
                var local=axis==Direction.Axis.X?new Vec3(offset,t,s):axis==Direction.Axis.Y?new Vec3(s,offset,t):new Vec3(s,t,offset);
                var vertex=local.add(relative);
                var uv=MirrorPortalLayers.textureCoordinates(layer,coordinate(vertex,sAxis),coordinate(vertex,tAxis),projection.q(),
                        projection.translateS(),projection.translateT(),textureTime);
                out.vertex(pose.last().pose(),(float)local.x,(float)local.y,(float)local.z)
                        .color(layer.red()*dim,layer.green()*dim,layer.blue()*dim,1)
                        .uv((float)(uv[0]/uv[2]),(float)(uv[1]/uv[2])).uv2(light).endVertex();
            }
            if(buffers instanceof MultiBufferSource.BufferSource immediate)immediate.endBatch(type);
        }
    }
    private static double coordinate(Vec3 v,Direction.Axis axis){return axis==Direction.Axis.X?v.x:axis==Direction.Axis.Y?v.y:v.z;}
}
