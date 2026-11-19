package automagy.content;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import tc4revival.content.ItemMirrorBlockEntity;

public final class DeliveryMirrorBlockEntity extends BlockEntity implements InventariumMemoryUser {
    private ItemStack eye=ItemStack.EMPTY;
    private BlockPos core;
    private final List<ItemStack> output=new ArrayList<>();
    private final IItemHandler handler=new IItemHandler(){
        public int getSlots(){return 1;} public ItemStack getStackInSlot(int slot){return ItemStack.EMPTY;}
        public int getSlotLimit(int slot){return 64;} public boolean isItemValid(int slot,ItemStack stack){return slot==0;}
        public ItemStack extractItem(int slot,int count,boolean simulate){return ItemStack.EMPTY;}
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return slot==0?insert(stack,simulate):stack.copy();}
    };
    private LazyOptional<IItemHandler> capability=LazyOptional.of(()->handler);
    public DeliveryMirrorBlockEntity(BlockPos pos,BlockState state){super(Content.DELIVERY_MIRROR_TILE.get(),pos,state);}
    public boolean multi(){return getBlockState().getBlock() instanceof DeliveryMirrorBlock block&&block.multi;}
    private boolean live(){return level instanceof ServerLevel&&!isRemoved()&&level.hasChunkAt(worldPosition)&&level.getBlockEntity(worldPosition)==this&&level.getServer().isSameThread();}
    @Override public BlockPos linkedCore(){return core;}
    @Override public void linkCore(BlockPos pos){core=pos==null?null:pos.immutable();setChanged();}
    public InventariumBlockEntity core(){return multi()&&level!=null&&core!=null&&level.hasChunkAt(core)&&level.getBlockEntity(core) instanceof InventariumBlockEntity inventory&&inventory.hasMemoryUser(worldPosition)&&inventory.capacity().loaded()?inventory:null;}
    public ItemMirrorBlockEntity target(){
        if(!(level instanceof ServerLevel server)||!(eye.getItem() instanceof CrystalEyeItem item))return null;
        var target=item.linkedPosition(eye).map(pos->MirrorDelivery.resolve(server,pos)).orElse(null);
        return target instanceof ItemMirrorBlockEntity mirror&&mirror.linked()?mirror:null;
    }
    public void installEye(Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);if(!live()||multi()||!eye.isEmpty()||!(stack.getItem() instanceof CrystalEyeItem item))return;
        var endpoint=item.linkedPosition(stack).map(pos->MirrorDelivery.resolve((ServerLevel)level,pos)).orElse(null);
        if(!(endpoint instanceof ItemMirrorBlockEntity mirror)||!mirror.linked())return;
        eye=stack.copyWithCount(1);stack.shrink(1);setChanged();level.setBlock(worldPosition,getBlockState().setValue(DeliveryMirrorBlock.SOCKETED,true),3);
    }
    public void removeEye(Player player){if(!live()||eye.isEmpty())return;player.getInventory().placeItemBackInInventory(eye);eye=ItemStack.EMPTY;setChanged();level.setBlock(worldPosition,getBlockState().setValue(DeliveryMirrorBlock.SOCKETED,false),3);}
    public ItemStack insertOutput(ItemStack stack,boolean simulate){if(!live()||multi())return stack.copy();if(!simulate&&!stack.isEmpty()){output.add(stack.copy());setChanged();}return ItemStack.EMPTY;}
    public ItemStack insert(ItemStack stack,boolean simulate){
        if(stack.isEmpty())return ItemStack.EMPTY;if(!live())return stack.copy();
        if(!multi())return MirrorDelivery.enqueue(target(),stack,simulate);
        var inventory=core();if(inventory==null)return stack.copy();
        var remainder=stack.copy();var key=new RequestedItem(stack);
        for(var request:inventory.requests().requests()){
            if(remainder.isEmpty())break;if(!request.item().equals(key))continue;
            long reserved=inventory.requests().claims().stream().filter(c->c.item().equals(key)&&c.destination().equals(request.destination())).mapToLong(c->c.amount()).sum();
            int count=(int)Math.min(remainder.getCount(),Math.max(0,request.amount()-reserved));if(count==0)continue;
            var offered=remainder.copyWithCount(count);
            int accepted=count-MirrorDelivery.requested((ServerLevel)level,inventory,request.destination(),offered,true).getCount();if(accepted==0)continue;
            if(simulate){remainder.shrink(accepted);continue;}
            var claim=inventory.requests().claim(key,request.destination(),accepted);if(claim.isEmpty())continue;
            offered.setCount(claim.get().amount());int moved=offered.getCount()-MirrorDelivery.requested((ServerLevel)level,inventory,request.destination(),offered,false).getCount();
            inventory.requests().finish(claim.get().id(),moved);remainder.shrink(moved);
        }
        return remainder;
    }
    /** Courier already owns this reservation; do not reserve the same demand a second time. */
    public int deliverReserved(InventariumBlockEntity inventory,long id,ItemStack stack){
        if(!live()||!multi()||core()!=inventory)return 0;
        var claim=inventory.requests().claims().stream().filter(c->c.id()==id&&c.item().equals(new RequestedItem(stack))).findFirst();if(claim.isEmpty())return 0;
        int count=Math.min(stack.getCount(),inventory.requests().deliveryLimit(id));
        return count-MirrorDelivery.requested((ServerLevel)level,inventory,claim.get().destination(),stack.copyWithCount(count),false).getCount();
    }
    public static void tick(Level level,BlockPos pos,BlockState state,DeliveryMirrorBlockEntity mirror){
        if(mirror.multi()&&level.getGameTime()%20==0){boolean active=mirror.core()!=null;if(state.getValue(DeliveryMirrorBlock.SOCKETED)!=active)level.setBlock(pos,state.setValue(DeliveryMirrorBlock.SOCKETED,active),3);}
        if(mirror.output.isEmpty())return;
        int index=level.random.nextInt(mirror.output.size());var stack=mirror.output.get(index);var face=state.getValue(DeliveryMirrorBlock.FACING);
        var item=new ItemEntity(level,pos.getX()+.5-face.getStepX()*.3,pos.getY()+.5-face.getStepY()*.3,pos.getZ()+.5-face.getStepZ()*.3,stack.copyWithCount(1));
        item.setDeltaMovement(face.getStepX()*.15,face.getStepY()*.15,face.getStepZ()*.15);item.setPortalCooldown(20);
        if(level.addFreshEntity(item)){stack.shrink(1);if(stack.isEmpty())mirror.output.remove(index);mirror.setChanged();}
    }
    public void removed(){if(!live())return;if(core!=null&&level.hasChunkAt(core)&&level.getBlockEntity(core) instanceof InventariumBlockEntity inventory)inventory.unlinkMemory(worldPosition);Block.popResource(level,worldPosition,eye);for(var stack:output)Block.popResource(level,worldPosition,stack);eye=ItemStack.EMPTY;output.clear();}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.put("Eye",eye.save(new CompoundTag()));if(core!=null)tag.put("Core",NbtUtils.writeBlockPos(core));var list=new ListTag();for(var stack:output)list.add(stack.save(new CompoundTag()));tag.put("Output",list);}
    @Override public void load(CompoundTag tag){super.load(tag);eye=ItemStack.of(tag.getCompound("Eye"));core=tag.contains("Core",Tag.TAG_COMPOUND)?NbtUtils.readBlockPos(tag.getCompound("Core")):null;output.clear();for(var entry:tag.getList("Output",Tag.TAG_COMPOUND)){var stack=ItemStack.of((CompoundTag)entry);if(!stack.isEmpty())output.add(stack);}}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){return cap==ForgeCapabilities.ITEM_HANDLER&&!isRemoved()?capability.cast():super.getCapability(cap,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();capability.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();capability=LazyOptional.of(()->handler);}
}
