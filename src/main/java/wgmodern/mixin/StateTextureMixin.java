package wgmodern.mixin;
import org.spongepowered.asm.mixin.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import wgmodern.SpectralBuffers;
@Mixin(value=RenderType.CompositeState.class,remap=false)
public abstract class StateTextureMixin implements SpectralBuffers.Texture {
    @org.spongepowered.asm.mixin.gen.Accessor("textureState")
    protected abstract RenderStateShard.EmptyTextureStateShard wg$textureState();
    public java.util.Optional<ResourceLocation> wg$texture(){return ((SpectralBuffers.Texture)(Object)wg$textureState()).wg$texture();}
}
