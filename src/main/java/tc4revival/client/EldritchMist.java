package tc4revival.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/** The guardian refreshes a 200-tick mist, with the old density ramp and final 100-tick fade. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public  class EldritchMist {
    private static ClientLevel current;
    private static int remaining;
    private static float density;
    private static float red, green, blue;
    private EldritchMist() {}
    private static void world() {
        var level=Minecraft.getInstance().level;
        if (current!=level) { current=level; remaining=0; density=0; }
    }
    public static void refresh() { world(); if (current!=null) remaining=Math.max(200,remaining); }
    public static void longMist() { world(); if (current!=null) remaining=2400; }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase!=TickEvent.Phase.END) return; world();
        if (current==null || Minecraft.getInstance().isPaused() || remaining<=0) return;
        if (remaining<100) density=.1f*(remaining/100f);
        else if (density<.1f) density+=.001f;
        if (--remaining<=0) density=0;
    }
    @SubscribeEvent public static void fog(ViewportEvent.RenderFog event) {
        world(); if (remaining<=0 || density<=0) return;
        // Use Forge's ordinary finite fog range. Encoding density in a large negative start
        // only works with our own global fog.glsl and breaks terrain shaders supplied by mods.
        // 95% extinction of the old exponential mist occurs at -log(.05) / density.
        var range = MistFogRange.apply(event.getNearPlaneDistance(), event.getFarPlaneDistance(), density);
        event.setNearPlaneDistance(range.near());
        event.setFarPlaneDistance(range.far());
        event.setFogShape(com.mojang.blaze3d.shaders.FogShape.SPHERE);
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void fogColor(ViewportEvent.ComputeFogColor event) {
        red = event.getRed();
        green = event.getGreen();
        blue = event.getBlue();
    }

    // Packs can ignore Forge's fog range. Apply the same density to the composed
    // scene using the pack's opaque depth, before vanilla draws the HUD.
    static void renderShaderPackFog(Matrix4f projection) {
        var shader = LegacyLightmapShaders.eldritchFog;
        if (density <= 0 || current != Minecraft.getInstance().level || shader == null) return;
        int depth = ShaderPackDepth.opaqueTexture();
        if (depth <= 0) return;

        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        var previousShader = RenderSystem.getShader();
        int previousTexture = RenderSystem.getShaderTexture(0);
        boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        int dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        int dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        try {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ZERO, GL11.GL_ONE);
            RenderSystem.setShaderTexture(0, depth);
            RenderSystem.setShader(() -> shader);
            shader.safeGetUniform("InvProjection").set(new Matrix4f(projection).invert());
            shader.safeGetUniform("MistColor").set(red, green, blue);
            shader.safeGetUniform("Density").set(density);
            var quad = Tesselator.getInstance().getBuilder();
            quad.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            quad.vertex(-1, -1, 0).uv(0, 0).endVertex();
            quad.vertex(1, -1, 0).uv(1, 0).endVertex();
            quad.vertex(1, 1, 0).uv(1, 1).endVertex();
            quad.vertex(-1, 1, 0).uv(0, 1).endVertex();
            BufferUploader.drawWithShader(quad.end());
        } finally {
            RenderSystem.setShaderTexture(0, previousTexture);
            RenderSystem.setShader(() -> previousShader);
            RenderSystem.depthMask(depthWrite);
            if (depthTest) RenderSystem.enableDepthTest();
            RenderSystem.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
        }
    }
}
