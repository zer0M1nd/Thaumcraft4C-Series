package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public  class HungryChestBlockEntity extends BaseContainerBlockEntity {
    private final NonNullList<ItemStack> items=NonNullList.withSize(27,ItemStack.EMPTY);
    private int viewers;
    public float lid,previousLid;
    public HungryChestBlockEntity(BlockPos pos,BlockState state) { super(TCArcane.HUNGRY_CHEST_ENTITY.get(),pos,state); }
    @Override protected Component getDefaultName() { return Component.translatable("container.thaumcraft.hungry_chest"); }
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inventory) { return ChestMenu.threeRows(id,inventory,this); }
    @Override public int getContainerSize() { return 27; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot,int count) { var result=ContainerHelper.removeItem(items,slot,count); if (!result.isEmpty()) setChanged(); return result; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items,slot); }
    @Override public void setItem(int slot,ItemStack stack) {
        var previous=items.get(slot);
        boolean added=!stack.isEmpty()&&(!ItemStack.isSameItemSameTags(previous,stack)||stack.getCount()>previous.getCount());
        items.set(slot,stack); if (stack.getCount()>64) stack.setCount(64); setChanged();
        if(added&&viewers==0)eat();
    }
    public void eat() {
        if(level!=null&&!level.isClientSide)level.blockEvent(worldPosition,getBlockState().getBlock(),2,2);
    }
    @Override public void clearContent() { items.clear(); setChanged(); }
    @Override public boolean stillValid(Player player) { return level!=null && level.getBlockEntity(worldPosition)==this && player.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64; }
    @Override public void load(CompoundTag tag) { super.load(tag); items.clear(); ContainerHelper.loadAllItems(tag,items); }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); ContainerHelper.saveAllItems(tag,items); }
    @Override public void startOpen(Player player) { viewers++; publishViewers(); }
    @Override public void stopOpen(Player player) { viewers--; publishViewers(); }
    private void publishViewers() { if (level!=null) level.blockEvent(worldPosition,getBlockState().getBlock(),1,viewers); }
    @Override public boolean triggerEvent(int id,int value) {
        if (id==1) { viewers=value; return true; }
        if (id==2) {
            lid=Math.max(lid,value/10f);
            if(level!=null&&!level.isClientSide)level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.GENERIC_EAT,
                    net.minecraft.sounds.SoundSource.BLOCKS,.25f,1+(level.random.nextFloat()-level.random.nextFloat())*.2f);
            return true;
        }
        return super.triggerEvent(id,value);
    }
    public static void tick(Level level,BlockPos pos,BlockState state,HungryChestBlockEntity chest) {
        chest.previousLid=chest.lid;
        boolean open=chest.viewers>0;
        if (open && chest.lid==0) chest.sound(net.minecraft.sounds.SoundEvents.CHEST_OPEN);
        if (!open && chest.lid>0 || open && chest.lid<1) {
            float old=chest.lid; chest.lid=Math.max(0,Math.min(1,chest.lid+(open?.1f:-.1f)));
            if (chest.lid<.5f && old>=.5f) chest.sound(net.minecraft.sounds.SoundEvents.CHEST_CLOSE);
        }
    }
    private void sound(net.minecraft.sounds.SoundEvent sound) {
        if (level!=null&&!level.isClientSide) level.playSound(null,worldPosition,sound,net.minecraft.sounds.SoundSource.BLOCKS,.5f,level.random.nextFloat()*.1f+.9f);
    }
}
