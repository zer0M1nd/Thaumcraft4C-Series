package thaumicomniverse.mixin;

import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** A wand's name has cap/rod parameters; its raw description key has no standalone translation. */
@Mixin(value=DeployerBlockEntity.class,remap=false)
public abstract class CreateDeployerNameMixin {
    @Shadow public ItemStack heldItem;
    @ModifyArg(method="addToGoggleTooltip",at=@At(value="INVOKE",
            target="Lcom/simibubi/create/foundation/utility/CreateLang;translate(Ljava/lang/String;[Ljava/lang/Object;)Lnet/createmod/catnip/lang/LangBuilder;"),
            index=1,remap=false)
    private Object[] omniverse$heldName(Object[] arguments){
        if(arguments.length==2&&heldItem.getItem() instanceof tc4revival.content.CastingWandItem){
            arguments=java.util.Arrays.copyOf(arguments,arguments.length);arguments[0]=heldItem.getHoverName();
        }
        return arguments;
    }
}
