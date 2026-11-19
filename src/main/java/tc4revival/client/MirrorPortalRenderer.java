package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import tc4revival.content.EssentiaMirrorBlockEntity;

/** CPU-generated projective texture coordinates replace fixed-function OpenGL texgen. */
 class MirrorPortalRenderer {
    private static final ResourceLocation TUNNEL=ResourceLocation.parse("thaumcraft:textures/misc/tunnel.png");
    private static final ResourceLocation FIELD=ResourceLocation.parse("thaumcraft:textures/misc/particlefield.png");
    private MirrorPortalRenderer() {}
    static boolean visible(net.minecraft.world.level.block.entity.BlockEntity mirror) {
        var client=Minecraft.getInstance(); var player=client.player; if (player==null) return false;
        var center=Vec3.atCenterOf(mirror.getBlockPos()); double distance=player.position().distanceTo(center);
        if (distance<2) return true;
        if (distance>=400) return false;
        if (!client.options.getCameraType().isFirstPerson()) return true;
        var offset=center.subtract(player.getEyePosition());
        double angle=Math.acos(net.minecraft.util.Mth.clamp(offset.normalize().dot(player.getLookAngle()),-1,1));
        double vertical=Math.toRadians(client.options.fov().get()/2.0);
        double aspect=client.getWindow().getWidth()/(double)Math.max(1,client.getWindow().getHeight());
        double diagonal=Math.atan(Math.tan(vertical)*Math.sqrt(1+aspect*aspect));
        return angle<diagonal+Math.atan(1.5/distance);
    }
    static void render(net.minecraft.world.level.block.entity.BlockEntity mirror,Direction facing,PoseStack pose,MultiBufferSource buffers) {
        var camera=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        var relative=Vec3.atLowerCornerOf(mirror.getBlockPos()).subtract(camera);
        // Undo the supplied view transform to recover the near-plane point in object coordinates.
        var inverse=new org.joml.Matrix4f(pose.last().pose()).invert();
        var near=inverse.transformPosition(new org.joml.Vector3f(0,0,-.05f));
        var eye=new Vec3(near.x+relative.x,near.y+relative.y,near.z+relative.z);
        var axis=facing.getAxis();
        boolean positive=facing.getAxisDirection()==Direction.AxisDirection.NEGATIVE;
        double offset=positive ? .99 : .01;
        double plane=coordinate(relative,axis)+offset;
        Direction.Axis sAxis=axis==Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        Direction.Axis tAxis=axis==Direction.Axis.Y ? Direction.Axis.Z : Direction.Axis.Y;
        long time=System.currentTimeMillis();
        for (var layer:MirrorPortalLayers.layers()) {
            var projection=MirrorPortalLayers.projection(layer,plane,coordinate(eye,axis),coordinate(eye,sAxis),coordinate(eye,tAxis),
                    coordinate(camera,sAxis),coordinate(camera,tAxis),positive);
            if (projection==null) continue;
            var type=layer.additive() ? TCRenderTypes.softParticles(FIELD) : TCRenderTypes.softTranslucent(TUNNEL);
            var out=TCRenderTypes.buffer(buffers,type);
            for (int corner=0;corner<4;corner++) {
                double s=corner==0 || corner==3 ? .1875 : .8125,t=corner<2 ? .1875 : .8125;
                var local=axis==Direction.Axis.X ? new Vec3(offset,t,s) : axis==Direction.Axis.Y ? new Vec3(s,offset,t) : new Vec3(s,t,offset);
                var vertex=local.add(relative);
                var uv=MirrorPortalLayers.textureCoordinates(layer,coordinate(vertex,sAxis),coordinate(vertex,tAxis),projection.q(),
                        projection.translateS(),projection.translateT(),time);
                out.vertex(pose.last().pose(),(float)local.x,(float)local.y,(float)local.z).color(layer.red(),layer.green(),layer.blue(),1)
                        .uv((float)(uv[0]/uv[2]),(float)(uv[1]/uv[2])).uv2(180).endVertex();
            }
            // Preserve the base-alpha then fifteen additive passes before the glass cover.
            if (buffers instanceof MultiBufferSource.BufferSource immediate) TCRenderTypes.endBatch(immediate,type);
        }
    }
    private static double coordinate(Vec3 vector,Direction.Axis axis) { return axis==Direction.Axis.X ? vector.x : axis==Direction.Axis.Y ? vector.y : vector.z; }
}
