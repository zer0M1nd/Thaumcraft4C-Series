package thaumicfutures.devices.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

final class DeviceDraw {
    static void texture(PoseStack pose,MultiBufferSource buffers,ResourceLocation texture,float x1,float y1,float x2,float y2,float z,float u1,float v1,float u2,float v2,int light) {
        var out=buffers.getBuffer(RenderType.entityTranslucent(texture));var p=pose.last();
        out.vertex(p.pose(),x1,y1,z).color(255,255,255,255).uv(u1,v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(),0,0,1).endVertex();
        out.vertex(p.pose(),x2,y1,z).color(255,255,255,255).uv(u2,v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(),0,0,1).endVertex();
        out.vertex(p.pose(),x2,y2,z).color(255,255,255,255).uv(u2,v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(),0,0,1).endVertex();
        out.vertex(p.pose(),x1,y2,z).color(255,255,255,255).uv(u1,v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(),0,0,1).endVertex();
    }
    static void color(PoseStack pose,MultiBufferSource buffers,float x1,float y1,float x2,float y2,float z,int rgb) {
        var out=buffers.getBuffer(RenderType.debugQuads());var p=pose.last().pose();
        float r=(rgb>>16&255)/255f,g=(rgb>>8&255)/255f,b=(rgb&255)/255f;
        out.vertex(p,x1,y1,z).color(r,g,b,1).endVertex();out.vertex(p,x2,y1,z).color(r,g,b,1).endVertex();
        out.vertex(p,x2,y2,z).color(r,g,b,1).endVertex();out.vertex(p,x1,y2,z).color(r,g,b,1).endVertex();
    }
    private DeviceDraw() {}
}
