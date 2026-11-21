package thaumicomniverse.mixin;

import cy.jdkdigital.productivebees.common.block.entity.IncubatorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumicomniverse.compat.ProductiveFeatureRules;

@Mixin(value=IncubatorBlockEntity.class,remap=false)
public abstract class ProductiveFeatureIncubatorMixin {
    @Inject(method="canProcessInput",at=@At("HEAD"),cancellable=true,remap=false)
    private void omniverse$input(IItemHandlerModifiable inventory,CallbackInfoReturnable<Boolean> ci) {
        if(ProductiveFeatureRules.incubationDisabled(inventory))ci.setReturnValue(false);
    }
    @Inject(method="tick",at=@At("HEAD"),cancellable=true,remap=false)
    private static void omniverse$pause(Level level,BlockPos pos,BlockState state,IncubatorBlockEntity tile,CallbackInfo ci) {
        if(tile.getCapability(ForgeCapabilities.ITEM_HANDLER).map(ProductiveFeatureRules::incubationDisabled).orElse(false)) {
            tile.isRunning=false;ci.cancel();
        }
    }
}
