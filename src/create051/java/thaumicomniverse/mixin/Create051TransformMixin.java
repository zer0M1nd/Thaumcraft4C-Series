package thaumicomniverse.mixin;
import com.simibubi.create.content.contraptions.StructureTransform;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import thaumicomniverse.compat.create.LegacyTransforms;
@Mixin(value=StructureTransform.class,remap=false)
public abstract class Create051TransformMixin {
    @Inject(method="apply(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/state/BlockState;",at=@At("HEAD"),cancellable=true)
    private void omniverse$state(BlockState state,CallbackInfoReturnable<BlockState> ci){
        var transformer=LegacyTransforms.BLOCK_TRANSFORMERS.get(state.getBlock());
        if(transformer!=null)ci.setReturnValue(transformer.apply(state,(StructureTransform)(Object)this));
    }
    @Inject(method="apply(Lnet/minecraft/world/level/block/entity/BlockEntity;)V",at=@At("TAIL"))
    private void omniverse$tile(BlockEntity tile,CallbackInfo ci){
        // The host calls apply(null) for ordinary blocks during disassembly.
        if(tile==null)return;
        var transformer=LegacyTransforms.BLOCK_ENTITY_TRANSFORMERS.get(tile.getType());
        if(transformer!=null)transformer.accept(tile,(StructureTransform)(Object)this);
    }
}
