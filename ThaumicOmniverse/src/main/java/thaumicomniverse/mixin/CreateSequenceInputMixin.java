package thaumicomniverse.mixin;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumicomniverse.compat.create.SequenceAspects;

@Mixin(value=SequencedAssemblyRecipe.class, remap=false)
public abstract class CreateSequenceInputMixin {
    @Inject(method="advance", at=@At("RETURN"))
    private void omniverse$retainInput(ItemStack input, CallbackInfoReturnable<ItemStack> cir) {
        var result=cir.getReturnValue();
        if(result.isEmpty() || !result.hasTag() || !result.getTag().contains("SequencedAssembly"))return;
        CompoundTag original;
        if(input.hasTag() && input.getTag().contains(SequenceAspects.ORIGINAL))original=input.getTag().getCompound(SequenceAspects.ORIGINAL).copy();
        else if(input.hasTag() && input.getTag().contains("SequencedAssembly"))return;
        else original=input.copyWithCount(1).save(new CompoundTag());
        result.getOrCreateTag().put(SequenceAspects.ORIGINAL,original);
    }
}
