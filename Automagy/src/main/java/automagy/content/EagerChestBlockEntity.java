package automagy.content;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import net.minecraftforge.items.wrapper.*;
import thaumcraft.api.aspects.*;

public final class EagerChestBlockEntity extends InventoryBlockEntity implements MenuProvider {
    private final Set<UUID> users=new HashSet<>(),viewers=new HashSet<>();
    private boolean open;
    private int anticipationCooldown,sneakingTicks;
    private UUID closest;
    public float lid,previousLid;
    private LazyOptional<IItemHandler> exposed=LazyOptional.of(this::storage);
    public EagerChestBlockEntity(BlockPos pos,BlockState state){super(Content.EAGER_TILE.get(),pos,state,31);}
    public IItemHandler storage(){return new RangedWrapper(inventory,0,27);}
    public IItemHandler menuInventory(){return inventory;}
    @Override protected int slotLimit(int slot){return slot>=27?1:64;}
    @Override protected boolean accepts(int slot,ItemStack stack){return slot<27||stack.getItem() instanceof FilterItem&&ItemFilter.load(stack).populated();}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){return cap==ForgeCapabilities.ITEM_HANDLER&&!isRemoved()?exposed.cast():super.getCapability(cap,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();exposed.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();exposed=LazyOptional.of(this::storage);}
    public boolean allowed(Player player){return users.contains(player.getUUID());}
    public void allow(Player player,boolean value){if(value)users.add(player.getUUID());else users.remove(player.getUUID());sync();}
    private List<EagerFilters.Sheet> sheets(int start){
        List<EagerFilters.Sheet> result=new ArrayList<>();
        for(int i=start;i<start+2;i++)if(stored(i).getItem() instanceof FilterItem item)result.add(new EagerFilters.Sheet(ItemFilter.load(stored(i)),item.blacklist));
        return result;
    }
    public void trade(Player player){
        if(level==null||level.isClientSide||player.isShiftKeyDown()||player.isSpectator()||!allowed(player)||player.distanceToSqr(worldPosition.getCenter())>64)return;
        IItemHandler playerInv=new RangedWrapper(new InvWrapper(player.getInventory()),0,36);
        List<EagerFilters.Sheet> give=sheets(27),take=sheets(29);int moved=0;
        if(!take.isEmpty()||give.isEmpty())moved+=exchange(playerInv,storage(),take,false,player);
        if(!give.isEmpty()||take.isEmpty())moved+=exchange(storage(),playerInv,give,true,player);
        if(!take.isEmpty()||give.isEmpty())moved+=exchange(playerInv,storage(),take,false,player);
        player.getInventory().setChanged();player.containerMenu.broadcastChanges();setChanged();
        if(moved>0)level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,net.minecraft.sounds.SoundSource.BLOCKS,.5f,.9f+level.random.nextFloat()*.1f);
    }
    private int exchange(IItemHandler source,IItemHandler destination,List<EagerFilters.Sheet> sheets,boolean giving,Player player){
        IItemHandler ordered=giving?source:reverse(source);List<ItemStack> seen=new ArrayList<>();int moved=0;
        for(ItemStack key:InventoryTransfer.snapshot(ordered,false)){
            if(seen.stream().anyMatch(s->EagerFilters.matches(sheets,s,key)))continue;seen.add(key);
            long maximum;
            if(sheets.isEmpty()){
                boolean building=key.is(Items.DIRT)||key.is(Items.STONE)||key.is(Items.COBBLESTONE)||key.is(Items.GRAVEL);
                boolean accepts=giving?building:!building&&new AspectList(key).getAmount(Aspect.GREED)>0;
                maximum=accepts?Long.MAX_VALUE:0;
            }else maximum=EagerFilters.limit(sheets,key,InventoryTransfer.snapshot(source,false),InventoryTransfer.snapshot(destination,false));
            moved+=InventoryTransfer.move(ordered,destination,key,(int)Math.min(Integer.MAX_VALUE,maximum),(a,b)->EagerFilters.matches(sheets,a,b),
                    stack->Containers.dropItemStack(level,player.getX(),player.getY(),player.getZ(),stack));
        }
        return moved;
    }
    private static IItemHandler reverse(IItemHandler source){return new IItemHandler(){
        public int getSlots(){return source.getSlots();}private int slot(int index){return getSlots()-1-index;}
        public ItemStack getStackInSlot(int index){return source.getStackInSlot(slot(index));}
        public ItemStack insertItem(int index,ItemStack stack,boolean simulate){return source.insertItem(slot(index),stack,simulate);}
        public ItemStack extractItem(int index,int amount,boolean simulate){return source.extractItem(slot(index),amount,simulate);}
        public int getSlotLimit(int index){return source.getSlotLimit(slot(index));}
        public boolean isItemValid(int index,ItemStack stack){return source.isItemValid(slot(index),stack);}
    };}
    public void viewer(Player player,boolean value){if(level.isClientSide)return;if(value)viewers.add(player.getUUID());else viewers.remove(player.getUUID());updateOpen();}
    private void updateOpen(){boolean value=!viewers.isEmpty();if(value==open)return;open=value;sync();level.playSound(null,worldPosition,open?net.minecraft.sounds.SoundEvents.CHEST_OPEN:net.minecraft.sounds.SoundEvents.CHEST_CLOSE,net.minecraft.sounds.SoundSource.BLOCKS,.5f,1);}
    public static void tick(Level level,BlockPos pos,BlockState state,EagerChestBlockEntity tile){
        if(level.isClientSide){
            tile.previousLid=tile.lid;tile.lid=Math.max(0,Math.min(1,tile.lid+(tile.open?.1f:-.1f)));
            if(tile.anticipationCooldown>0)tile.anticipationCooldown--;
            else if(!tile.open&&tile.lid<.01f){
                Player player=level.getNearestPlayer(pos.getX(),pos.getY(),pos.getZ(),7,false);
                UUID id=player==null?null:player.getUUID();if(!Objects.equals(tile.closest,id)){tile.closest=id;tile.sneakingTicks=0;}
                if(player!=null&&!(player.containerMenu instanceof EagerChestMenu menu&&menu.tile()==tile)){
                    boolean sneak=player.isShiftKeyDown();var hit=player.pick(7,0,false);
                    boolean looking=hit instanceof net.minecraft.world.phys.BlockHitResult blockHit&&blockHit.getBlockPos().equals(pos);
                    double distance=player.distanceToSqr(pos.getX(),pos.getY(),pos.getZ());float angle=(float)(Math.max(50-distance,0)/(sneak||!looking?1200:800));
                    if(sneak){tile.sneakingTicks++;if(tile.sneakingTicks>40)angle=Math.max(0,angle*(1-(tile.sneakingTicks-40)/30f));tile.lid=Math.min(tile.previousLid,angle);}
                    else {tile.sneakingTicks=0;tile.lid=angle;}
                }
            }return;
        }
        if(level.getGameTime()%20==0){tile.viewers.removeIf(id->{Player p=level.getPlayerByUUID(id);return p==null||!(p.containerMenu instanceof EagerChestMenu menu)||menu.tile()!=tile;});tile.updateOpen();}
    }
    @Override protected void inventoryChanged(){super.inventoryChanged();if(level!=null&&!level.isClientSide)level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock());}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);ListTag list=new ListTag();for(UUID id:users)list.add(NbtUtils.createUUID(id));tag.put("Users",list);}
    @Override public void load(CompoundTag tag){super.load(tag);users.clear();for(Tag raw:tag.getList("Users",Tag.TAG_INT_ARRAY))if(((IntArrayTag)raw).getAsIntArray().length==4)users.add(NbtUtils.loadUUID(raw));boolean next=tag.getBoolean("Open");if(open!=next){anticipationCooldown=200;closest=null;}open=next;}
    @Override public CompoundTag getUpdateTag(){var tag=super.getUpdateTag();tag.putBoolean("Open",open);return tag;}
    @Override public Component getDisplayName(){return getBlockState().getBlock().getName();}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player player){return new EagerChestMenu(id,inv,this);}
}
