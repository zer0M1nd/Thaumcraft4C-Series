package thaumicomniverse.mixin;

import com.simibubi.create.foundation.utility.BlockHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumicomniverse.compat.create.ArcaneContraptions;

@Mixin(value=BlockHelper.class,remap=false)
public class CreateSchematicPlacementMixin {
    @ModifyVariable(method="placeSchematicBlock",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private static BlockState omniverse$legacyState(BlockState value,Level level,BlockState original,BlockPos target,ItemStack stack,CompoundTag data){
        if(thaumicomniverse.compat.create.ArcaneSchematic.hasStateFilters()||!ArcaneContraptions.thaumic(value))return value;
        return thaumicomniverse.compat.create.ArcaneSchematic.state(level.getBlockEntity(target),value);
    }
    @Inject(method="placeSchematicBlock",at=@At("TAIL"))
    private static void omniverse$restoreSettings(Level level,BlockState state,BlockPos target,ItemStack stack,CompoundTag data,CallbackInfo ci){
        if(data==null||!ArcaneContraptions.enabled()||!ArcaneContraptions.thaumic(state))return;
        var tile=level.getBlockEntity(target);
        if(tile==null||!tile.getBlockState().is(state.getBlock()))return;
        // Create loads its sanitized data before setPlacedBy. Portable jars and other
        // item-backed blocks then reset it from the plain construction material.
        tile.load(data.copy());tile.setChanged();
        level.sendBlockUpdated(target,tile.getBlockState(),tile.getBlockState(),3);
    }
}
