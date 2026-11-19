package conciliummodern.mixin;

import conciliummodern.DoomCatWitcheryHost;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** The old ASM event ran only after a bound contract successfully affected its victim. */
@Pseudo
@Mixin(targets="com.emoniph.witchery.entity.ImpEntity",remap=false)
public abstract class WitcheryImpDealMixin {
    @Redirect(method="workContract",at=@At(value="INVOKE",target="Lcom/emoniph/witchery/item/ContractItem;activate(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)Z"),remap=false)
    private boolean concilium$afterSuccessfulContract(@Coerce Object contract,ItemStack stack,LivingEntity target,Player player,ItemStack held){
        return DoomCatWitcheryHost.activateContract(contract,stack,target,player);
    }
}
