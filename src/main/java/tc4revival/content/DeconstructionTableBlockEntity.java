package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraftforge.items.wrapper.*;
import tc4revival.aspect.*;
import tc4revival.aspect.item.ItemStackAspects;
import tc4revival.research.KnowledgeSavedData;

/** Forty-tick destructive research: recursive primal totals determine success, distinct types determine output. */
public  class DeconstructionTableBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    private final NonNullList<ItemStack> items=NonNullList.withSize(1,ItemStack.EMPTY);
    private Aspect pending;
    private int remaining;
    private String customName="";
    private LazyOptional<IItemHandler> sided=LazyOptional.of(()->new SidedInvWrapper(this,Direction.DOWN));
    private LazyOptional<IItemHandler> top=LazyOptional.of(()->new SidedInvWrapper(this,Direction.UP));
    private LazyOptional<IItemHandler> unsided=LazyOptional.of(()->new InvWrapper(this));
    public DeconstructionTableBlockEntity(BlockPos pos,BlockState state) { super(TCArcane.DECONSTRUCTION_TABLE_ENTITY.get(),pos,state); }
    public static boolean accepts(ItemStack stack) { return !stack.isEmpty() && ItemStackAspects.resolve(stack).filter(a->!a.isEmpty()).isPresent(); }
    public Aspect pending() { return pending; }
    public int remaining() { return remaining; }
    public static void tick(Level level,BlockPos pos,BlockState state,DeconstructionTableBlockEntity table) {
        if (level.isClientSide) return;
        if (table.pending!=null || !accepts(table.getItem(0))) { table.remaining=0; return; }
        boolean started=table.remaining==0;
        if (started) table.remaining=40;
        if (--table.remaining==0) {
            var amounts=ItemStackAspects.resolve(table.getItem(0)).orElseThrow();
            var primals=TCAspects.catalog().toPrimals(amounts,AspectCatalog.Reduction.SUM);
            if (level.random.nextInt(80)<primals.total() && !primals.isEmpty())
                table.pending=primals.aspects().get(level.random.nextInt(primals.size()));
            // TC4 destroys the whole item, including any crafting container remainder.
            table.items.get(0).shrink(1);
            table.sync();
        } else if (started) table.sync();
    }
    public boolean claim(ServerPlayer player) {
        if (pending==null || !stillValid(player) || !(player.containerMenu instanceof DeconstructionTableMenu menu) || menu.table()!=this) return false;
        var reward=pending;
        KnowledgeSavedData.get(player.server).update(player.getUUID(),player.getRandom()::nextInt,knowledge->{ knowledge.changePool(reward,(short)1); return true; });
        pending=null;
        // Persist the removal as well as the award so a later chunk reload cannot duplicate it.
        sync();
        return true;
    }
    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return items.get(0).isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot==0 ? items.get(0) : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot,int amount) { var result=ContainerHelper.removeItem(items,slot,amount); if (!result.isEmpty()) sync(); return result; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items,slot); }
    @Override public void setItem(int slot,ItemStack stack) { if (slot==0) { items.set(0,stack.copyWithCount(Math.min(64,stack.getCount()))); sync(); } }
    @Override public void clearContent() { items.clear(); sync(); }
    @Override public boolean stillValid(Player player) { return level!=null && level.getBlockEntity(worldPosition)==this && player.distanceToSqr(worldPosition.getCenter())<=64; }
    @Override public boolean canPlaceItem(int slot,ItemStack stack) {
        if(slot!=0||stack.isEmpty()||level==null||isRemoved())return false;
        if(level.isClientSide)return tc4revival.client.ClientItemAspects.resolve(stack).filter(result->!result.amounts().isEmpty()).isPresent();
        return accepts(stack);
    }
    @Override public int[] getSlotsForFace(Direction face) { return face==Direction.UP ? new int[0] : new int[]{0}; }
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction face) { return face!=Direction.UP && canPlaceItem(slot,stack); }
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction face) { return slot==0 && face!=Direction.UP; }
    public void setCustomName(String name) { customName=name; sync(); }
    @Override public Component getDisplayName() { return customName.isEmpty() ? Component.translatable("block.thaumcraft.deconstruction_table") : Component.literal(customName); }
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player) { return new DeconstructionTableMenu(id,inventory,this); }
    public void sync() { setChanged(); if (level!=null && !level.isClientSide) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2); }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); ContainerHelper.saveAllItems(tag,items); if (pending!=null) tag.putString("Aspect",pending.tag()); if (!customName.isEmpty()) tag.putString("CustomName",customName); }
    @Override public void load(CompoundTag tag) { super.load(tag); items.clear(); ContainerHelper.loadAllItems(tag,items); pending=TCAspects.catalog().find(tag.getString("Aspect")).orElse(null); customName=tag.getString("CustomName"); remaining=0; }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability,Direction side) {
        if (capability==ForgeCapabilities.ITEM_HANDLER && !isRemoved()) return (side==null ? unsided : side==Direction.UP ? top : sided).cast();
        return super.getCapability(capability,side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); sided.invalidate(); top.invalidate(); unsided.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); sided=LazyOptional.of(()->new SidedInvWrapper(this,Direction.DOWN)); top=LazyOptional.of(()->new SidedInvWrapper(this,Direction.UP)); unsided=LazyOptional.of(()->new InvWrapper(this)); }
}
