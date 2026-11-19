package thaumicomniverse.mixin;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import slimeknights.tconstruct.library.tools.definition.module.mining.IsEffectiveModule;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import thaumicomniverse.compat.tinkers.BedrockMining;

@Mixin(value=IsEffectiveModule.class,remap=false)
public abstract class TinkersBedrockEffectiveMixin {
    @Inject(method="isToolEffective",at=@At("HEAD"),cancellable=true,remap=false)
    private void omniverse$bedrock(IToolStackView tool,BlockState state,CallbackInfoReturnable<Boolean> ci) {
        if(state.is(Blocks.BEDROCK) && BedrockMining.pick(tool))ci.setReturnValue(true);
    }
}
