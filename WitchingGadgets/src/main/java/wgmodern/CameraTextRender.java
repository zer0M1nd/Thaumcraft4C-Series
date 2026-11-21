package wgmodern;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/** The original camera draws target names with GL_SRC_ALPHA, GL_ONE. */
public class CameraTextRender extends RenderType {
    private CameraTextRender() {
        super("unused", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS, 256, false, false, () -> {}, () -> {});
    }

    private static final Function<RenderType, RenderType> TYPES = Util.memoize(original ->
            new RenderType("witchinggadgets_camera_text", original.format(), original.mode(),
                    original.bufferSize(), original.affectsCrumbling(), false,
                    () -> {
                        original.setupRenderState();
                        RenderSystem.enableBlend();
                        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                                GlStateManager.DestFactor.ONE);
                    }, () -> {
                        RenderSystem.defaultBlendFunc();
                        original.clearRenderState();
                    }) {});

    public static MultiBufferSource additive(MultiBufferSource buffers) {
        return original -> buffers.getBuffer(TYPES.apply(original));
    }
}
