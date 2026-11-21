package thaumicomniverse.mixin;
import com.simibubi.create.content.equipment.armor.BacktankRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumicomniverse.compat.create.*;
@Mixin(value=BacktankRenderer.class,remap=false)
public class CreateBacktankModelMixin {
    @Inject(method="getCogsModel",at=@At("HEAD"),cancellable=true)
    private static void omniverse$cogs(BlockState state,CallbackInfoReturnable<PartialModel> cir){if(state.is(ArcaneCreateContent.BACKTANK.get()))cir.setReturnValue(ArcaneMachineRenderers.TANK_COGS);}
    @Inject(method="getShaftModel",at=@At("HEAD"),cancellable=true)
    private static void omniverse$shaft(BlockState state,CallbackInfoReturnable<PartialModel> cir){if(state.is(ArcaneCreateContent.BACKTANK.get()))cir.setReturnValue(ArcaneMachineRenderers.TANK_SHAFT);}
}
