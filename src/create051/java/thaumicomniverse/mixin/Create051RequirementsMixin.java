package thaumicomniverse.mixin;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumicomniverse.compat.create.LegacyRequirements;
@Mixin(value=ItemRequirement.class,remap=false)
public abstract class Create051RequirementsMixin {
    @Inject(method="of(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)Lcom/simibubi/create/content/schematics/requirement/ItemRequirement;",at=@At("HEAD"),cancellable=true)
    private static void omniverse$requirements(BlockState state,BlockEntity tile,CallbackInfoReturnable<ItemRequirement> ci){
        var requirements=LegacyRequirements.BLOCKS.get(state.getBlock());
        if(requirements!=null)ci.setReturnValue(requirements.apply(state,tile));
    }
}
