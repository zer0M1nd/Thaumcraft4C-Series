package wgmodern.mixin;
import org.spongepowered.asm.mixin.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import wgmodern.SpectralBuffers;
@Mixin(targets="net.minecraft.client.renderer.RenderType$CompositeRenderType",remap=false)
public abstract class RenderTextureMixin implements SpectralBuffers.Texture {
    @org.spongepowered.asm.mixin.gen.Accessor("state")
    protected abstract RenderType.CompositeState wg$state();
    public java.util.Optional<ResourceLocation> wg$texture(){return ((SpectralBuffers.Texture)(Object)wg$state()).wg$texture();}
}
