package tc4revival.content;

import java.util.ArrayList;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

/** Paired item transport; the output queue is private and cannot be extracted by automation. */
public  class ItemMirrorBlockEntity extends BlockEntity implements thaumcraft.api.devices.DeviceExtensions.Host {
    private final thaumcraft.api.devices.DeviceExtensions.Store extensions=new thaumcraft.api.devices.DeviceExtensions.Store(this,"Items");
    @Override public thaumcraft.api.devices.DeviceExtensions.Store extensions(){return extensions;}
    private java.util.UUID identity=java.util.UUID.randomUUID(),targetIdentity;
    private thaumcraft.api.transport.MirrorEndpoints.MobileHost mobile;
    private GlobalPos destination=GlobalPos.of(Level.OVERWORLD,BlockPos.ZERO);
    private boolean linked;
    private int instability,interval=40;
    private long count;
    private final ArrayList<ItemStack> output=new ArrayList<>();
    private final IItemHandler handler=new MirrorHandlerDelegate(this);
    private LazyOptional<IItemHandler> items=LazyOptional.of(()->handler);
    public ItemMirrorBlockEntity(BlockPos pos,BlockState state) { super(TCArcane.ITEM_MIRROR_ENTITY.get(),pos,state); }
    public Direction facing() { return getBlockState().getValue(ItemMirrorBlock.FACING); }
    public GlobalPos destination() { return destination; }
    public boolean linked() { return linked; }
    public int instability() { return instability; }
    public java.util.List<ItemStack> queuedItems() { return output.stream().map(ItemStack::copy).toList(); }
    public GlobalPos location() { return GlobalPos.of(level.dimension(),mobile==null?worldPosition:BlockPos.containing(mobile.position())); }
    public java.util.UUID identity(){return identity;}
    public java.util.UUID targetIdentity(){return targetIdentity;}
    public boolean endpointLive(){return mobile==null?thaumcraft.api.LiveBlockEntity.server(this):mobile.valid();}
    public void prepareMovement(){
        var other=target();
        if(other!=null&&linked&&other.linked){targetIdentity=other.identity;other.targetIdentity=identity;other.changed();other.registerEndpoint();}
        registerEndpoint();
    }
    private void registerEndpoint(){if(level instanceof ServerLevel server)thaumcraft.api.transport.MirrorEndpoints.get(server.getServer()).register(this,location());}
    public void mount(ServerLevel level,thaumcraft.api.transport.MirrorEndpoints.MobileHost host){setLevel(level);mobile=host;registerEndpoint();}
    @Override public void onLoad(){super.onLoad();registerEndpoint();}
    @Override public void setRemoved(){if(level instanceof ServerLevel server)thaumcraft.api.transport.MirrorEndpoints.get(server.getServer()).unregister(this);super.setRemoved();}
    @Override public void onChunkUnloaded(){extensions.unload();if(level instanceof ServerLevel server)thaumcraft.api.transport.MirrorEndpoints.get(server.getServer()).unregister(this);super.onChunkUnloaded();}
    @Override public void setChanged(){if(mobile!=null){mobile.changed(this);return;}super.setChanged();}
    ItemMirrorBlockEntity target() {
        if (!(level instanceof ServerLevel server) || destination==null) return null;
        if(targetIdentity!=null)return thaumcraft.api.transport.MirrorEndpoints.get(server.getServer()).resolve(server.getServer(),targetIdentity);
        var world=server.getServer().getLevel(destination.dimension());
        if (world==null) return null;
        // Like the old world lookup, resolve the destination chunk on demand, without a permanent ticket.
        return world.getBlockEntity(destination.pos()) instanceof ItemMirrorBlockEntity mirror ? mirror : null;
    }
    public void remember(GlobalPos target) { destination=target;targetIdentity=null;linked=false; changed(); }
    public void remember(GlobalPos target,java.util.UUID id){destination=target;targetIdentity=id;linked=false;changed();}
    public boolean valid(boolean invalidate) {
        if (!linked) return false;
        var other=target();
        boolean valid=other!=null && other.linked && (other.targetIdentity!=null?identity.equals(other.targetIdentity):location().equals(other.destination));
        if (!valid && invalidate && (other!=null||targetIdentity==null)) { linked=false; changed(); }
        return valid;
    }
    public boolean restoreLink() {
        var other=target();
        if (other==null) { if (linked&&targetIdentity==null) { linked=false; changed(); } return false; }
        if (other.valid(true)) return false;
        other.destination=location(); if(targetIdentity!=null||mobile!=null){targetIdentity=other.identity;other.targetIdentity=identity;}other.linked=true; other.changed(); linked=true; changed(); return true;
    }
    public void invalidateLink() {
        if (!(level instanceof ServerLevel server) || destination==null) return;
        if(targetIdentity!=null){var other=target();if(other!=null){other.linked=false;other.changed();}return;}
        var world=server.getServer().getLevel(destination.dimension());
        if (world!=null && world.hasChunkAt(destination.pos()) && world.getBlockEntity(destination.pos()) instanceof ItemMirrorBlockEntity other) {
            other.linked=false; other.changed();
        }
    }
    public boolean transport(ItemEntity item) {
        if (level==null || level.isClientSide || !item.isAlive() || item.getItem().isEmpty() || !valid(true)) return false;
        var other=target(); if (other==null) return false;
        other.enqueue(item.getItem()); addInstability(item.getItem().getCount()); item.discard(); transferEvent(); return true;
    }
    /** Enqueues at this mirror, not its partner. Caller retains ownership of its input stack.
     * The legacy queue is unbounded; an unavailable target rejects the entire stack.
     */
    public ItemStack insertOutput(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (!endpointLive()) return stack.copy();
        if (!simulate) enqueue(stack);
        return ItemStack.EMPTY;
    }
    private void enqueue(ItemStack stack) { if (!stack.isEmpty()) { output.add(stack.copy()); setChanged(); } }
    public void dropQueuedItems() {
        if(level==null || level.isClientSide)return;
        for(var stack:output)net.minecraft.world.Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,stack);
        output.clear();setChanged();
    }
    private void addInstability(int amount) {
        instability=(int)Math.max(0,Math.min(Integer.MAX_VALUE,(long)instability+amount)); changed();
    }
    public boolean spawnItem(ItemStack stack) {
        if (!(level instanceof ServerLevel world) || stack.isEmpty()) return false;
        if(mobile!=null){
            if(!mobile.valid())return false;
            var direction=mobile.outward();var at=mobile.position().subtract(direction.scale(.3));
            var item=new ItemEntity(world,at.x,at.y,at.z,stack.copy());item.setDeltaMovement(direction.scale(.15));item.setPortalCooldown(20);return world.addFreshEntity(item);
        }
        var face=facing();
        var item=new ItemEntity(world,worldPosition.getX()+.5-face.getStepX()*.3,
                worldPosition.getY()+.5-face.getStepY()*.3,worldPosition.getZ()+.5-face.getStepZ()*.3,stack.copy());
        item.setDeltaMovement(face.getStepX()*.15f,face.getStepY()*.15f,face.getStepZ()*.15f);
        item.setPortalCooldown(20);
        return world.addFreshEntity(item);
    }
    public void transferEvent() { if(mobile!=null){mobile.transfer();return;}if (level!=null && !level.isClientSide) level.blockEvent(worldPosition,getBlockState().getBlock(),1,0); }
    /** Offers at most one legal stack to an output integration. The return value is the
     * committed amount, not a simulation. Ordinary world ejection retains its one-item cadence. */
    public int transferOutput(ItemStack offered) {
        if(mobile==null){var routed=thaumcraft.api.transport.MirrorOutputRoutes.transfer(this,offered);if(routed.isPresent())return routed.getAsInt();}
        if(mobile!=null){int accepted=mobile.transferOutput(offered.copy());if(accepted>=0)return accepted;}
        return !offered.isEmpty() && spawnItem(offered.copyWithCount(1)) ? 1 : 0;
    }
    public void tick() {
        if (level==null || level.isClientSide) return;
        if(mobile!=null){if(!mobile.valid())return;if(count%20==0)registerEndpoint();}
        long delay=instability/50;
        if ((delay==0 || count%(delay*delay)==0) && count>20 && !output.isEmpty()) {
            int index=level.random.nextInt(output.size()); var stack=output.get(index);
            int offered=Math.min(stack.getCount(),stack.getMaxStackSize());
            int accepted=transferOutput(stack.copyWithCount(offered));
            if(accepted<0||accepted>offered)throw new IllegalStateException("Invalid mirror output amount: "+accepted);
            if (accepted>0) {
                stack.shrink(accepted); if (stack.isEmpty()) output.remove(index);
                addInstability(accepted); transferEvent(); setChanged();
            }
        }
        if (instability>0 && count%20==0) addInstability(-1);
        if (instability>0) {
            int drained=VisNetwork.drain(level,location().pos(),tc4revival.aspect.TCAspects.catalog().require("ordo"),1);
            if (drained>0) { addInstability(-drained); var other=target(); if (other!=null) other.addInstability(-drained); }
        }
        if (count++%interval==0) {
            if (valid(false)) interval=40;
            else { if (interval<600) interval+=20; restoreLink(); }
        }
    }
    private void changed() { setChanged(); if (mobile==null&&level!=null && !level.isClientSide) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2); }
    private void saveLink(CompoundTag tag) {
        tag.putBoolean("linked",linked); tag.putInt("instability",instability);
        if (destination!=null) ItemMirrorItem.writeDestination(tag,destination,"");
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); extensions.save(tag); saveLink(tag);
        tag.putUUID("MirrorIdentity",identity);if(targetIdentity!=null)tag.putUUID("MirrorTargetIdentity",targetIdentity);
        var list=new ListTag(); for (var stack:output) if (!stack.isEmpty()) list.add(stack.save(new CompoundTag())); tag.put("Items",list);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); destination=ItemMirrorItem.readDestination(tag); linked=destination!=null && tag.getBoolean("linked");
        if(tag.hasUUID("MirrorIdentity"))identity=tag.getUUID("MirrorIdentity");targetIdentity=tag.hasUUID("MirrorTargetIdentity")?tag.getUUID("MirrorTargetIdentity"):null;
        instability=Math.max(0,tag.getInt("instability")); output.clear();
        var list=tag.getList("Items",Tag.TAG_COMPOUND); for (int i=0;i<list.size();i++) { var stack=ItemStack.of(list.getCompound(i)); if (!stack.isEmpty()) output.add(stack); }
     extensions.load(tag); }
    @Override public CompoundTag getUpdateTag() { var tag=new CompoundTag(); saveLink(tag); return tag; }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public boolean triggerEvent(int id,int value) {
        if (id==1) { if (level!=null && level.isClientSide) tc4revival.client.ClientFocusEffects.mirrorTransfer(level,worldPosition,facing()); return true; }
        return super.triggerEvent(id,value);
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side) { return cap==ForgeCapabilities.ITEM_HANDLER && !isRemoved() ? items.cast() : super.getCapability(cap,side); }
    @Override public void invalidateCaps() { super.invalidateCaps(); extensions.invalidate(); items.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); extensions.revive(); items=LazyOptional.of(()->handler); }

    // reviveCaps needs a fresh optional but the same live handler, not a second queue.
    private static  class MirrorHandlerDelegate implements IItemHandler {
        private final ItemMirrorBlockEntity owner;
        MirrorHandlerDelegate(ItemMirrorBlockEntity owner) { this.owner=owner; }
        @Override public int getSlots() { return 1; }
        @Override public ItemStack getStackInSlot(int slot) { return ItemStack.EMPTY; }
        @Override public ItemStack extractItem(int slot,int amount,boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot,ItemStack stack) { return slot==0 && owner.target()!=null; }
        @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate) {
            if (slot!=0 || stack.isEmpty()) return stack;
            var target=owner.target(); if (target==null) return stack;
            int amount=Math.min(64,stack.getCount());
            if (!simulate) { target.enqueue(stack.copyWithCount(amount)); owner.addInstability(amount); owner.transferEvent(); }
            return stack.copyWithCount(stack.getCount()-amount);
        }
    }
}
