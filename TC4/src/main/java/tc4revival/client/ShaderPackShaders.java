package tc4revival.client;

import java.util.function.Supplier;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;

/** Keep legacy shading in vanilla and participate in the active shader pack's world passes. */
final class ShaderPackShaders {
    private static final java.util.function.BooleanSupplier SHADOW_PASS = findShadowPass();
    private ShaderPackShaders() {}

    static ShaderInstance lightmap(Supplier<ShaderInstance> legacy) {
        // Text has exactly POSITION_COLOR_TEX_LIGHTMAP, without entity normals or overlays.
        // Oculus replaces this vanilla getter with a shader that also binds the pack's
        // framebuffer. A custom core shader bypasses that binding and can disappear.
        return replacement(GameRenderer.getRendertypeTextShader(), legacy);
    }

    static ShaderInstance rays(Supplier<ShaderInstance> legacy) {
        return replacement(GameRenderer.getRendertypeLightningShader(), legacy);
    }

    static boolean particlesInUse() {
        var shader = GameRenderer.getParticleShader();
        return shader != null && shader.getClass() != ShaderInstance.class;
    }

    static boolean shadowPass() { return SHADOW_PASS.getAsBoolean(); }

    private static java.util.function.BooleanSupplier findShadowPass() {
        try {
            var api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            var instance = api.getMethod("getInstance").invoke(null);
            var method = api.getMethod("isRenderingShadowPass");
            return () -> {
                try { return (boolean) method.invoke(instance); }
                catch (ReflectiveOperationException e) { throw new IllegalStateException("Cannot query shader shadow pass", e); }
            };
        } catch (ReflectiveOperationException | LinkageError absent) {
            return () -> false;
        }
    }

    private static ShaderInstance replacement(ShaderInstance standard, Supplier<ShaderInstance> legacy) {
        // Resolve at draw time: packs can be toggled/reloaded and the shadow/hand/world
        // passes have different replacements. Keep the exact old shader when the
        // vanilla getter is untouched, including in GUI rendering and without Oculus.
        return !ShaderPackMagicRendering.drawing() && standard != null && standard.getClass() != ShaderInstance.class ? standard : legacy.get();
    }
}
