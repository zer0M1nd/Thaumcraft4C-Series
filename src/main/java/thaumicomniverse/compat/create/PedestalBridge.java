package thaumicomniverse.compat.create;

import com.simibubi.create.content.kinetics.belt.behaviour.*;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.ItemHandlerHelper;
import tc4revival.content.ArcanePedestalBlockEntity;

/** Create behaviour host only: the real TC4 block entity remains the inventory and renderer. */
public final class PedestalBridge extends SmartBlockEntity implements thaumcraft.api.devices.DeviceExtensions.Component {
    private final ArcanePedestalBlockEntity source;
    private TransportedItemStack transported;
    private Direction travel;
    private ItemStack completed=ItemStack.EMPTY;

    public PedestalBridge(ArcanePedestalBlockEntity source) {
        super(source.getType(), source.getBlockPos(), source.getBlockState());
        this.source = source;
        setLevel(source.getLevel());
    }
    @Override public void extensionAttached(){setLevel(source.getLevel());}
    @Override public void extensionTick(){if(thaumicomniverse.RevivalFeatures.Group.CREATE.enabled())tick();}
    @Override public CompoundTag extensionSave(){return processingState();}
    @Override public void extensionLoad(CompoundTag tag){restore(tag);}
    @Override public void extensionInventoryChanged(thaumcraft.api.devices.DeviceExtensions.InventoryChange change){
        if(change==thaumcraft.api.devices.DeviceExtensions.InventoryChange.REPLACED||change==thaumcraft.api.devices.DeviceExtensions.InventoryChange.CLEARED)inventoryReplaced();
        else if(change==thaumcraft.api.devices.DeviceExtensions.InventoryChange.EXTRACTED&&source.isEmpty())inventoryReplaced();
    }
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(new DirectBeltInputBehaviour(this).allowingBeltFunnels()
                .considerOccupiedWhen(direction -> !source.isEmpty())
                .setInsertionHandler((stack, direction, simulate) -> {
                    var handler = source.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite()).orElse(null);
                    var remainder=handler == null ? stack.stack : ItemHandlerHelper.insertItem(handler, stack.stack, simulate);
                    if(!simulate && remainder.getCount()<stack.stack.getCount()) {
                        travel=direction.getAxis().isHorizontal()?direction:null;completed=ItemStack.EMPTY;transported=null;source.sync();
                    }
                    return remainder;
                }));
        behaviours.add(new TransportedItemStackHandlerBehaviour(this, this::process)
                .withStackPlacement(stack -> Vec3.atCenterOf(getBlockPos()).add(0, .5, 0)));
    }
    private void refresh() {
        setLevel(source.getLevel());
        ItemStack stack = source.getItem(0);
        if (stack.isEmpty()) {transported = null;travel=null;completed=ItemStack.EMPTY;}
        else if (transported == null || !ItemStack.matches(transported.stack, stack)) {
            transported = new TransportedItemStack(stack.copy());
            transported.beltPosition = transported.prevBeltPosition = .5f;
        }
    }
    private void commit() {
        ItemStack stack = transported == null ? ItemStack.EMPTY : transported.stack;
        // Processing, like infusion, may produce multiple items from one input.
        if (!ItemStack.matches(source.getItem(0), stack)) source.setFromInfusion(stack.copy());
        else source.sync();
    }
    private void process(float distance, Function<TransportedItemStack, TransportedItemStackHandlerBehaviour.TransportedResult> action) {
        refresh();
        if (transported == null || source.getLevel() == null || source.getLevel().isClientSide) return;
        ItemStack before = transported.stack.copy();
        var result = action.apply(transported);
        if (result == null) return;
        if (result.didntChangeFrom(before)) { commit(); return; }
        transported = result.hasHeldOutput() ? result.getHeldOutput() : null;
        for (TransportedItemStack output : result.getOutputs()) {
            if (transported == null || transported.stack.isEmpty()) transported = output;
            else Containers.dropItemStack(source.getLevel(), getBlockPos().getX() + .5,
                    getBlockPos().getY() + 1, getBlockPos().getZ() + .5, output.stack.copy());
        }
        commit();
    }
    @Override public void tick() {
        refresh();
        if (transported == null || source.getLevel() == null || source.getLevel().isClientSide) return;
        if(!completed.isEmpty()) {
            if(!ItemStack.matches(completed,source.getItem(0)))completed=ItemStack.EMPTY;
            else {
                var rest=BeltTransfer.export(source.getLevel(),source.getBlockPos(),travel,completed);
                if(!ItemStack.matches(rest,completed)) {
                    completed=rest.copy();transported=rest.isEmpty()?null:new TransportedItemStack(rest.copy());commit();return;
                }
            }
        }
        ItemStack remainder = getBehaviour(DirectBeltInputBehaviour.TYPE)
                .tryExportingToBeltFunnel(transported.stack, null, false);
        if (remainder != null && remainder.getCount() != transported.stack.getCount()) {
            if (remainder.isEmpty()) transported = null;
            else transported.stack = remainder;
            commit();
            return;
        }
        var processing = BlockEntityBehaviour.get(source.getLevel(), getBlockPos().above(2), BeltProcessingBehaviour.TYPE);
        if (processing != null && (transported.locked || !BeltProcessingBehaviour.isBlocked(source.getLevel(), getBlockPos()))) {
            var handler = getBehaviour(TransportedItemStackHandlerBehaviour.TYPE);
            var result = transported.locked ? processing.handleHeldItem(transported, handler) : processing.handleReceivedItem(transported, handler);
            if (result == BeltProcessingBehaviour.ProcessingResult.REMOVE) transported = null;
            else if (transported != null) transported.locked = result == BeltProcessingBehaviour.ProcessingResult.HOLD;
            commit();
        }
    }
    @Override public void notifyUpdate() { if (source != null) commit(); }
    @Override public void sendData() { if (source != null) commit(); }
    public void infusionFinished() { if(travel!=null){completed=source.getItem(0).copy();source.sync();} }
    /** An ordinary inventory replacement starts a new operation, even if the replacement item is identical. */
    public void inventoryReplaced(){travel=null;completed=ItemStack.EMPTY;transported=null;}
    public CompoundTag processingState() {
        refresh();var tag=transported==null?new CompoundTag():transported.serializeNBT();
        if(travel!=null)tag.putString("OmniverseTravel",travel.getName());
        if(!completed.isEmpty())tag.put("OmniverseCompleted",completed.save(new CompoundTag()));return tag;
    }
    public void restore(CompoundTag tag) {
        transported = tag.isEmpty() ? null : TransportedItemStack.read(tag);
        travel=Direction.byName(tag.getString("OmniverseTravel"));completed=ItemStack.of(tag.getCompound("OmniverseCompleted"));
        refresh();
    }
}
