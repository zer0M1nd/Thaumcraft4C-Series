package tc4revival.mixin;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import tc4revival.compat.AutoResearchCompat;
@Pseudo @Mixin(targets="com.github.tc4auto.service.BatchQueue",remap=false)
public abstract class AutoResearchBatchMixin {
    @Inject(method="start",at=@At("HEAD"),cancellable=true)
    private static void tc4$start(ServerPlayer p,CallbackInfoReturnable<Integer> ci){ci.setReturnValue(AutoResearchCompat.start(p));}
    @Inject(method="advance",at=@At("HEAD"),cancellable=true)
    private static void tc4$advance(ServerPlayer p,CallbackInfoReturnable<Boolean> ci){ci.setReturnValue(AutoResearchCompat.advance(p));}
    @Inject(method="remaining",at=@At("HEAD"),cancellable=true)
    private static void tc4$remaining(ServerPlayer p,CallbackInfoReturnable<Integer> ci){ci.setReturnValue(AutoResearchCompat.remaining(p));}
    @Inject(method="isRunning",at=@At("HEAD"),cancellable=true)
    private static void tc4$running(ServerPlayer p,CallbackInfoReturnable<Boolean> ci){ci.setReturnValue(AutoResearchCompat.running(p));}
    @Inject(method="onNoteDone",at=@At("HEAD"),cancellable=true)
    private static void tc4$done(ServerPlayer p,boolean success,CallbackInfo ci){AutoResearchCompat.done(p,success);ci.cancel();}
    @Inject(method={"stop","onPlayerLogout"},at=@At("HEAD"),cancellable=true)
    private static void tc4$stop(ServerPlayer p,CallbackInfo ci){AutoResearchCompat.stop(p);ci.cancel();}
}
