package thmodern.vat;

import net.minecraft.core.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.*;
import tc4revival.content.EssentiaTransport;
import thmodern.Horizons;

public final class VatPartEntity extends BlockEntity implements EssentiaTransport {
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> items=net.minecraftforge.common.util.LazyOptional.of(this::handler);
    public VatPartEntity(BlockPos pos,BlockState state){super(Horizons.VAT_PART_ENTITY.get(),pos,state);}
    public VatEntity controller(){return level==null?null:VatStructure.controller(level,worldPosition,getBlockState());}
    private net.minecraftforge.items.IItemHandler handler(){return new net.minecraftforge.items.IItemHandler(){
        @Override public int getSlots(){return 2;}
        @Override public net.minecraft.world.item.ItemStack getStackInSlot(int slot){var vat=controller();return vat==null?net.minecraft.world.item.ItemStack.EMPTY:vat.getItem(slot);}
        @Override public boolean isItemValid(int slot,net.minecraft.world.item.ItemStack stack){var vat=controller();return vat!=null&&vat.canPlaceItem(slot,stack);}
        @Override public int getSlotLimit(int slot){return 64;}
        @Override public net.minecraft.world.item.ItemStack insertItem(int slot,net.minecraft.world.item.ItemStack stack,boolean simulate){
            var vat=controller();if(stack.isEmpty()||vat==null||!isItemValid(slot,stack))return stack;
            var current=vat.getItem(slot);if(!current.isEmpty()&&!net.minecraft.world.item.ItemStack.isSameItemSameTags(current,stack))return stack;
            int accepted=Math.min(stack.getCount(),Math.min(64,stack.getMaxStackSize())-current.getCount());if(accepted<=0)return stack;
            if(!simulate)vat.setItem(slot,stack.copyWithCount(current.getCount()+accepted));return stack.copyWithCount(stack.getCount()-accepted);
        }
        @Override public net.minecraft.world.item.ItemStack extractItem(int slot,int count,boolean simulate){return net.minecraft.world.item.ItemStack.EMPTY;}
    };}
    @Override public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap,Direction side){return !isRemoved()&&VatStructure.port(getBlockState().getValue(VatBlock.PART))&&cap==net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER?items.cast():super.getCapability(cap,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();items.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();items=net.minecraftforge.common.util.LazyOptional.of(this::handler);}
    public void tick(){
        if(level==null||level.isClientSide||level.getGameTime()%20!=0)return;
        var center=VatStructure.center(worldPosition,getBlockState());
        // A section loaded after its controller was broken restores itself without loading other chunks.
        if(level.hasChunkAt(center)&&!(level.getBlockEntity(center) instanceof VatEntity))level.setBlock(worldPosition,VatStructure.original(getBlockState().getValue(VatBlock.PART)),3);
    }
    @Override public boolean connectable(Direction face){return face!=null&&VatStructure.port(getBlockState().getValue(VatBlock.PART));}
    @Override public boolean canInput(Direction face){return connectable(face);}
    @Override public boolean canOutput(Direction face){return false;}
    @Override public Aspect suctionType(Direction face){var vat=controller();var key=vat==null||vat.progress()>0?null:vat.essentia().wanted();return connectable(face)&&key!=null?TCAspects.catalog().require(key):null;}
    @Override public int suction(Direction face){var vat=controller();return connectable(face)&&vat!=null&&vat.progress()==0&&vat.essentia().wanted()!=null?128:0;}
    @Override public int minimumSuction(){return 0;}
    @Override public Aspect essentiaType(Direction face){return null;}
    @Override public int essentiaAmount(Direction face){return 0;}
    @Override public int takeEssentia(Aspect aspect,int amount,Direction face){return 0;}
    @Override public int addEssentia(Aspect aspect,int amount,Direction face){var vat=controller();return vat!=null&&aspect!=null&&connectable(face)?vat.accept(aspect.tag(),amount):0;}
}
