package wgmodern.mixin;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** BLOCK use pose must not accidentally turn the old half-damage stance into a modern shield. */
@Mixin(value=LivingEntity.class,remap=false)
public class PrimordialBlockingMixin {
 @Inject(method={"isDamageSourceBlocked","m_21275_"},at=@At("HEAD"),cancellable=true)private void noShield(DamageSource source,CallbackInfoReturnable<Boolean> ci){var self=(LivingEntity)(Object)this;if(self.isUsingItem()&&self.getUseItem().getItem() instanceof wgmodern.PrimordialGear.Weapon)ci.setReturnValue(false);}
}
