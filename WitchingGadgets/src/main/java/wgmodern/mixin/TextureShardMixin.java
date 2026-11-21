package wgmodern.mixin;
import org.spongepowered.asm.mixin.*;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.resources.ResourceLocation;
import wgmodern.SpectralBuffers;
@Mixin(value=RenderStateShard.EmptyTextureStateShard.class,remap=false)
public abstract class TextureShardMixin implements SpectralBuffers.Texture {
    @Shadow(aliases={"m_142706_"}) protected abstract java.util.Optional<ResourceLocation> cutoutTexture();
    public java.util.Optional<ResourceLocation> wg$texture(){return cutoutTexture();}
}
