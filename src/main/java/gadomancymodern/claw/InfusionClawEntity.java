package gadomancymodern.claw;

import gadomancymodern.Gadomancy;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.*;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import tc4revival.content.*;

public class InfusionClawEntity extends BlockEntity implements WorldlyContainer,MenuProvider {
    protected ItemStack wand=ItemStack.EMPTY;
    protected java.util.UUID owner;
    protected String ownerName="";
    protected Boolean previousPower;
    protected boolean locked;
    protected int remaining,ticks;
    protected final java.util.Map<Direction,LazyOptional<net.minecraftforge.items.IItemHandler>> handlers=new java.util.EnumMap<>(Direction.class);
    public final float[] animation=new float[12];
    public float lastRenderTick=-1;
    public InfusionClawEntity(BlockPos pos,BlockState state){super(Gadomancy.CLAW_BE.get(),pos,state);}
    public boolean running(){return remaining>0;}
    public int remaining(){return remaining;}
    public boolean locked(){return locked;}
    public void locked(boolean value){locked=value;changed();}
    public boolean hasOwner(){return owner!=null;}
    public java.util.UUID owner(){return owner;}
    public boolean claim(Player player){if(level==null||level.isClientSide||player instanceof FakePlayer)return false;owner=player.getUUID();ownerName=player.getGameProfile().getName();changed();return true;}
    public static boolean accepts(ItemStack stack){return stack.getItem() instanceof CastingWandItem item&&!item.isSceptre(stack)&&WandFocusItem.attached(stack).isEmpty();}
    public boolean sufficientVis(){if(!(wand.getItem() instanceof CastingWandItem item)||level==null)return false;var costs=new java.util.LinkedHashMap<tc4revival.aspect.Aspect,Integer>();for(var aspect:tc4revival.aspect.TCAspects.catalog().primals())costs.put(aspect,250);return item.consume(wand,costs,aspect->item.cap(wand).cost(aspect.tag()),false,false,level);}
    public ClawBehavior behavior(){return level instanceof ServerLevel server&&owner!=null&&!wand.isEmpty()&&level.hasChunkAt(worldPosition.below())?ClawBehavior.find(server,worldPosition.below()):null;}
    public void powerChanged(boolean value){if(previousPower!=null&&value&&!previousPower)start();previousPower=value;}
    public boolean start(){var behavior=behavior();if(running()||behavior==null||behavior.consumesVis()&&!sufficientVis())return false;remaining=440;changed();level.blockEvent(worldPosition,getBlockState().getBlock(),0,0);return true;}
    public static void tick(Level level,BlockPos pos,BlockState state,InfusionClawEntity claw){
        if(level.isClientSide){if(claw.remaining>0)claw.remaining--;return;}
        if(claw.previousPower==null)claw.previousPower=level.hasNeighborSignal(pos);
        if(claw.remaining>0){claw.remaining--;if(claw.remaining==150)claw.click();if(claw.remaining==0)claw.changed();}
        if(++claw.ticks>21){claw.ticks=0;level.updateNeighbourForOutputSignal(pos,state.getBlock());}
    }
    public boolean click(){
        var behavior=behavior();if(behavior==null||!(level instanceof ServerLevel server)||!(wand.getItem() instanceof CastingWandItem item))return false;
        // Owner UUID directly consults the real saved knowledge for online and offline owners.
        // Never copy all research into a shared synthetic player's saved data.
        var player=new FakePlayer(server,new com.mojang.authlib.GameProfile(owner,ownerName));
        player.setPos(worldPosition.getX()+.5,worldPosition.getY(),worldPosition.getZ()+.5);
        player.setItemInHand(InteractionHand.MAIN_HAND,wand);
        if(behavior.consumesVis()){
            if(!sufficientVis())return false;
            var focus=(PrimalFocusItem)TCItems.PRIMAL_FOCUS.get();
            if(!item.consumeForPlayer(wand,player,focus.costs(wand),false,false,true))return false;
            changed();
        }
        var target=worldPosition.below();var hit=new BlockHitResult(Vec3.atCenterOf(target),Direction.UP,target,false);
        player.gameMode.useItemOn(player,server,wand,InteractionHand.MAIN_HAND,hit);
        int amount=ClawInstability.calculate(item.capacity(wand)/100,item.isStaff(wand),item.cap(wand).cost,item.cap(wand)==WandParts.Cap.VOID,item.rod(wand)==WandParts.Rod.PRIMAL_STAFF);
        behavior.instability(server,target,amount);changed();return true;
    }
    public int comparator(){return level instanceof ServerLevel server?java.util.Optional.ofNullable(ClawBehavior.find(server,worldPosition.below())).map(value->value.comparator(server,worldPosition.below())).orElse(0):0;}
    public void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override public boolean triggerEvent(int id,int value){if(id==0){if(level!=null&&level.isClientSide){animation[8]=1;remaining=440;}return true;}return super.triggerEvent(id,value);}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);if(owner!=null){tag.putUUID("Owner",owner);tag.putString("player",ownerName);}if(!wand.isEmpty())tag.put("wandStack",wand.save(new CompoundTag()));tag.putBoolean("isLocked",locked);}
    @Override public void load(CompoundTag tag){super.load(tag);owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;ownerName=tag.getString("player");wand=ItemStack.of(tag.getCompound("wandStack"));locked=tag.getBoolean("isLocked");if(tag.contains("Remaining"))remaining=tag.getInt("Remaining");}
    @Override public CompoundTag getUpdateTag(){var tag=saveWithoutMetadata();tag.putInt("Remaining",remaining);return tag;}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    @Override public AABB getRenderBoundingBox(){return new AABB(worldPosition).inflate(1);}
    @Override public int getContainerSize(){return 1;}
    @Override public int getMaxStackSize(){return 1;}
    @Override public boolean isEmpty(){return wand.isEmpty();}
    @Override public ItemStack getItem(int slot){return slot==0?wand:ItemStack.EMPTY;}
    @Override public ItemStack removeItem(int slot,int amount){if(slot!=0||running()||amount<1)return ItemStack.EMPTY;var result=wand;wand=ItemStack.EMPTY;changed();return result;}
    @Override public ItemStack removeItemNoUpdate(int slot){return ItemStack.EMPTY;}
    @Override public void setItem(int slot,ItemStack stack){if(slot==0){wand=stack;changed();}}
    @Override public void clearContent(){wand=ItemStack.EMPTY;changed();}
    @Override public boolean stillValid(Player player){return level!=null&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(Vec3.atCenterOf(worldPosition))<=64;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return slot==0&&!running()&&accepts(stack);}
    @Override public int[] getSlotsForFace(Direction side){return side==Direction.DOWN?new int[0]:new int[]{0};}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction face){return face!=Direction.DOWN&&canPlaceItem(slot,stack);}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction face){return face!=Direction.DOWN&&!running()&&(!locked||!sufficientVis());}
    private boolean capabilitiesValid=true;
    private LazyOptional<net.minecraftforge.items.IItemHandler> unsided=LazyOptional.of(()->new SidedInvWrapper(this,null));
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability,Direction side){if(capability==ForgeCapabilities.ITEM_HANDLER)return isRemoved()||!capabilitiesValid?LazyOptional.empty():(side==null?unsided:handlers.computeIfAbsent(side,d->LazyOptional.of(()->new SidedInvWrapper(this,d)))).cast();return super.getCapability(capability,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();capabilitiesValid=false;unsided.invalidate();handlers.values().forEach(LazyOptional::invalidate);handlers.clear();}
    @Override public void reviveCaps(){super.reviveCaps();capabilitiesValid=true;unsided=LazyOptional.of(()->new SidedInvWrapper(this,null));}
    @Override public Component getDisplayName(){return Component.translatable("block.gadomancy.infusion_claw");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new InfusionClawMenu(id,inventory,this);}
}
