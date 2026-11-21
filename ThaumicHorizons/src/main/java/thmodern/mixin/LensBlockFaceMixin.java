package thmodern.mixin;
import net.minecraft.core.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=Block.class,remap=false)
public abstract class LensBlockFaceMixin {
    @Inject(method={"shouldRenderFace","m_152444_"},at=@At("HEAD"),cancellable=true)
    private static void th$exposedFace(BlockState state,BlockGetter world,BlockPos pos,Direction side,BlockPos adjacent,CallbackInfoReturnable<Boolean> callback){if(thmodern.client.EarthLensMask.hidden(adjacent))callback.setReturnValue(true);}
}
