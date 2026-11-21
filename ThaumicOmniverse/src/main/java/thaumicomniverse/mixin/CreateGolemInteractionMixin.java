package thaumicomniverse.mixin;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumicomniverse.compat.create.MovingGolemBindings;

@Mixin(value=AbstractContraptionEntity.class,remap=false)
public class CreateGolemInteractionMixin {
    @Inject(method="handlePlayerInteraction",at=@At("HEAD"),cancellable=true)
    private void omniverse$golems(Player player,BlockPos local,Direction side,InteractionHand hand,CallbackInfoReturnable<Boolean> cir){
        if(MovingGolemBindings.interact((AbstractContraptionEntity)(Object)this,player,local,side,hand))cir.setReturnValue(true);
    }
}
