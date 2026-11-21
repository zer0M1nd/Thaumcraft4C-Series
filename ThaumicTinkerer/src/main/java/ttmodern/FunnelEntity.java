package ttmodern;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import tc4revival.content.*;
import tc4revival.aspect.*;

public final class FunnelEntity extends BlockEntity implements RevealedAspectContainer {
    public final ItemStackHandler inventory=new ItemStackHandler(1) {
        @Override public int getSlotLimit(int slot){return 1;}
        @Override public boolean isItemValid(int slot,ItemStack stack){return stack.is(TCItems.FILLED_ESSENTIA_JAR.get())||stack.is(TCItems.FILLED_VOID_JAR.get());}
        @Override protected void onContentsChanged(int slot){changed();}
    };
    private final IItemHandler bottom=new IItemHandler() {
        public int getSlots(){return 1;} public ItemStack getStackInSlot(int slot){return inventory.getStackInSlot(slot);}
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return inventory.insertItem(slot,stack,simulate);}
        public ItemStack extractItem(int slot,int amount,boolean simulate){return ItemStack.EMPTY;}
        public int getSlotLimit(int slot){return 1;} public boolean isItemValid(int slot,ItemStack stack){return inventory.isItemValid(slot,stack);}
    };
    private LazyOptional<IItemHandler> access=LazyOptional.of(()->inventory),down=LazyOptional.of(()->bottom);
    public FunnelEntity(BlockPos pos,BlockState state){super(Tinkerer.FUNNEL_ENTITY.get(),pos,state);}
    private void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public ItemStack remove(){var stack=inventory.getStackInSlot(0);inventory.setStackInSlot(0,ItemStack.EMPTY);return stack;}
    public void tick() {
        var stack=inventory.getStackInSlot(0);if(level==null||stack.isEmpty()||!(stack.getItem() instanceof EssentiaJarItem item))return;
        var values=item.storedAspects(stack);if(values.size()!=1)return;var aspect=values.aspects().get(0);if(values.amount(aspect)<=0)return;
        var below=worldPosition.below();var hopper=level.getBlockState(below);if(!(hopper.getBlock() instanceof HopperBlock))return;
        var target=below.relative(hopper.getValue(HopperBlock.FACING));if(!level.hasChunkAt(target)||!(level.getBlockEntity(target) instanceof EssentiaJarBlockEntity jar))return;
        if(!jar.accepts(aspect)||jar.addToContainer(aspect,1)!=0)return;
        values.reduce(aspect,1);item.setStoredAspects(stack,values);
        if(values.amount(aspect)==0&&(!stack.hasTag()||!stack.getTag().contains("AspectFilter"))) {
            var empty=new ItemStack(stack.is(TCItems.FILLED_VOID_JAR.get())?TCItems.VOID_JAR.get():TCItems.ESSENTIA_JAR.get());
            if(stack.hasTag())empty.setTag(stack.getTag().copy());inventory.setStackInSlot(0,empty);
        }
        changed();
    }
    @Override public AspectAmounts revealedAspects(){var stack=inventory.getStackInSlot(0);return stack.getItem() instanceof StoredAspectItem container?container.storedAspects(stack):new AspectAmounts();}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.put("Inventory",inventory.serializeNBT());}
    @Override public void load(CompoundTag tag){super.load(tag);inventory.deserializeNBT(tag.getCompound("Inventory"));}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability,Direction side){return capability==ForgeCapabilities.ITEM_HANDLER?(side==Direction.DOWN?down:access).cast():super.getCapability(capability,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();access.invalidate();down.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();access=LazyOptional.of(()->inventory);down=LazyOptional.of(()->bottom);}
}
