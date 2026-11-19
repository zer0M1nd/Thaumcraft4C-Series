package gadomancymodern.automation;

import gadomancymodern.Gadomancy;
import gadomancymodern.item.ArcanePackageItem;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import tc4revival.aspect.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;

public class PackagerBlockEntity extends BlockEntity implements WorldlyContainer,MenuProvider,IEssentiaTransport {
    private NonNullList<ItemStack> items=NonNullList.withSize(12,ItemStack.EMPTY);
    private final PackagingCycle cycle=new PackagingCycle();
    public boolean autoStart,useEssentia,disguise;
    public int cloth;
    private final java.util.Map<Direction,LazyOptional<net.minecraftforge.items.IItemHandler>> handlers=new java.util.EnumMap<>(Direction.class);
    public PackagerBlockEntity(BlockPos pos,BlockState state){super(Gadomancy.PACKAGER_BE.get(),pos,state);}
    public int progress(){return cycle.progress();}
    public static void tick(Level level,BlockPos pos,BlockState state,PackagerBlockEntity be){
        if(level.isClientSide)return;
        be.powerChanged(level.hasNeighborSignal(pos));
        if(level.getGameTime()%5==0)be.pullEssentia();
        int previous=be.progress();
        if(be.cycle.tick(be.autoStart,be.canPack(),level.getGameTime()))be.pack();
        if(previous!=be.progress())be.changed();
    }
    public void powerChanged(boolean powered){int before=progress();cycle.powerChanged(powered,canPack());if(before!=progress())changed();}
    public boolean canPack(){
        if(!items.get(11).isEmpty()||java.util.stream.IntStream.range(0,9).allMatch(i->items.get(i).isEmpty()))return false;
        return useEssentia?cloth>=4:items.get(9).is(Items.LEATHER)&&items.get(10).is(Items.STRING);
    }
    public boolean pack(){
        if(level==null||level.isClientSide||!canPack())return false;
        ItemStack result=new ItemStack(disguise?Gadomancy.DISGUISED_PACKAGE.get():Gadomancy.PACKAGE.get());
        result.getOrCreateTag().putBoolean("essentia_wrapper",useEssentia);
        java.util.List<ItemStack> payload=new java.util.ArrayList<>();for(int i=0;i<9;i++)payload.add(items.get(i));
        if(!((ArcanePackageItem)result.getItem()).setContents(result,payload)){
            // Legacy failure returns every input into the world; it never destroys the items.
            for(int i=0;i<9;i++){Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+.8125,worldPosition.getZ()+.5,items.get(i));items.set(i,ItemStack.EMPTY);}
            level.explode(null,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,1,Level.ExplosionInteraction.NONE);changed();return false;
        }
        if(useEssentia)cloth-=4;else{items.get(9).shrink(1);items.get(10).shrink(1);}
        for(int i=0;i<9;i++)items.set(i,ItemStack.EMPTY);items.set(11,result);changed();return true;
    }
    private void pullEssentia(){
        if(!useEssentia||cloth>=8||level==null)return;
        for(Direction direction:Direction.values())if(level.getBlockEntity(worldPosition.relative(direction)) instanceof tc4revival.content.EssentiaTransport source){
            Direction face=direction.getOpposite();var type=source.essentiaType(face);
            if(source.canOutput(face)&&type!=null&&type.tag().equals("pannus")&&source.suction(face)<suction(direction)&&suction(direction)>=source.minimumSuction()){
                int moved=source.takeEssentia(type,Math.min(1,8-cloth),face);if(moved>0){cloth+=moved;changed();}
            }
        }
    }
    public void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);ContainerHelper.saveAllItems(tag,items);tag.putInt("progress",progress());tag.putInt("cloth",cloth);tag.putBoolean("auto",autoStart);tag.putBoolean("essentia",useEssentia);tag.putBoolean("disguise",disguise);}
    @Override public void load(CompoundTag tag){super.load(tag);items=NonNullList.withSize(12,ItemStack.EMPTY);ContainerHelper.loadAllItems(tag,items);cycle.restore(tag.getInt("progress"));cloth=Math.max(0,Math.min(8,tag.getInt("cloth")));autoStart=tag.getBoolean("auto");useEssentia=tag.getBoolean("essentia");disguise=tag.getBoolean("disguise");}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public int getContainerSize(){return 12;}
    @Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int slot){return items.get(slot);}
    @Override public ItemStack removeItem(int slot,int amount){ItemStack removed=ContainerHelper.removeItem(items,slot,amount);if(!removed.isEmpty())changed();return removed;}
    @Override public ItemStack removeItemNoUpdate(int slot){return ContainerHelper.takeItem(items,slot);}
    @Override public void setItem(int slot,ItemStack stack){items.set(slot,stack);if(stack.getCount()>getMaxStackSize())stack.setCount(getMaxStackSize());changed();}
    @Override public boolean stillValid(Player player){return level!=null&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64;}
    @Override public void clearContent(){items.clear();changed();}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return slot<9||slot==9&&stack.is(Items.LEATHER)||slot==10&&stack.is(Items.STRING);}
    @Override public int[] getSlotsForFace(Direction face){return face==Direction.DOWN?new int[]{11}:face==getBlockState().getValue(PackagerBlock.FACING)?new int[]{9,10,0,1,2,3,4,5,6,7,8}:new int[0];}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction face){return canPlaceItem(slot,stack)&&(!useEssentia||slot<9);}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction face){return slot==11&&face==Direction.DOWN;}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability,Direction side){
        if(capability==ForgeCapabilities.ITEM_HANDLER)return isRemoved()||!capabilitiesValid?LazyOptional.empty():(side==null?unsided:handlers.computeIfAbsent(side,d->LazyOptional.of(()->new SidedInvWrapper(this,d)))).cast();
        return super.getCapability(capability,side);
    }
    private boolean capabilitiesValid=true;
    private LazyOptional<net.minecraftforge.items.IItemHandler> unsided=LazyOptional.of(()->new net.minecraftforge.items.wrapper.InvWrapper(this));
    @Override public void invalidateCaps(){super.invalidateCaps();capabilitiesValid=false;unsided.invalidate();handlers.values().forEach(LazyOptional::invalidate);handlers.clear();}
    @Override public void reviveCaps(){super.reviveCaps();capabilitiesValid=true;unsided=LazyOptional.of(()->new net.minecraftforge.items.wrapper.InvWrapper(this));}
    @Override public Component getDisplayName(){return Component.translatable("tile.BlockStoneMachine.4.name");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new PackagerMenu(id,inventory,this);}
    private Aspect clothAspect(){return tc4revival.aspect.TCAspects.catalog().require("pannus");}
    @Override public boolean connectable(Direction face){return true;}
    @Override public boolean canInput(Direction face){return true;}
    @Override public boolean canOutput(Direction face){return false;}
    @Override public Aspect suctionType(Direction face){return clothAspect();}
    @Override public int suction(Direction face){return cloth<8?64:0;}
    @Override public int minimumSuction(){return 64;}
    @Override public Aspect essentiaType(Direction face){return cloth>0?clothAspect():null;}
    @Override public int essentiaAmount(Direction face){return cloth;}
    @Override public int takeEssentia(Aspect type,int amount,Direction face){return 0;}
    @Override public int addEssentia(Aspect type,int amount,Direction face){if(type==null||!type.tag().equals("pannus"))return 0;int accepted=Math.max(0,Math.min(amount,8-cloth));cloth+=accepted;if(accepted>0)changed();return accepted;}
}
