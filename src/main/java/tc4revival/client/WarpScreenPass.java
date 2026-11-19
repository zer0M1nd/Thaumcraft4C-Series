package tc4revival.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;

/** Core-shader path: never clear or sample a framebuffer while writing that same framebuffer. */
 class WarpScreenPass {
    private int frames;
    private TextureTarget first,second;
    void close() {
        if(first!=null) { first.destroyBuffers(); first=null; }
        if(second!=null) { second.destroyBuffers(); second=null; }
    }
    void process(RenderTarget main,boolean sun) {
        if(LegacyLightmapShaders.warp==null) throw new IllegalStateException("Warp shader unavailable");
        if(first==null || first.width!=main.width || first.height!=main.height) {
            close();
            first=new TextureTarget(main.width,main.height,false,Minecraft.ON_OSX);
            second=new TextureTarget(main.width,main.height,false,Minecraft.ON_OSX);
            first.setFilterMode(org.lwjgl.opengl.GL11.GL_LINEAR);
            second.setFilterMode(org.lwjgl.opengl.GL11.GL_LINEAR);
        }
        RenderSystem.disableBlend(); RenderSystem.disableDepthTest(); RenderSystem.disableCull();
        RenderSystem.depthMask(false); RenderSystem.colorMask(true,true,true,true);
        boolean captureFrame=++frames==120;
        // Main stays intact until both effect passes have completed.
        if(captureFrame) capture(main,"input");
        draw(main,first,sun?2:0);
        if(captureFrame) capture(first,"first");
        draw(first,second,sun?3:1);
        if(captureFrame) capture(second,"second");
        draw(second,main,4);
        if(captureFrame) capture(main,"output");
    }
    private static void capture(RenderTarget target,String label) {
        if(!Boolean.getBoolean("tc4.warpCapture")) return;
        try(var picture=net.minecraft.client.Screenshot.takeScreenshot(target)) {
            var path=java.nio.file.Path.of("screenshots","warp-"+label+".png");
            java.nio.file.Files.createDirectories(path.getParent()); picture.writeToFile(path);
            tc4revival.TC4Revival.LOGGER.info("Warp framebuffer {}: texture={} size={}x{} glError={}",label,target.getColorTextureId(),target.width,target.height,org.lwjgl.opengl.GL11.glGetError());
        } catch(java.io.IOException failure) { tc4revival.TC4Revival.LOGGER.warn("Warp capture failed",failure); }
    }
    private static void draw(RenderTarget source,RenderTarget target,int pass) {
        if(source==target) throw new IllegalArgumentException("Framebuffer feedback");
        target.bindWrite(true);
        RenderSystem.setShader(()->LegacyLightmapShaders.warp);
        RenderSystem.setShaderTexture(0,source.getColorTextureId());
        LegacyLightmapShaders.warp.safeGetUniform("Pass").set(pass);
        var vertices=Tesselator.getInstance().getBuilder();
        vertices.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX);
        vertices.vertex(-1,-1,0).uv(0,0).endVertex();
        vertices.vertex(1,-1,0).uv(1,0).endVertex();
        vertices.vertex(1,1,0).uv(1,1).endVertex();
        vertices.vertex(-1,1,0).uv(0,1).endVertex();
        BufferUploader.drawWithShader(vertices.end());
    }
}
