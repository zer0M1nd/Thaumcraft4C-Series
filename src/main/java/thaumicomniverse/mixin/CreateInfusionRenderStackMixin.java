package thaumicomniverse.mixin;

import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import thaumicomniverse.compat.create.ArcaneCreateContent;
import thaumicomniverse.compat.create.SequenceAspects;

/** Pass the real input through tint providers and custom item renderers as well as the baked model. */
@Mixin(value=ItemRenderer.class,remap=false)
public abstract class CreateInfusionRenderStackMixin {
    @ModifyVariable(method={"render","m_115143_"},remap=false,
            at=@At("HEAD"),argsOnly=true,ordinal=0)
    private ItemStack omniverse$renderInput(ItemStack stack){
        if(stack.is(ArcaneCreateContent.INCOMPLETE_INFUSION.get())&&stack.hasTag()){
            var input=ItemStack.of(stack.getTag().getCompound(SequenceAspects.ORIGINAL));
            if(!input.isEmpty()&&!input.is(ArcaneCreateContent.INCOMPLETE_INFUSION.get()))return input;
        }
        return stack;
    }
}
