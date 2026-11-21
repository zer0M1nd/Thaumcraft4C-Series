package emtmodern.energy;

import emtmodern.ElectroMagicTools;
import ic2.api.energy.prefab.BasicSink;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import net.minecraftforge.items.wrapper.InvWrapper;
import tc4revival.content.CastingWandItem;

public class WandChargerEntity extends BlockEntity implements Container,MenuProvider {
    public final BasicSink energy=new BasicSink(this,100000,4);
    protected NonNullList<ItemStack> items=NonNullList.withSize(1,ItemStack.EMPTY);
    protected LazyOptional<IItemHandler> inventory=LazyOptional.of(()->new InvWrapper(this));
    public WandChargerEntity(BlockPos pos,BlockState state){super(ElectroMagicTools.WAND_CHARGER_BE.get(),pos,state);}
    public static void tick(Level level,BlockPos pos,BlockState state,WandChargerEntity machine){
        if(level.isClientSide)return;machine.energy.update();ItemStack stack=machine.getItem(0);
        if(stack.getItem() instanceof CastingWandItem wand){
            var primals=tc4revival.aspect.TCAspects.catalog().primals();
            // GTNH maintenance fix: a full or nonrechargeable wand must not burn EU.
            boolean room=primals.stream().anyMatch(aspect->wand.receiveVis(stack,aspect,1,true)>0);
            if(room&&machine.energy.useEnergy(EmtFocusConfig.STATION_COST.get())){for(var aspect:primals)wand.addVis(stack,aspect,1,true,true);machine.setChanged();}
        }
    }
    @Override public void onLoad(){super.onLoad();if(level!=null&&!level.isClientSide)energy.onLoad();}
    @Override public void onChunkUnloaded(){energy.onChunkUnload();super.onChunkUnloaded();}
    @Override public void setRemoved(){energy.invalidate();super.setRemoved();}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);ContainerHelper.saveAllItems(tag,items);energy.writeToNBT(tag);}
    @Override public void load(CompoundTag tag){super.load(tag);items=NonNullList.withSize(1,ItemStack.EMPTY);ContainerHelper.loadAllItems(tag,items);energy.readFromNBT(tag);energy.setEnergyStored(Math.max(0,Math.min(100000,energy.getEnergyStored())));}
    @Override public int getContainerSize(){return 1;}
    @Override public boolean isEmpty(){return items.get(0).isEmpty();}
    @Override public ItemStack getItem(int slot){return slot==0?items.get(0):ItemStack.EMPTY;}
    @Override public ItemStack removeItem(int slot,int amount){var removed=ContainerHelper.removeItem(items,slot,amount);if(!removed.isEmpty())setChanged();return removed;}
    @Override public ItemStack removeItemNoUpdate(int slot){return ContainerHelper.takeItem(items,slot);}
    @Override public void setItem(int slot,ItemStack stack){items.set(slot,stack);if(stack.getCount()>64)stack.setCount(64);setChanged();}
    @Override public void clearContent(){items.set(0,ItemStack.EMPTY);setChanged();}
    @Override public boolean stillValid(Player player){return level!=null&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(worldPosition.getCenter())<=64;}
    @Override public Component getDisplayName(){return Component.translatable("gui.EMT.wandRecharge.title");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new WandChargerMenu(id,inventory,this);}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability,Direction face){return capability==ForgeCapabilities.ITEM_HANDLER?inventory.cast():super.getCapability(capability,face);}
    @Override public void invalidateCaps(){super.invalidateCaps();inventory.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();inventory=LazyOptional.of(()->new InvWrapper(this));}
}
