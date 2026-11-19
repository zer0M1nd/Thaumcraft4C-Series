package conciliummodern.mixin;

import conciliummodern.NetherMembraneRecipe;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep the original item entity, UUID, owner, pickup delay and custom capabilities. */
@Mixin(value=ItemEntity.class,remap=false)
public abstract class CoatedItemFireMixin {
    @Inject(method={"fireImmune()Z","m_5825_()Z"},at=@At("HEAD"),cancellable=true,require=1)
    private void concilium$immune(CallbackInfoReturnable<Boolean> result) {
        if(NetherMembraneRecipe.coated(((ItemEntity)(Object)this).getItem()))result.setReturnValue(true);
    }
    @Inject(method={"hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z","m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z"},at=@At("HEAD"),cancellable=true,require=1)
    private void concilium$fireproof(DamageSource source,float amount,CallbackInfoReturnable<Boolean> result) {
        if(source.is(DamageTypeTags.IS_FIRE) && NetherMembraneRecipe.coated(((ItemEntity)(Object)this).getItem()))result.setReturnValue(false);
    }
}
