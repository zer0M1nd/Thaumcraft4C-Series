package tmmodern.mixin;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;import net.minecraft.world.entity.player.Player;import net.minecraft.world.damagesource.*;
@Mixin(value=DamageSources.class,remap=false)
public class KatanaDamageMixin {
    @Inject(method={"playerAttack","m_269075_"},at=@At("HEAD"),cancellable=true)
    private void tm$katanaSource(Player p,CallbackInfoReturnable<DamageSource> c){var s=p.getMainHandItem();if(s.getItem() instanceof tmmodern.KatanaItem&&s.hasTag()&&s.getTag().contains("inscription"))c.setReturnValue(tmmodern.KatanaItem.source(p,s));}
}
