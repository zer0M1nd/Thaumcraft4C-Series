package automagy.content;

import automagy.logic.ScribePower;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.visnet.VisNetHandler;

public final class RequisitionTomeBlockEntity extends BlockEntity implements InventariumMemoryUser {
    private BlockPos core;
    private final boolean[] ghostEyes={false,false};
    private final ItemStack[] mirrorEyes={ItemStack.EMPTY,ItemStack.EMPTY};
    private java.util.UUID requestBook;
    private final ScribePower power=new ScribePower();
    public final automagy.logic.RequisitionAnimation animation=new automagy.logic.RequisitionAnimation();
    public RequisitionTomeBlockEntity(BlockPos pos,BlockState state){super(Content.REQUISITION_TOME_TILE.get(),pos,state);}
    @Override public BlockPos linkedCore(){return core;}
    @Override public void linkCore(BlockPos pos){core=pos==null?null:pos.immutable();setChanged();}
    public net.minecraft.world.level.block.entity.BlockEntity mirror(int slot){
        if(!(level instanceof net.minecraft.server.level.ServerLevel server)||!(mirrorEyes[slot].getItem() instanceof CrystalEyeItem eye))return null;
        var target=eye.linkedPosition(mirrorEyes[slot]).orElse(null);
        if(target==null||!target.dimension().equals(level.dimension())||target.pos().distSqr(worldPosition)>25)return null;
        var tile=MirrorDelivery.resolve(server,target);return MirrorDelivery.output(tile)?tile:null;
    }
    public ItemStack mirrorEye(int slot){return mirrorEyes[slot].copy();}
    public void configureEye(int slot,ItemStack sample,net.minecraft.world.entity.player.Player player){
        if(level==null||level.isClientSide||slot<0||slot>1||!player.mayBuild()||!(sample.isEmpty()||sample.getItem() instanceof CrystalEyeItem))return;
        if(!ghostEyes[slot]&&!mirrorEyes[slot].isEmpty())player.getInventory().placeItemBackInInventory(mirrorEyes[slot].copy());
        mirrorEyes[slot]=sample.copyWithCount(1);ghostEyes[slot]=true;setChanged();
    }
    public int mirrorStatus(int slot){
        var eye=mirrorEyes[slot];if(eye.isEmpty())return 0;
        var target=eye.getItem() instanceof CrystalEyeItem item?item.linkedPosition(eye).orElse(null):null;
        if(target==null||level==null||!target.dimension().equals(level.dimension())||target.pos().distSqr(worldPosition)>25)return 2;
        // Status must distinguish an existing unlinked mirror from a missing mirror.
        // The delivery resolver intentionally filters unusable endpoints and cannot do that.
        if(!(level instanceof net.minecraft.server.level.ServerLevel server))return 3;
        var mirror=MirrorDelivery.resolve(server,target);
        if(mirror instanceof tc4revival.content.ItemMirrorBlockEntity nativeMirror)return nativeMirror.linked()?1:4;
        if(mirror instanceof DeliveryMirrorBlockEntity delivery&&!delivery.multi())return delivery.target()!=null?1:4;
        return 3;
    }
    public net.minecraft.world.level.block.entity.BlockEntity outputMirror(){return mirror(1);}
    public boolean installMirror(net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand){
        if(!(level instanceof net.minecraft.server.level.ServerLevel server)||!player.mayBuild())return false;
        var stack=player.getItemInHand(hand);if(!(stack.getItem() instanceof CrystalEyeItem eye))return false;
        var pos=eye.linkedPosition(stack).orElse(null);int slot=player.isShiftKeyDown()?1:0;
        if(!mirrorEyes[slot].isEmpty()||pos==null||!pos.dimension().equals(level.dimension())||pos.pos().distSqr(worldPosition)>25||!MirrorDelivery.output(MirrorDelivery.resolve(server,pos)))return false;
        mirrorEyes[slot]=stack.copyWithCount(1);ghostEyes[slot]=false;stack.shrink(1);setChanged();
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(slot==0?"message.automagy.mirror.input":"message.automagy.mirror.output"),true);return true;
    }
    public void removeMirrors(net.minecraft.world.entity.player.Player player){for(int i=0;i<2;i++){if(!ghostEyes[i])player.getInventory().placeItemBackInInventory(mirrorEyes[i]);mirrorEyes[i]=ItemStack.EMPTY;}setChanged();}
    public ItemStack deposit(ItemStack stack){
        if(!(level instanceof net.minecraft.server.level.ServerLevel server)||!powered()||core()==null)return stack.copy();
        var source=mirror(0);
        if(source instanceof tc4revival.content.ItemMirrorBlockEntity base){
            // Check both endpoints are loaded before invoking the native forwarding handler.
            var other=MirrorDelivery.resolve(server,base.destination());
            if(!(other instanceof tc4revival.content.ItemMirrorBlockEntity target)||!target.linked()||!GlobalPos.of(level.dimension(),base.getBlockPos()).equals(target.destination()))return stack.copy();
            return base.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).map(handler->handler.insertItem(0,stack,false)).orElseGet(stack::copy);
        }
        return source instanceof DeliveryMirrorBlockEntity delivery?delivery.insert(stack,false):stack.copy();
    }
    public boolean powered(){return power.powered();}
    public InventariumBlockEntity core(){
        return level!=null&&core!=null&&level.hasChunkAt(core)&&level.getBlockEntity(core) instanceof InventariumBlockEntity inventory&&inventory.hasMemoryUser(worldPosition)&&inventory.capacity().loaded()?inventory:null;
    }
    public boolean request(ItemStack item,int count){
        var inventory=core();if(level==null||level.isClientSide||inventory==null||!powered()||item.isEmpty()||count<=0||count>64)return false;
        CourierCleanupData.get((net.minecraft.server.level.ServerLevel)level).apply(inventory);
        requestBook=inventory.requests().identity();setChanged();
        inventory.requests().request(item,GlobalPos.of(level.dimension(),worldPosition),count);return true;
    }
    public void removed(){
        if(!(level instanceof net.minecraft.server.level.ServerLevel server))return;
        for(int i=0;i<2;i++){if(!ghostEyes[i])net.minecraft.world.level.block.Block.popResource(level,worldPosition,mirrorEyes[i]);mirrorEyes[i]=ItemStack.EMPTY;}
        if(core==null)return;
        if(level.hasChunkAt(core)&&level.getBlockEntity(core) instanceof InventariumBlockEntity inventory){
            if(requestBook==null||requestBook.equals(inventory.requests().identity()))inventory.requests().cancelDestination(GlobalPos.of(level.dimension(),worldPosition));
            inventory.unlinkMemory(worldPosition);
        }else if(requestBook!=null)CourierCleanupData.get(server).removeTerminal(core,requestBook,worldPosition);
    }
    public void cancel(ItemStack item,int count){
        var inventory=core();if(level!=null&&!level.isClientSide&&inventory!=null&&powered()&&!item.isEmpty()&&count>0&&count<=64)
            inventory.requests().cancel(new RequestedItem(item),GlobalPos.of(level.dimension(),worldPosition),count);
    }
    public static void tick(Level level,BlockPos pos,BlockState state,RequisitionTomeBlockEntity tome){
        if(level.isClientSide){
            if(tome.animation.tick(state.getValue(RequisitionTomeBlock.POWERED),bound->level.random.nextInt(bound)))
                level.playLocalSound(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,
                    net.minecraft.sounds.SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation("thaumcraft","page")),
                    net.minecraft.sounds.SoundSource.BLOCKS,.1f,.9f+level.random.nextFloat()*.1f,false);
            return;
        }
        if(tome.core!=null&&level.hasChunkAt(tome.core)&&(!(level.getBlockEntity(tome.core) instanceof InventariumBlockEntity inventory)||!inventory.hasMemoryUser(pos)))tome.linkCore(null);
        int before=tome.power.remaining();tome.power.tick(tome.core()!=null,amount->VisNetHandler.drainVis(level,pos,Aspect.ORDER,amount));
        if(before!=tome.power.remaining())tome.setChanged();
        if(state.getValue(RequisitionTomeBlock.POWERED)!=tome.powered())level.setBlock(pos,state.setValue(RequisitionTomeBlock.POWERED,tome.powered()),3);
    }
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);for(int i=0;i<2;i++){tag.put("MirrorEye"+i,mirrorEyes[i].save(new CompoundTag()));tag.putBoolean("GhostEye"+i,ghostEyes[i]);}if(requestBook!=null)tag.putUUID("RequestBook",requestBook);if(core!=null)tag.put("Core",NbtUtils.writeBlockPos(core));tag.putInt("PoweredTicks",power.remaining());}
    @Override public void load(CompoundTag tag){super.load(tag);for(int i=0;i<2;i++){mirrorEyes[i]=ItemStack.of(tag.getCompound("MirrorEye"+i));ghostEyes[i]=tag.getBoolean("GhostEye"+i);}requestBook=tag.hasUUID("RequestBook")?tag.getUUID("RequestBook"):null;core=tag.contains("Core",Tag.TAG_COMPOUND)?NbtUtils.readBlockPos(tag.getCompound("Core")):null;power.restore(tag.getInt("PoweredTicks"));}
}
