package conciliummodern.mixin;

import conciliummodern.NetherMembraneRecipe;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** setSecondsOnFire is inherited by ItemEntity, so inject at its actual declaring class. */
@Mixin(value=Entity.class,remap=false)
public abstract class CoatedEntityFireMixin {
    @Inject(method={"setSecondsOnFire(I)V","m_20254_(I)V"},at=@At("HEAD"),cancellable=true,require=1)
    private void concilium$preventIgnition(int seconds,CallbackInfo result) {
        if((Object)this instanceof ItemEntity item&&NetherMembraneRecipe.coated(item.getItem()))result.cancel();
    }
}
