package automagy.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
import java.util.function.Function;

/** Legacy world OBJ rendering disables item directional lighting, but keeps the world lightmap. */
public class LegacyModelRenderTypes extends RenderType {
    private LegacyModelRenderTypes(){super("unused",DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,256,false,false,()->{},()->{});}
    private static final Function<ResourceLocation,RenderType> WORLD=Util.memoize(texture->create("automagy_legacy_world_model",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,256,false,false,
            CompositeState.builder().setShaderState(POSITION_COLOR_TEX_LIGHTMAP_SHADER)
                    .setTextureState(new TextureStateShard(texture,false,false)).setLightmapState(LIGHTMAP)
                    .setCullState(CULL).createCompositeState(false)));
    public static RenderType world(ResourceLocation texture){return WORLD.apply(texture);}
}
