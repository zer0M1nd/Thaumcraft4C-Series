package thaumicfutures.expansion;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.*;
import tc4revival.content.EssentiaJarBlockEntity;

public final class CreativeEssentiaTankEntity extends EssentiaJarBlockEntity {
    public CreativeEssentiaTankEntity(BlockPos pos,BlockState state){super(RestoredContent.CREATIVE_TANK_ENTITY.get(),pos,state);}
    private void saturate(){if(aspect()!=null&&amount()>0&&amount()<64)super.addToContainer(aspect(),64-amount());}
    @Override public int addToContainer(Aspect aspect,int offered){
        int remaining=super.addToContainer(aspect,offered);
        saturate();return remaining;
    }
    @Override public boolean contains(Aspect requested,int quantity){
        return requested!=null&&quantity>=0&&amount()>0&&requested.equals(aspect());
    }
    @Override public int availableAmount(Aspect requested){return contains(requested,1)?Integer.MAX_VALUE:0;}
    @Override public boolean takeFromContainer(Aspect aspect,int amount){return contains(aspect,amount);}
    @Override public void load(CompoundTag tag){
        var saved=tag.copy();
        if(saved.getShort("Amount")>0&&!saved.getString("Aspect").isEmpty())saved.putShort("Amount",(short)64);
        super.load(saved);
    }
    @Override public void readItem(ItemStack stack){super.readItem(stack);saturate();}
    @Override public ItemStack asItem(){
        var stack=new ItemStack(RestoredContent.CREATIVE_TANK_ITEM.get());
        if(amount()>0)AspectNbt.write(stack.getOrCreateTag(),revealedAspects());
        if(filter()!=null)stack.getOrCreateTag().putString("AspectFilter",filter().tag());
        return stack;
    }
}
