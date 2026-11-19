package tc4revival.mixin;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo @Mixin(targets="com.github.tc4auto.service.NoteReader",remap=false)
public abstract class AutoResearchNoteMixin {
    @Inject(method="isResearchNote",at=@At("HEAD"),cancellable=true)
    private static void tc4$completed(ItemStack stack,CallbackInfoReturnable<Boolean> cir) {
        if(stack.is(tc4revival.content.TCItems.DISCOVERY.get()))cir.setReturnValue(true);
    }
}
