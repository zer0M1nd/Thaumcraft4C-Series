package thmodern.mixin;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.chunk.VisGraph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=VisGraph.class,remap=false)
public abstract class LensVisibilityMixin {
    @Inject(method={"setOpaque","m_112971_"},at=@At("HEAD"),cancellable=true)
    private void th$window(BlockPos pos,CallbackInfo callback){if(thmodern.client.EarthLensMask.hidden(pos))callback.cancel();}
}
