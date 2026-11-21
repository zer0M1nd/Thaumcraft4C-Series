package thaumicomniverse.mixin;
import com.simibubi.create.content.contraptions.MountedStorage;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumicomniverse.compat.create.ArcaneContraptions;
@Mixin(value=MountedStorage.class,remap=false)
public abstract class Create051MountedInventoryMixin {
    @Inject(method="canUseModdedInventory",at=@At("HEAD"),cancellable=true)
    private static void omniverse$inventory(BlockEntity tile,IItemHandler handler,CallbackInfoReturnable<Boolean> ci){
        if(tile==null)return;
        if(handler instanceof IItemHandlerModifiable && ArcaneContraptions.thaumic(tile.getBlockState())
                && !tile.getBlockState().is(tc4revival.content.TCArcane.ITEM_MIRROR.get())
                && !tile.getBlockState().is(tc4revival.content.TCArcane.CRUCIBLE.get())
                && !com.simibubi.create.AllTags.AllBlockTags.CONTRAPTION_INVENTORY_DENY.matches(tile.getBlockState()))ci.setReturnValue(true);
    }
}
