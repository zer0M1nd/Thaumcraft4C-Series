package thaumicomniverse.compat.create;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import tc4revival.client.TCRenderTypes;

/** The receiving leaf renders its own connection, including the mounted local transform. */
public class ChargerVisRenderer {
    public static Vec3 tip(net.minecraft.core.Direction face){return new Vec3(.5,.5,.5).subtract(Vec3.atLowerCornerOf(face.getNormal()).scale(.03));}
    public static void beam(Vec3 parent,net.minecraft.core.Direction face,net.minecraft.nbt.CompoundTag state,PoseStack pose,MultiBufferSource buffers,float ticks){
        if(parent==null)return;
        var end=tip(face);var delta=end.subtract(parent);double length=delta.length();if(length<1e-5||length>10)return;
        var axis=delta.normalize();var cross=axis.cross(Math.abs(axis.y)>.99?new Vec3(1,0,0):new Vec3(0,1,0)).normalize();
        var mc=Minecraft.getInstance();boolean revealed=mc.getCameraEntity() instanceof net.minecraft.world.entity.LivingEntity viewer&&tc4revival.content.RevealingGear.visible(viewer,true);
        float elapsed=state.contains("Until")?Math.max(0,ticks-state.getLong("Until")):20;
        float opacity=Math.max(.3f,.8f-elapsed*.025f);
        float alpha=tc4revival.client.VisBeamVisibility.ribbon(opacity,revealed);
        int color=state.contains("Color")?state.getInt("Color"):0xffffff;
        float fade=Math.min(1,(elapsed+5)*.025f);
        float red=Math.min(1,((color>>16)&255)/255f+fade),green=Math.min(1,((color>>8)&255)/255f+fade),blue=Math.min(1,(color&255)/255f+fade);
        var out=buffers.getBuffer(TCRenderTypes.node(ResourceLocation.parse("thaumcraft:textures/misc/beam1.png"),false));
        for(int i=0;i<2;i++){
            var offset=(i==0?cross:axis.cross(cross)).scale(.105);float v=-1-ticks*.2f-(float)Math.floor(-ticks*.1f)+i/3f;
            vertex(out,pose,end.subtract(offset),1,v+(float)length,red,green,blue,alpha);vertex(out,pose,parent.subtract(offset),1,v,red,green,blue,alpha);
            vertex(out,pose,parent.add(offset),0,v,red,green,blue,alpha);vertex(out,pose,end.add(offset),0,v+(float)length,red,green,blue,alpha);
        }
    }
    private static void vertex(VertexConsumer out,PoseStack pose,Vec3 p,float u,float v,float r,float g,float b,float alpha){out.vertex(pose.last().pose(),(float)p.x,(float)p.y,(float)p.z).color(r,g,b,alpha).uv(u,v).uv2(200).endVertex();}
}
