package gadomancymodern.client;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.*;
/** Every layer keeps its own texture and shader; alpha and depth-write state apply at actual buffer draw. */
public class AchromaticRendering extends RenderType {
    protected static final java.util.Map<RenderType,RenderType> TYPES=new java.util.IdentityHashMap<>();
    protected AchromaticRendering(RenderType original){super("gadomancy_achromatic_"+original.toString(),original.format(),original.mode(),original.bufferSize(),original.affectsCrumbling(),true,()->{original.setupRenderState();RenderSystem.setShaderColor(1,1,1,.15f);RenderSystem.depthMask(false);RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();},()->{original.clearRenderState();RenderSystem.setShaderColor(1,1,1,1);RenderSystem.depthMask(true);RenderSystem.disableBlend();});}
    public static MultiBufferSource wrap(MultiBufferSource buffers){return type->buffers.getBuffer(TYPES.computeIfAbsent(type,AchromaticRendering::new));}
}
