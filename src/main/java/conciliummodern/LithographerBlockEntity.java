package conciliummodern;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

public final class LithographerBlockEntity extends BlockEntity implements Container {
    private final NonNullList<ItemStack> items=NonNullList.withSize(4,ItemStack.EMPTY);
    private int phase,timer;
    private java.util.UUID lastSleeper;
    public LithographerBlockEntity(BlockPos pos,BlockState state){super(ConciliumBlocks.LITHOGRAPHER_TILE.get(),pos,state);}
    public int phase(){return phase;}
    public int timer(){return timer;}
    public void project(){if(level!=null && !level.isClientSide && phase==0){phase=1;timer=20;changed();}}
    public static void tick(Level level,BlockPos pos,BlockState state,LithographerBlockEntity tile){
        if(tile.timer>0){tile.timer--;if(!level.isClientSide)tile.setChanged();}
        if(level.isClientSide)return;
        // A successful sleep is checked on the next server tick, after all sleep event handlers.
        if(level.getGameTime()%5==0){var sleeping=tile.sleeper();var id=sleeping==null?null:sleeping.getUUID();if(!java.util.Objects.equals(id,tile.lastSleeper)){tile.lastSleeper=id;tile.setChanged();if(id!=null)tile.project();}}
        if(tile.phase!=0 && tile.timer==0){
            if(tile.phase==1){tile.apply();tile.phase=2;tile.timer=20;}
            else if(tile.phase==2){tile.phase=3;tile.timer=20;}
            else tile.phase=0;
            tile.changed();
        }
    }
    private ServerPlayer sleeper(){
        if(!(level instanceof ServerLevel server))return null;
        var bed=worldPosition.below();var state=level.getBlockState(bed);
        if(!(state.getBlock() instanceof BedBlock) || state.getValue(BedBlock.PART)!=BedPart.HEAD)return null;
        for(var player:server.players())if(player.isSleeping() && player.getSleepingPos().filter(bed::equals).isPresent())return player;
        return null;
    }
    private void apply(){
        var player=sleeper();if(player==null || !level.hasChunkAt(worldPosition.above(2)) || !(level.getBlockEntity(worldPosition.above()) instanceof VisCondenserBlockEntity)
                || !(level.getBlockEntity(worldPosition.above(2)) instanceof DestabilizedCrystalBlockEntity crystal) || !crystal.aspectKey().equals("sensus") || crystal.amount()<32)return;
        var sources=new java.util.ArrayList<CompoundTag>();for(var stack:items)sources.add(stack.getItem() instanceof AstralMonitor?MonitorSource.from(AstralMonitor.stored(stack)):new CompoundTag());
        if(crystal.drain("sensus",32)!=32)return;
        Lithography.inscribeSources(player,sources);
        level.playSound(null,worldPosition,tc4revival.content.TCSounds.ZAP.get(),net.minecraft.sounds.SoundSource.BLOCKS,1,1);
        ConciliumArcNetwork.emit(level,net.minecraft.world.phys.Vec3.atCenterOf(worldPosition),net.minecraft.world.phys.Vec3.atCenterOf(worldPosition.below()),0x00d9ff,.01f);
    }
    private void changed(){setChanged();if(level!=null && !level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override public int getContainerSize(){return 4;}
    @Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int slot){return items.get(slot);}
    @Override public ItemStack removeItem(int slot,int count){var result=ContainerHelper.removeItem(items,slot,count);if(!result.isEmpty())changed();return result;}
    @Override public ItemStack removeItemNoUpdate(int slot){return ContainerHelper.takeItem(items,slot);}
    @Override public void setItem(int slot,ItemStack stack){items.set(slot,stack);changed();}
    @Override public int getMaxStackSize(){return 1;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return stack.getItem() instanceof AstralMonitor && AstralMonitor.accepts(AstralMonitor.stored(stack));}
    @Override public boolean stillValid(Player player){return !isRemoved() && level!=null && level.getBlockEntity(worldPosition)==this && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition))<=64;}
    @Override public void clearContent(){items.clear();changed();}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);ContainerHelper.saveAllItems(tag,items);tag.putInt("phase",phase);tag.putInt("timer",timer);if(lastSleeper!=null)tag.putUUID("lastSleeper",lastSleeper);}
    @Override public void load(CompoundTag tag){super.load(tag);items.clear();ContainerHelper.loadAllItems(tag,items);phase=Math.max(0,Math.min(3,tag.getInt("phase")));timer=Math.max(0,Math.min(20,tag.getInt("timer")));lastSleeper=tag.hasUUID("lastSleeper")?tag.getUUID("lastSleeper"):null;}
    @Override public CompoundTag getUpdateTag(){var tag=new CompoundTag();tag.putInt("phase",phase);tag.putInt("timer",timer);return tag;}
    @Override public void handleUpdateTag(CompoundTag tag){phase=Math.max(0,Math.min(3,tag.getInt("phase")));timer=Math.max(0,Math.min(20,tag.getInt("timer")));}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public void onDataPacket(Connection connection,ClientboundBlockEntityDataPacket packet){if(packet.getTag()!=null)handleUpdateTag(packet.getTag());}
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(){return new net.minecraft.world.phys.AABB(worldPosition).inflate(1,2,1);}
}
