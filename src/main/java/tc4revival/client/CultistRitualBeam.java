package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import tc4revival.config.TCConfig;
import tc4revival.content.CultistClericEntity;

 class CultistRitualBeam {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/misc/wispy.png");
    static void render(CultistClericEntity entity,float bob,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        int quality=TCConfig.CLIENT.golemLinkQuality(); if (quality==0) return;
        var interpolated=new Vec3(Mth.lerp(partial,entity.xOld,entity.getX()),Mth.lerp(partial,entity.yOld,entity.getY()),Mth.lerp(partial,entity.zOld,entity.getZ()));
        var start=Vec3.atLowerCornerOf(entity.ritualHome()).add(.5,1.5,.5);
        var end=interpolated.add(0,entity.getEyeHeight()*1.2+bob,0); var delta=end.subtract(start);
        float distance=(float)delta.length(),segments=Math.round(distance)*(quality/2f);
        if (segments<=0 || !Float.isFinite(segments)) return;
        // Preserve zero-length/quality suppression without allowing malformed configuration to allocate unbounded ribbons.
        segments=Math.min(8192,segments); int count=(int)(segments*Math.min(entity.tickCount,10)/10f);
        float time=System.nanoTime()/30_000_000L;
        var out=TCRenderTypes.buffer(buffers,TCRenderTypes.worldTranslucent(TEXTURE));
        var origin=start.subtract(interpolated); Vec3 previous=origin; float previousU=distance+time*.03f;
        for (int index=1;index<=count;index++) {
            float progress=index/segments,envelope=1-Math.abs(index-segments/2)/(segments/2),phase=distance*(1-progress)*quality/2-time%32767/5;
            var offset=new Vec3(Mth.sin((float)(entity.getZ()%16+phase)/4),Mth.sin((float)(entity.getX()%16+phase)/3),Mth.sin((float)(entity.getY()%16+phase)/2)).scale(.5f*envelope);
            var current=origin.add(delta.add(offset).scale(progress)); float u=(1-progress)*distance+time*.03f;
            for (var width:new Vec3[]{new Vec3(0,.25,0),new Vec3(.25,0,0)}) {
                vertex(out,pose,previous.subtract(width),previousU,1,light); vertex(out,pose,previous.add(width),previousU,0,light);
                vertex(out,pose,current.add(width),u,0,light); vertex(out,pose,current.subtract(width),u,1,light);
            }
            previous=current; previousU=u;
        }
    }
    private static void vertex(VertexConsumer out,PoseStack pose,Vec3 pos,float u,float v,int light) {
        out.vertex(pose.last().pose(),(float)pos.x,(float)pos.y,(float)pos.z).color(17/255f,0,17/255f,.8f).uv(u,v).uv2(light).endVertex();
    }
    private CultistRitualBeam() {}
}
