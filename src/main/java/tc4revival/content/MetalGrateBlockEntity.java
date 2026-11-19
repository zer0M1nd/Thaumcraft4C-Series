package tc4revival.content;
import net.minecraft.core.*;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
public  class MetalGrateBlockEntity extends BlockEntity implements WorldlyContainer {
    private LazyOptional<IItemHandler> top=LazyOptional.of(()->new SidedInvWrapper(this,Direction.UP));
    private LazyOptional<IItemHandler> other=LazyOptional.of(()->new SidedInvWrapper(this,Direction.DOWN));
    public MetalGrateBlockEntity(BlockPos pos,BlockState state) { super(TCArcane.METAL_GRATE_ENTITY.get(),pos,state); }
    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return true; }
    @Override public ItemStack getItem(int slot) { return ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot,int count) { return ItemStack.EMPTY; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ItemStack.EMPTY; }
    @Override public void setItem(int slot,ItemStack stack) {
        if(level==null||level.isClientSide||stack.isEmpty())return;
        var item=new GrateItemEntity(TCEntities.GRATE_ITEM.get(),level);
        item.setPos(worldPosition.getX()+.5,worldPosition.getY()+.6,worldPosition.getZ()+.5);item.setItem(stack.copy());
        item.setDeltaMovement(0,-.1,0);level.addFreshEntity(item);
    }
    @Override public boolean stillValid(Player player) { return false; }
    @Override public void clearContent() { }
    @Override public boolean canPlaceItem(int slot,ItemStack stack) { return slot==0&&!getBlockState().getValue(MetalGrateBlock.CLOSED); }
    @Override public int[] getSlotsForFace(Direction face) { return face==Direction.UP&&!getBlockState().getValue(MetalGrateBlock.CLOSED)?new int[]{0}:new int[0]; }
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction face) { return face==Direction.UP&&canPlaceItem(slot,stack); }
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction face) { return false; }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side) {
        if(!isRemoved()&&cap==ForgeCapabilities.ITEM_HANDLER)return (side==null||side==Direction.UP?top:other).cast();return super.getCapability(cap,side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps();top.invalidate();other.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps();top=LazyOptional.of(()->new SidedInvWrapper(this,Direction.UP));other=LazyOptional.of(()->new SidedInvWrapper(this,Direction.DOWN)); }
}
