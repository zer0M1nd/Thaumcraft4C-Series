package gadomancymodern.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.io.IOException;
import java.util.function.Function;

/** Original REHWandHandler.drawOverlayBlock uses GL_ADD texture environment, then SRC_ALPHA/ONE blending. */
@Mod.EventBusSubscriber(modid="gadomancy",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public class DropperOverlayRender extends RenderType {
    private static ShaderInstance shader;
    private DropperOverlayRender() {
        super("unused",DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,256,false,false,()->{},()->{});
    }

    // Preserve ConnectedFaces' texture, depth and blend state; only its texture-combine shader differs.
    private static final Function<RenderType,RenderType> TYPES=Util.memoize(original ->
        new RenderType("gadomancy_dropper_overlay",original.format(),original.mode(),original.bufferSize(),false,false,
            ()->{original.setupRenderState();RenderSystem.setShader(()->shader);},original::clearRenderState) {});

    public static RenderType wrap(RenderType original) { return TYPES.apply(original); }

    @SubscribeEvent public static void register(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
            new ResourceLocation("gadomancy","dropper_overlay"),DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),
            value->shader=value);
    }
}
