package tc4revival.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import tc4revival.content.ArcaneBoreBlockEntity;

/** The two oppositely scrolling, three-plane beams from FXBeamBore. */
 class ArcaneBoreBeam {
    private ArcaneBoreBeam() {}
    static void render(ArcaneBoreBlockEntity bore,float partial,PoseStack pose,MultiBufferSource buffers) {
        var level=bore.getLevel(); var visual=bore.visual;
        if (level==null || visual.beamFade>=4 || visual.beamLength<=0) return;
        var origin=Vec3.atCenterOf(bore.getBlockPos()); var mc=Minecraft.getInstance();
        int distance=Minecraft.useFancyGraphics()?50:25;
        if (mc.gameRenderer.getMainCamera().getPosition().distanceToSqr(origin)>distance*distance) return;
        float yaw=(visual.rotation.x()+90-visual.aimX)/180f*(float)Math.PI;
        float pitch=(visual.rotation.z()+90-visual.aimZ)/180f*(float)Math.PI;
        var direction=new Vec3(Mth.sin(yaw)*Mth.cos(pitch),Mth.sin(pitch),Mth.cos(yaw)*Mth.cos(pitch));
        var end=origin.add(direction.scale(visual.beamLength));
        var hit=level.clip(new ClipContext(origin.add(direction),end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,null));
        boolean impact=hit.getType()!=HitResult.Type.MISS; if (impact) end=hit.getLocation();
        var delta=end.subtract(origin); double length=delta.length(); if (length<1e-6) return;
        var axis=delta.scale(1/length);
        var across=axis.cross(Math.abs(axis.y)>.99?new Vec3(1,0,0):new Vec3(0,1,0)).normalize();
        var second=axis.cross(across).normalize(); var start=new Vec3(.5,.5,.5);
        float size=Math.min((visual.beamAge+partial)/4,1),alpha=.4f-visual.beamFade*.1f;
        var tip=start.add(delta.scale(size));
        float time=(mc.player==null?level.getGameTime():mc.player.tickCount)+partial;
        double rotation=((level.getDayTime()%72)*5+5*partial)*Math.PI/180;
        for (int type=1;type<=2;type++) {
            var out=TCRenderTypes.buffer(buffers,TCRenderTypes.runes(ResourceLocation.parse("thaumcraft:textures/misc/beam"+type+".png")));
            float ticks=type==1?-time:time,scroll=-ticks*.2f-Mth.floor(-ticks*.1f);
            int color=type==1?65382:0xff8855;
            for (int plane=0;plane<3;plane++) {
                double angle=rotation+(plane+1)*Math.PI/3;
                var width=across.scale(Math.cos(angle)).add(second.scale(Math.sin(angle))).scale(.15*size);
                var endWidth=width.scale(impact?2:0); float v=-1+scroll+plane/3f;
                vertex(out,pose,tip.subtract(endWidth),1,v+(float)length*size,color,alpha);
                vertex(out,pose,start.subtract(width),1,v,color,alpha);
                vertex(out,pose,start.add(width),0,v,color,alpha);
                vertex(out,pose,tip.add(endWidth),0,v+(float)length*size,color,alpha);
            }
            if (impact) {
                pose.pushPose(); var endpoint=end.subtract(Vec3.atLowerCornerOf(bore.getBlockPos()));
                pose.translate(endpoint.x,endpoint.y,endpoint.z); pose.mulPose(mc.gameRenderer.getMainCamera().rotation());
                var flare=TCRenderTypes.buffer(buffers,TCRenderTypes.runes(ResourceLocation.parse("thaumcraft:textures/misc/particles.png")));
                float u=(visual.beamAge%16)/16f;
                // endMod=2 and impact=5 while the endpoint is refreshed each tick.
                vertex(flare,pose,new Vec3(-1,-1,0),u+.0624375f,.3749375f,color,.66f);
                vertex(flare,pose,new Vec3(-1,1,0),u+.0624375f,.3125f,color,.66f);
                vertex(flare,pose,new Vec3(1,1,0),u,.3125f,color,.66f);
                vertex(flare,pose,new Vec3(1,-1,0),u,.3749375f,color,.66f);
                pose.popPose();
            }
        }
    }
    private static void vertex(VertexConsumer out,PoseStack pose,Vec3 point,float u,float v,int color,float alpha) {
        out.vertex(pose.last().pose(),(float)point.x,(float)point.y,(float)point.z).color((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,alpha).uv(u,v).uv2(200).endVertex();
    }
}
