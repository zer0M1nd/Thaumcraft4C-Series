package tc4revival.content;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.FakePlayerFactory;

public  class ArcaneBoreBlockEntity extends BlockEntity implements Container,MenuProvider,ArcaneBoreWork.Host {
    private static final GameProfile PROFILE=new GameProfile(java.util.UUID.nameUUIDFromBytes("FakeThaumcraftBore".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"FakeThaumcraftBore");
    private final NonNullList<ItemStack> items=NonNullList.withSize(2,ItemStack.EMPTY);
    private final ArcaneBoreWork work=new ArcaneBoreWork(this);
    public final ArcaneBoreVisual visual=new ArcaneBoreVisual();
    private BlockPos visualTarget;
    private int visualSequence,brokenState;
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> automation=net.minecraftforge.common.util.LazyOptional.of(()->new net.minecraftforge.items.wrapper.InvWrapper(this));
    public ArcaneBoreBlockEntity(BlockPos pos,BlockState state) { super(TCArcane.ARCANE_BORE_ENTITY.get(),pos,state); }
    public ArcaneBoreWork work() { return work; }
    @Override public void onLoad() { super.onLoad(); work.orient(true); }
    public static void tick(Level level,BlockPos pos,BlockState state,ArcaneBoreBlockEntity bore) { if (level instanceof ServerLevel) bore.work.tick(); else bore.visual.tick(bore); }
    @Override public ServerLevel level() { return (ServerLevel)level; }
    @Override public BlockPos pos() { return worldPosition; }
    @Override public Direction facing() { return getBlockState().getValue(ArcaneBoreBlock.FACING); }
    @Override public ArcaneBoreWork.Base base() {
        var pos=worldPosition.relative(getBlockState().getValue(ArcaneBoreBlock.MOUNT).getOpposite());
        if (level==null || !level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof ArcaneBoreBaseBlockEntity base)) return null;
        return new ArcaneBoreWork.Base(pos,base.output(),base.transport());
    }
    @Override public ItemStack focus() { return items.get(0); }
    @Override public ItemStack pick() { return items.get(1); }
    @Override public ServerPlayer worker() { return FakePlayerFactory.get(level(),PROFILE); }
    @Override public void changed() { setChanged(); }
    @Override public void targetSelected(BlockPos target) { visualTarget=target.immutable(); visualSequence++; brokenState=0; setChanged(); }
    @Override public void blockBroken(BlockPos target,int stateId) { visualTarget=target.immutable(); brokenState=stateId; level.blockEvent(worldPosition,getBlockState().getBlock(),99,stateId); }
    public BlockPos visualTarget() { return visualTarget; }
    public int visualSequence() { return visualSequence; }
    public int brokenState() { return brokenState; }
    @Override public boolean triggerEvent(int event,int value) {
        if (event==99) {
            brokenState=value;
            if (level!=null && level.isClientSide && visualTarget!=null && value>0) {
                var state=net.minecraft.world.level.block.Block.stateById(value);
                var sound=state.getSoundType(level,visualTarget,null);
                level.playLocalSound(visualTarget,sound.getBreakSound(),net.minecraft.sounds.SoundSource.BLOCKS,(sound.getVolume()+1)/2,sound.getPitch()*.8f,false);
                for (int i=0;i<tc4revival.client.ArcaneBoreDebris.burstCount();i++) tc4revival.client.ArcaneBoreDebris.spawn(level,visualTarget,worldPosition.relative(facing()),state);
            }
            return true;
        }
        return super.triggerEvent(event,value);
    }
    @Override public int getContainerSize() { return 2; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot,int amount) { var result=ContainerHelper.removeItem(items,slot,amount); if (!result.isEmpty()) setChanged(); return result; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items,slot); }
    @Override public void setItem(int slot,ItemStack stack) { items.set(slot,stack); if (stack.getCount()>64) stack.setCount(64); setChanged(); }
    @Override public void clearContent() { items.clear(); setChanged(); }
    @Override public boolean stillValid(Player player) { return level!=null && level.getBlockEntity(worldPosition)==this && player.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64; }
    @Override public boolean canPlaceItem(int slot,ItemStack stack) { return slot==0?stack.getItem() instanceof ExcavationFocusItem:slot==1 && stack.getItem() instanceof net.minecraft.world.item.PickaxeItem; }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); ContainerHelper.saveAllItems(tag,items); work.save(tag);
        tag.putInt("orientation",facing().get3DDataValue()); tag.putInt("baseOrientation",getBlockState().getValue(ArcaneBoreBlock.MOUNT).get3DDataValue());
        if (visualTarget!=null) tag.putLong("target",visualTarget.asLong()); tag.putInt("targetSequence",visualSequence);
    }
    @Override public void load(CompoundTag tag) { super.load(tag); items.clear(); ContainerHelper.loadAllItems(tag,items); work.load(tag); visualTarget=tag.contains("target")?BlockPos.of(tag.getLong("target")):null; visualSequence=tag.getInt("targetSequence"); }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void setChanged() { super.setChanged(); if (level!=null && !level.isClientSide) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2); }
    @Override public Component getDisplayName() { return Component.translatable("block.thaumcraft.arcane_bore"); }
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player) { return new ArcaneBoreMenu(id,inventory,this); }
    @Override public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability,Direction side) {
        if (capability==net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && !isRemoved()) return automation.cast();
        return super.getCapability(capability,side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); automation.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); automation=net.minecraftforge.common.util.LazyOptional.of(()->new net.minecraftforge.items.wrapper.InvWrapper(this)); }
}
