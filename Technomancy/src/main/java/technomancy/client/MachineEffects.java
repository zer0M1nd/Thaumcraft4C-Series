package technomancy.client;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import technomancy.*;

/** Blockbench sphere export and transient arcs at the author's generator electrodes. */
final class MachineEffects {
    private static float[][][] sphere;
    private static final Map<MachineEntity,Arc> ARCS=new WeakHashMap<>();
    private static class Arc {long tick=Long.MIN_VALUE,until;Vec3[] points;int color;}
    static void clear(){sphere=null;ARCS.clear();}
    static void sphere(MachineEntity m,PoseStack pose,MultiBufferSource buffers,int light){
        if(sphere==null)try(var reader=Minecraft.getInstance().getResourceManager().openAsReader(Technomancy.id("geometry/blacksphere.json"))){
            sphere=new com.google.gson.Gson().fromJson(JsonParser.parseReader(reader).getAsJsonObject().get("triangles"),float[][][].class);
        }catch(java.io.IOException e){throw new IllegalStateException("Missing black-hole mesh",e);}
        float radius=switch(m.state.getString("ritual")){case "black_hole_2"->4.7f;case "black_hole_3"->8.1f;default->3;};
        var out=buffers.getBuffer(RenderType.entitySolid(Technomancy.id("textures/models/white.png")));
        pose.pushPose();pose.translate(.5,.5,.5);pose.scale(radius,radius,radius);
        for(var triangle:sphere)for(int i:new int[]{0,1,2,2}){var v=triangle[i];
            out.vertex(pose.last().pose(),v[0],v[1],v[2]).color(26,26,26,255).uv(0,0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.last().normal(),v[0],v[1],v[2]).endVertex();
        }pose.popPose();
    }
    static void arcs(MachineEntity m,PoseStack pose,MultiBufferSource buffers){
        if(m.getLevel()==null||!m.active||!TechConfig.FANCY.get())return;
        long now=m.getLevel().getGameTime();var arc=ARCS.computeIfAbsent(m,key->new Arc());
        if(arc.tick!=now){arc.tick=now;var random=m.getLevel().random;
            if(now>=arc.until&&random.nextInt(Math.max(60-m.progress/4,1))==0){
                var forward=Vec3.atLowerCornerOf(m.facing().getNormal());var side=new Vec3(-forward.z,0,forward.x);
                double horizontal=random.nextBoolean()?1:-1,vertical=random.nextBoolean()?1:-1;
                var center=new Vec3(.5,1.5,.5);var start=center.add(forward).add(side.scale(horizontal)).add(0,vertical,0);
                var end=center.add(forward.scale(5)).add(side.scale(-horizontal)).add(0,-vertical,0);
                arc.points=new Vec3[17];arc.points[0]=start;arc.points[16]=end;
                for(int i=1;i<16;i++)arc.points[i]=start.lerp(end,i/16d).add((random.nextDouble()-.5)*.3,(random.nextDouble()-.5)*.3,(random.nextDouble()-.5)*.3);
                arc.until=now+6;arc.color=m.facing()==net.minecraft.core.Direction.NORTH||m.facing()==net.minecraft.core.Direction.WEST?0xbdb7ff:0xff7aff;
                m.getLevel().playLocalSound(m.getBlockPos(),tc4revival.content.TCSounds.ZAP.get(),net.minecraft.sounds.SoundSource.BLOCKS,.5f,.5f+random.nextFloat(),false);
            }
        }
        if(arc.points==null||now>=arc.until)return;
        var out=buffers.getBuffer(RenderType.lightning());var camera=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(Vec3.atLowerCornerOf(m.getBlockPos()));
        for(int i=1;i<arc.points.length;i++){
            // Lightning uses face culling; this winding must face the camera.
            var a=arc.points[i-1];var b=arc.points[i];var normal=camera.subtract(a).cross(b.subtract(a)).normalize().scale(.02);
            for(var point:new Vec3[]{a.subtract(normal),b.subtract(normal),b.add(normal),a.add(normal)})
                out.vertex(pose.last().pose(),(float)point.x,(float)point.y,(float)point.z).color(arc.color>>16&255,arc.color>>8&255,arc.color&255,220).endVertex();
        }
    }
    private MachineEffects(){}
}
