package infusionbrewing.mixin;

import infusionbrewing.BrewingConfig;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class PotionStackSizeMixin {
    @Inject(method={"getMaxStackSize()I","m_41459_()I"}, at=@At("HEAD"), cancellable=true, remap=false)
    private void infusionbrewing$stackSize(CallbackInfoReturnable<Integer> cir) {
        if ((Object)this==Items.POTION || (Object)this==Items.SPLASH_POTION) cir.setReturnValue(BrewingConfig.stackSize());
    }
}
