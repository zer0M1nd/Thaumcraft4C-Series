package tc4revival.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/** Preserve TC's additive/alpha effects after world composition, including vanilla clouds. */
@Mod.EventBusSubscriber(modid="thaumcraft", value=Dist.CLIENT)
public final class ShaderPackMagicRendering {
    private static final List<Draw> pending = new ArrayList<>();
    private static final List<BufferBuilder> pool = new ArrayList<>();
    private static boolean drawing;
    private static boolean vanillaWorld;
    private record Draw(RenderType type, BufferBuilder builder, boolean sort,
                        Matrix4f modelView, Matrix4f projection, VertexSorting sorting,
                        float[] color) {}

    private ShaderPackMagicRendering() {}

    static VertexConsumer buffer(RenderType type, boolean sort) {
        // Iris replaces MultiBufferSource and bypasses RenderType.end. Own the buffer
        // from acquisition through upload so its batching cannot submit it early.
        if (ShaderPackShaders.shadowPass()) return DISCARD;
        int index = pending.size();
        if (pool.size() <= index) pool.add(new BufferBuilder(type.bufferSize()));
        var builder = pool.get(index);
        builder.begin(type.mode(), type.format());
        pending.add(new Draw(type, builder, sort, new Matrix4f(RenderSystem.getModelViewMatrix()),
                new Matrix4f(RenderSystem.getProjectionMatrix()), RenderSystem.getVertexSorting(),
                RenderSystem.getShaderColor().clone()));
        return builder;
    }

    static boolean drawing() { return drawing; }
    static boolean shouldDefer() { return !drawing && (vanillaWorld || ShaderPackShaders.particlesInUse()); }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void worldStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            vanillaWorld = !ShaderPackShaders.particlesInUse();
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES && vanillaWorld) {
            // Block/entity geometry is present, but translucent terrain (including
            // jar glass) and distant clouds have not written depth yet.
            Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
            ShaderPackDepth.captureVanilla();
        }
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void afterLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        boolean vanilla = vanillaWorld;
        vanillaWorld = false;
        if (!pending.isEmpty()) drawPending(vanilla);
        EldritchMist.renderShaderPackFog(event.getProjectionMatrix());
    }

    private static void drawPending(boolean vanilla) {
        var modelView = RenderSystem.getModelViewStack();
        var projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        var color = RenderSystem.getShaderColor().clone();
        modelView.pushPose();
        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        boolean borrowedDepth = vanilla ? ShaderPackDepth.bindVanilla() : ShaderPackDepth.bind();
        drawing = true;
        try {
            for (var draw : pending) {
                modelView.last().pose().set(draw.modelView());
                RenderSystem.applyModelViewMatrix();
                RenderSystem.setProjectionMatrix(draw.projection(), draw.sorting());
                var tint = draw.color();
                RenderSystem.setShaderColor(tint[0], tint[1], tint[2], tint[3]);
                draw.type().setupRenderState();
                // Never modify the pack's borrowed opaque depth. Translucent glass
                // must not hide the node it contains, while stone and hands still occlude.
                if (borrowedDepth) RenderSystem.depthMask(false);
                try {
                    if (draw.sort()) draw.builder().setQuadSorting(draw.sorting());
                    BufferUploader.drawWithShader(draw.builder().end());
                } finally {
                    draw.type().clearRenderState();
                }
            }
        } finally {
            drawing = false;
            for (var draw : pending) if (draw.builder().building()) draw.builder().end().release();
            pending.clear();
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projection, sorting);
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        }
    }

    @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        for (var draw : pending) if (draw.builder().building()) draw.builder().end().release();
        pending.clear();
        pool.clear();
        vanillaWorld = false;
        ShaderPackDepth.close();
    }

    @SubscribeEvent
    public static void startFrame(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        vanillaWorld = false;
        ShaderPackDepth.startFrame();
        // A world change/reload can abort a frame before AFTER_LEVEL.
        for (var draw : pending) if (draw.builder().building()) draw.builder().end().release();
        pending.clear();
    }

    private static final VertexConsumer DISCARD = new VertexConsumer() {
        @Override public VertexConsumer vertex(double x,double y,double z) { return this; }
        @Override public VertexConsumer color(int r,int g,int b,int a) { return this; }
        @Override public VertexConsumer uv(float u,float v) { return this; }
        @Override public VertexConsumer overlayCoords(int u,int v) { return this; }
        @Override public VertexConsumer uv2(int u,int v) { return this; }
        @Override public VertexConsumer normal(float x,float y,float z) { return this; }
        @Override public void endVertex() {}
        @Override public void defaultColor(int r,int g,int b,int a) {}
        @Override public void unsetDefaultColor() {}
    };
}
