package thaumicomniverse.compat.create;

import java.util.List;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.*;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.*;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.ItemHandlerHelper;
import tc4revival.content.*;

public final class MirrorBridge extends SmartBlockEntity implements thaumcraft.api.devices.DeviceExtensions.Component {
    private final ItemMirrorBlockEntity source;
    public MirrorBridge(ItemMirrorBlockEntity source){super(source.getType(),source.getBlockPos(),source.getBlockState());this.source=source;setLevel(source.getLevel());}
    @Override public void extensionAttached(){setLevel(source.getLevel());}
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours){
        behaviours.add(new BeltProcessingBehaviour(this).whenItemEnters(this::accept).whileItemHeld(this::accept));
    }
    private BeltProcessingBehaviour.ProcessingResult accept(TransportedItemStack input,TransportedItemStackHandlerBehaviour handler){
        setLevel(source.getLevel());
        if(level==null||source.isRemoved()||!thaumicomniverse.RevivalFeatures.Group.CREATE.enabled()
                ||!(level.getBlockEntity(source.getBlockPos().below()) instanceof BeltBlockEntity belt)
                ||belt.getSpeed()==0||belt.getMovementFacing()!=source.facing().getOpposite())
            return BeltProcessingBehaviour.ProcessingResult.PASS;
        if(level.isClientSide)return BeltProcessingBehaviour.ProcessingResult.HOLD;
        if(!source.valid(false))return BeltProcessingBehaviour.ProcessingResult.HOLD;
        var target=source.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.DOWN).resolve();
        if(target.isEmpty())return BeltProcessingBehaviour.ProcessingResult.HOLD;
        input.stack=ItemHandlerHelper.insertItemStacked(target.get(),input.stack.copy(),false);
        handler.blockEntity.notifyUpdate();
        return input.stack.isEmpty()?BeltProcessingBehaviour.ProcessingResult.REMOVE:BeltProcessingBehaviour.ProcessingResult.HOLD;
    }
    /** Null leaves ordinary mirror spawning in charge; zero keeps the private output queue intact. */
    public static Integer output(ItemMirrorBlockEntity mirror,ItemStack stack){
        var level=mirror.getLevel();var pos=mirror.getBlockPos().below();
        if(!thaumicomniverse.RevivalFeatures.Group.CREATE.enabled()||level==null||level.isClientSide
                ||level.getBlockEntity(mirror.getBlockPos())!=mirror)return null;
        return output(level,pos,Direction.DOWN,stack);
    }
    public static Integer output(net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,Direction travel,ItemStack stack){
        if(!level.hasChunkAt(pos))return null;
        var target=level.getBlockEntity(pos);
        if(!WorkbenchChargerBlockEntity.openContainer(level,pos)
                &&!(target instanceof com.simibubi.create.content.contraptions.actors.psi.PortableItemInterfaceBlockEntity))return null;
        var direct=BlockEntityBehaviour.get(level,pos,DirectBeltInputBehaviour.TYPE);
        if(direct!=null){
            if(!direct.canInsertFromSide(travel)||direct.isOccupied(travel))return 0;
            return stack.getCount()-direct.handleInsertion(stack.copy(),travel,false).getCount();
        }
        var handler=target==null?null:target.getCapability(ForgeCapabilities.ITEM_HANDLER,travel.getOpposite()).resolve().orElse(null);
        if(handler==null)return 0;
        return stack.getCount()-ItemHandlerHelper.insertItemStacked(handler,stack.copy(),false).getCount();
    }
}
