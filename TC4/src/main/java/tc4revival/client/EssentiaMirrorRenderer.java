package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.EssentiaMirrorBlockEntity;

/** World-space frame, glass cover and projected portal layers. */
public  class EssentiaMirrorRenderer implements BlockEntityRenderer<EssentiaMirrorBlockEntity> {
    private static final ResourceLocation FRAME=ResourceLocation.parse("thaumcraft:textures/block/mirrorframe2.png");
    private static final ResourceLocation CLOSED=ResourceLocation.parse("thaumcraft:textures/block/mirrorpane.png");
    private static final ResourceLocation COVER=ResourceLocation.parse("thaumcraft:textures/block/mirrorpanetrans.png");
    public EssentiaMirrorRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(EssentiaMirrorBlockEntity mirror,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        renderMirror(mirror,mirror.facing(),mirror.linked(),0,FRAME,pose,buffers,light,overlay);
    }
    static void renderMirror(net.minecraft.world.level.block.entity.BlockEntity mirror,net.minecraft.core.Direction facing,boolean linked,float jitter,ResourceLocation frameTexture,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        // The dispatcher/contraption renderer already culls the geometry. A virtual
        // block entity's local position is not a world position for a second FOV test.
        // Link state must never depend on where the observer is looking.
        boolean portal=linked;
        if (portal) MirrorPortalRenderer.render(mirror,facing,pose,buffers);
        pose.pushPose(); orient(pose,facing);
        // Coordinates follow the old 0..1 item extrusion, whose front points along local -Z.
        // The portal is deferred past world composition. Queue its glass cover in
        // the same ordered pass; an earlier entity-translucent cover both writes
        // occluding depth and loses its tint when the portal is drawn over it.
        var paneType=TCRenderTypes.mirrorPane(portal ? COVER : CLOSED);
        var pane=TCRenderTypes.buffer(buffers,paneType);var matrix=pose.last().pose();float z=-.02f-jitter;
        pane.vertex(matrix,0,0,z).color(255,255,255,255).uv(0,1).uv2(light).endVertex();
        pane.vertex(matrix,1,0,z).color(255,255,255,255).uv(1,1).uv2(light).endVertex();
        pane.vertex(matrix,1,1,z).color(255,255,255,255).uv(1,0).uv2(light).endVertex();
        pane.vertex(matrix,0,1,z).color(255,255,255,255).uv(0,0).uv2(light).endVertex();
        if(buffers instanceof MultiBufferSource.BufferSource immediate)TCRenderTypes.endBatch(immediate,paneType);
        var frame=buffers.getBuffer(RenderType.entityCutoutNoCull(frameTexture));
        quad(frame,pose,light,overlay,0,0,0,1,1,0,1,1,0,0,0,0,1);
        quad(frame,pose,light,overlay,0,0,-1/16f,1,1,-1/16f,1,1,0,0,0,0,-1);
        // As in old renderItemIn2D, every texture row/column receives an extruded strip.
        // Alpha cutout removes transparent pixels, including the hole inside the frame.
        for (int pixel=0;pixel<16;pixel++) {
            float low=pixel/16f,high=(pixel+1)/16f,uv=(pixel+.5f)/16f;
            quad(frame,pose,light,overlay,low,0,-1/16f,low,1,0,1-uv,1,1-uv,0,-1,0,0);
            quad(frame,pose,light,overlay,high,0,-1/16f,high,1,0,1-uv,1,1-uv,0,1,0,0);
            quad(frame,pose,light,overlay,0,low,-1/16f,1,low,0,1,1-uv,0,1-uv,0,-1,0);
            quad(frame,pose,light,overlay,0,high,-1/16f,1,high,0,1,1-uv,0,1-uv,0,1,0);
        }
        pose.popPose();
    }
    static void orient(PoseStack pose,net.minecraft.core.Direction face) {
        switch(face) {
            case DOWN->{ pose.translate(0,1,1); pose.mulPose(Axis.XP.rotationDegrees(-90)); }
            case UP->pose.mulPose(Axis.XP.rotationDegrees(90));
            case NORTH->pose.translate(0,0,1);
            case SOUTH->{ pose.translate(1,0,0); pose.mulPose(Axis.YP.rotationDegrees(180)); }
            case WEST->{ pose.translate(1,0,1); pose.mulPose(Axis.YP.rotationDegrees(90)); }
            case EAST->pose.mulPose(Axis.YP.rotationDegrees(-90));
        }
    }
    private static void quad(VertexConsumer out,PoseStack pose,int light,int overlay,float x0,float y0,float z0,float x1,float y1,float z1,
            float u0,float v0,float u1,float v1,float nx,float ny,float nz) {
        if (nx!=0) {
            vertex(out,pose,x0,y0,z0,u0,v0,light,overlay,nx,ny,nz); vertex(out,pose,x0,y1,z0,u0,v1,light,overlay,nx,ny,nz);
            vertex(out,pose,x1,y1,z1,u1,v1,light,overlay,nx,ny,nz); vertex(out,pose,x1,y0,z1,u1,v0,light,overlay,nx,ny,nz);
        } else if (ny!=0) {
            vertex(out,pose,x0,y0,z0,u0,v0,light,overlay,nx,ny,nz); vertex(out,pose,x1,y0,z0,u1,v0,light,overlay,nx,ny,nz);
            vertex(out,pose,x1,y1,z1,u1,v1,light,overlay,nx,ny,nz); vertex(out,pose,x0,y1,z1,u0,v1,light,overlay,nx,ny,nz);
        } else {
            vertex(out,pose,x0,y0,z0,u0,v0,light,overlay,nx,ny,nz); vertex(out,pose,x1,y0,z0,u1,v0,light,overlay,nx,ny,nz);
            vertex(out,pose,x1,y1,z1,u1,v1,light,overlay,nx,ny,nz); vertex(out,pose,x0,y1,z1,u0,v1,light,overlay,nx,ny,nz);
        }
    }
    private static void vertex(VertexConsumer out,PoseStack pose,float x,float y,float z,float u,float v,int light,int overlay,float nx,float ny,float nz) {
        out.vertex(pose.last().pose(),x,y,z).color(255,255,255,255).uv(u,v).overlayCoords(overlay).uv2(light).normal(pose.last().normal(),nx,ny,nz).endVertex();
    }
}
