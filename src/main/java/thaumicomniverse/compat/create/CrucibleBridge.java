package thaumicomniverse.compat.create;

import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import tc4revival.content.CrucibleBlockEntity;
import tc4revival.content.CrucibleRecipe;
import tc4revival.research.KnowledgeSavedData;
import tc4revival.research.PlayerKnowledge;

/** Independent belt buffer. Dropped items continue to use TC4's original collision/dissolution path. */
public class CrucibleBridge extends SmartBlockEntity implements thaumcraft.api.devices.DeviceExtensions.Component {
    private final CrucibleBlockEntity source;
    private ItemStack input=ItemStack.EMPTY,output=ItemStack.EMPTY;
    private Direction travel;
    private int progress,exitTicks;
    private long visualStartedAt;
    private long capabilityGeneration;
    private boolean capabilityValid=true;
    private LazyOptional<IItemHandler> capability=LazyOptional.of(()->new Handler());
    public CrucibleBridge(CrucibleBlockEntity source) {
        super(source.getType(),source.getBlockPos(),source.getBlockState());this.source=source;setLevel(source.getLevel());
    }
    @Override public void extensionAttached(){setLevel(source.getLevel());}
    @Override public void extensionTick(){tick();}
    @Override public CompoundTag extensionSave(){return save();}
    @Override public void extensionLoad(CompoundTag tag){restore(tag);}
    @Override public void extensionInvalidate(){invalidate();}
    @Override public void extensionRevive(){revive();}
    @Override public void extensionRemoved(thaumcraft.api.devices.DeviceExtensions.Removal reason){if(reason==thaumcraft.api.devices.DeviceExtensions.Removal.DESTROYED||reason==thaumcraft.api.devices.DeviceExtensions.Removal.REPLACED)drop();}
    @Override public <T> LazyOptional<T> extensionCapability(net.minecraftforge.common.capabilities.Capability<T> cap,Direction side){return cap==net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER?capability().cast():LazyOptional.empty();}
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(new DirectBeltInputBehaviour(this).allowingBeltFunnels()
                .considerOccupiedWhen(direction->!input.isEmpty() || !output.isEmpty())
                .setInsertionHandler((stack,direction,simulate)->insert(stack.stack,direction,simulate)));
    }
    private boolean enabled() { return capabilityValid && thaumicomniverse.RevivalFeatures.Group.CREATE.enabled() && !source.isRemoved(); }
    public ItemStack insert(ItemStack stack,Direction direction,boolean simulate) {
        if(!enabled() || !input.isEmpty() || !output.isEmpty() || stack.isEmpty())return stack;
        if(!simulate){input=stack.copyWithCount(1);travel=direction!=null && direction.getAxis().isHorizontal()?direction:null;progress=0;source.synchronize();}
        return stack.copyWithCount(stack.getCount()-1);
    }
    @Override public void tick() {
        setLevel(source.getLevel());
        if(source.getLevel()!=null&&source.getLevel().isClientSide)return;
        if(!(source.getLevel() instanceof ServerLevel server) || !enabled())return;
        if(!output.isEmpty()) {
            if(exitTicks++<8)return;
            var rest=BeltTransfer.export(server,source.getBlockPos(),travel,output);
            if(ItemStack.matches(rest,output)) {
                var funnel=getBehaviour(DirectBeltInputBehaviour.TYPE).tryExportingToBeltFunnel(output,null,false);
                if(funnel!=null)rest=funnel;
            }
            if(!ItemStack.matches(rest,output)){output=rest.copy();source.synchronize();}
            return;
        }
        if(input.isEmpty())return;
        var result=source.processAutomation(input);
        if(result.isEmpty())return;
        input=ItemStack.EMPTY;output=result.get().copy();exitTicks=0;source.synchronize();
    }

    public LazyOptional<IItemHandler> capability(){return capability;}
    public void invalidate(){capabilityValid=false;capabilityGeneration++;capability.invalidate();}
    public void revive(){capability.invalidate();capabilityGeneration++;capabilityValid=true;capability=LazyOptional.of(Handler::new);}
    public CompoundTag save() {
        var tag=new CompoundTag();tag.put("Input",input.save(new CompoundTag()));tag.put("Output",output.save(new CompoundTag()));
        if(travel!=null)tag.putString("Travel",travel.getName());tag.putInt("Progress",progress);tag.putInt("ExitTicks",exitTicks);return tag;
    }
    public void restore(CompoundTag tag) {
        var nextInput=ItemStack.of(tag.getCompound("Input"));var nextOutput=ItemStack.of(tag.getCompound("Output"));
        if(!ItemStack.matches(input,nextInput)||!ItemStack.matches(output,nextOutput))visualStartedAt=source.getLevel()==null?0:source.getLevel().getGameTime();
        input=nextInput;output=nextOutput;travel=Direction.byName(tag.getString("Travel"));progress=Math.max(0,Math.min(19,tag.getInt("Progress")));exitTicks=Math.max(0,Math.min(8,tag.getInt("ExitTicks")));
    }
    public ItemStack visualStack(){return output.isEmpty()?input:output;}
    public Direction visualDirection(){return travel;}
    public float visualPosition(float partial){float age=source.getLevel()==null?1:Math.max(0,Math.min(1,(source.getLevel().getGameTime()-visualStartedAt+partial)/8));return output.isEmpty()?age-1:age;}
    public void drop() {
        if(source.getLevel()==null||source.getLevel().isClientSide)return;
        var pos=source.getBlockPos();
        for(var stack:List.of(input,output))if(!stack.isEmpty())Containers.dropItemStack(source.getLevel(),pos.getX()+.5,pos.getY()+1.2,pos.getZ()+.5,stack.copy());
        input=output=ItemStack.EMPTY;source.setChanged();
    }
    @Override public void notifyUpdate(){if(source!=null)source.synchronize();}
    @Override public void sendData(){if(source!=null)source.synchronize();}
    private class Handler implements IItemHandler {
        private final long generation=capabilityGeneration;
        private boolean valid(){return capabilityValid&&generation==capabilityGeneration&&enabled();}
        @Override public int getSlots(){return 2;}
        @Override public ItemStack getStackInSlot(int slot){return !valid()?ItemStack.EMPTY:slot==0?input.copy():slot==1?output.copy():ItemStack.EMPTY;}
        @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return valid()&&slot==0?insert(stack,null,simulate):stack;}
        @Override public ItemStack extractItem(int slot,int amount,boolean simulate){
            var stack=getStackInSlot(slot);if(slot!=1||amount<=0||stack.isEmpty())return ItemStack.EMPTY;
            var extracted=stack.copyWithCount(Math.min(amount,stack.getCount()));
            if(!simulate){var rest=stack.copyWithCount(stack.getCount()-extracted.getCount());if(slot==0)input=rest;else output=rest;source.synchronize();}return extracted;
        }
        @Override public int getSlotLimit(int slot){return slot==0?1:64;}
        @Override public boolean isItemValid(int slot,ItemStack stack){return valid()&&slot==0;}
    }
}
