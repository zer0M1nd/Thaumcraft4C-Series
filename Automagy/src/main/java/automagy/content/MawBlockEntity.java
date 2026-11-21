package automagy.content;

import net.minecraft.core.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;

public final class MawBlockEntity extends InventoryBlockEntity {
    private long nextSound;
    public MawBlockEntity(BlockPos pos,BlockState state){super(Content.MAW_TILE.get(),pos,state,1);}
    @Override protected int slotLimit(int slot){return 1;}
    @Override protected boolean accepts(int slot,ItemStack stack){return ((MawBlock)getBlockState().getBlock()).finical&&stack.getItem() instanceof FilterItem&&ItemFilter.load(stack).populated();}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){return cap==ForgeCapabilities.ITEM_HANDLER?LazyOptional.empty():super.getCapability(cap,side);}
    public ItemStack insertFilter(ItemStack stack){return inventory.insertItem(0,stack,false);}
    public ItemStack removeFilter(){return inventory.extractItem(0,1,false);}
    public void swallow(ItemEntity entity){
        if(level==null||level.isClientSide||!entity.isAlive())return;
        Direction face=getBlockState().getValue(MawBlock.FACING);
        var target=InventoryTransfer.at(level,worldPosition.relative(face.getOpposite()),face);
        if(target.isEmpty())return;
        ItemStack original=entity.getItem();long limit=original.getCount();
        if(stored(0).getItem() instanceof FilterItem item){
            ItemFilter filter=ItemFilter.load(stored(0));long present=InventoryTransfer.count(InventoryTransfer.snapshot(target.get(),false),original,filter::matches);
            limit=Math.min(limit,filter.transferLimit(original,item.blacklist,original.getCount(),present).maximum());
        }
        if(limit<=0)return;
        ItemStack remainder=InventoryTransfer.insert(target.get(),original.copyWithCount((int)limit),false);
        int moved=(int)limit-remainder.getCount();if(moved<=0)return;
        if(moved>=original.getCount())entity.discard();else entity.setItem(original.copyWithCount(original.getCount()-moved));
        if(level.getGameTime()>=nextSound){nextSound=level.getGameTime()+5;level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.GENERIC_EAT,net.minecraft.sounds.SoundSource.BLOCKS,.3f,1.5f);}
    }
}
