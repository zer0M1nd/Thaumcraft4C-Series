package thaumicomniverse.compat.create;

import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.ItemHandlerHelper;

public class BeltTransfer {
    /** Create passes the direction of travel, whereas Forge capabilities take the receiving face. */
    public static ItemStack export(Level level, BlockPos source, Direction travel, ItemStack stack) {
        if(travel==null || stack.isEmpty())return stack;
        var pos=source.relative(travel);
        if(!level.hasChunkAt(pos))return stack;
        var direct=BlockEntityBehaviour.get(level,pos,DirectBeltInputBehaviour.TYPE);
        if(direct!=null && direct.canInsertFromSide(travel) && !direct.isOccupied(travel))return direct.handleInsertion(stack.copy(),travel,false);
        var target=level.getBlockEntity(pos);
        if(target==null)return stack;
        return target.getCapability(ForgeCapabilities.ITEM_HANDLER,travel.getOpposite()).resolve()
                .map(handler->ItemHandlerHelper.insertItemStacked(handler,stack.copy(),false)).orElse(stack);
    }
}
