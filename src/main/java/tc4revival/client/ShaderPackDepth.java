package tc4revival.client;

import com.mojang.blaze3d.platform.GlStateManager;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** A color-only overlay on the pack's opaque depth, including its opaque first-person hand. */
final class ShaderPackDepth {
    private static final Access ACCESS = access();
    private static int framebuffer;
    private static boolean warned;
    private static com.mojang.blaze3d.pipeline.RenderTarget vanillaDepth;
    private static boolean vanillaCaptured;
    private record Access(Method manager, Method pipeline, Class<?> pipelineType, Field targets,
                          Method opaqueDepth, Method textureId) {}

    private ShaderPackDepth() {}

    static boolean bind() {
        int texture = opaqueTexture();
        return texture > 0 && bindTexture(texture);
    }

    static int opaqueTexture() {
        if (ACCESS == null) return 0;
        try {
            var pipeline = ACCESS.pipeline().invoke(ACCESS.manager().invoke(null));
            if (!ACCESS.pipelineType().isInstance(pipeline)) return 0;
            var depth = ACCESS.opaqueDepth().invoke(ACCESS.targets().get(pipeline));
            return (int) ACCESS.textureId().invoke(depth);
        } catch (ReflectiveOperationException | RuntimeException failure) {
            warn(failure);
            return 0;
        }
    }

    static void startFrame() { vanillaCaptured = false; }

    static void captureVanilla() {
        var main = Minecraft.getInstance().getMainRenderTarget();
        // Forge switches the main depth attachment to DEPTH32F_STENCIL8 when a
        // mod enables stencil. OpenGL requires matching formats for depth blits.
        if (vanillaDepth != null && vanillaDepth.isStencilEnabled() != main.isStencilEnabled()) {
            vanillaDepth.destroyBuffers();
            vanillaDepth = null;
        }
        if (vanillaDepth == null) {
            vanillaDepth = new com.mojang.blaze3d.pipeline.TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
            if (main.isStencilEnabled()) vanillaDepth.enableStencil();
        } else if (vanillaDepth.width != main.width || vanillaDepth.height != main.height) {
            vanillaDepth.resize(main.width, main.height, Minecraft.ON_OSX);
        }
        vanillaDepth.copyDepthFrom(main);
        main.bindWrite(false);
        vanillaCaptured = true;
    }

    static boolean bindVanilla() {
        return vanillaCaptured && bindTexture(vanillaDepth.getDepthTextureId());
    }

    private static boolean bindTexture(int texture) {
        var main = Minecraft.getInstance().getMainRenderTarget();
        try {
            if (framebuffer == 0) framebuffer = GlStateManager.glGenFramebuffers();
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            // The pack owns both textures. Reattach on every frame because reloads,
            // resizing and dimension changes can replace them without changing the IDs.
            GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, main.getColorTextureId(), 0);
            GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
                    GL11.GL_TEXTURE_2D, texture, 0);
            if (GlStateManager.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) == GL30.GL_FRAMEBUFFER_COMPLETE) return true;
            main.bindWrite(false);
            warn(null);
        } catch (RuntimeException failure) {
            main.bindWrite(false);
            warn(failure);
        }
        return false;
    }

    static void close() {
        if (framebuffer != 0) GlStateManager._glDeleteFramebuffers(framebuffer);
        framebuffer = 0;
        if (vanillaDepth != null) vanillaDepth.destroyBuffers();
        vanillaDepth = null;
        vanillaCaptured = false;
    }

    private static Access access() {
        try {
            var iris = Class.forName("net.irisshaders.iris.Iris");
            var manager = iris.getMethod("getPipelineManager");
            var pipeline = manager.getReturnType().getMethod("getPipelineNullable");
            var type = Class.forName("net.irisshaders.iris.pipeline.IrisRenderingPipeline");
            var targets = type.getDeclaredField("renderTargets");
            targets.setAccessible(true);
            var opaqueDepth = targets.getType().getMethod("getDepthTextureNoTranslucents");
            var texture = opaqueDepth.getReturnType().getMethod("getTextureId");
            return new Access(manager, pipeline, type, targets, opaqueDepth, texture);
        } catch (ClassNotFoundException absent) {
            return null;
        } catch (ReflectiveOperationException | RuntimeException incompatible) {
            warn(incompatible);
            return null;
        }
    }

    private static void warn(Throwable failure) {
        if (warned) return;
        warned = true;
        com.mojang.logging.LogUtils.getLogger().warn("Shader pack opaque depth is unavailable; using the scene depth for magic effects", failure);
    }
}
