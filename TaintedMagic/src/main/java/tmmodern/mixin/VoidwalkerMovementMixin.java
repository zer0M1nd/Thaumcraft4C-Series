package tmmodern.mixin;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;import net.minecraft.world.entity.player.Player;import net.minecraft.world.entity.EquipmentSlot;
@Mixin(value=Player.class,remap=false)
public class VoidwalkerMovementMixin {
    @Inject(method={"getFlyingSpeed","m_274460_"},at=@At("RETURN"),cancellable=true,require=1)
    private void tm$airSpeed(CallbackInfoReturnable<Float> c){var p=(Player)(Object)this;if(p.zza>0&&!p.onGround()&&!p.getAbilities().flying&&p.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof tmmodern.TMArmor.Boots)c.setReturnValue(tc4revival.content.HoverFlight.active(p)?.03f:p.isSprinting()?.045f:.04f);}
}
