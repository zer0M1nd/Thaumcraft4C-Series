package thaumicexploration;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.wrapper.InvWrapper;
import tc4revival.aspect.*;
import thaumcraft.api.aspects.*;

public class BoundEntity extends BlockEntity implements IEssentiaStorage, IEssentiaVessel, IEssentiaTransport, tc4revival.content.RevealedAspectContainer {
    public String network = "";
    public int color;
    public float lid,previousLid;
    private boolean open;
    private AspectAmounts clientContents = new AspectAmounts();
    private final SimpleContainer clientInventory = new SimpleContainer(27);
    private LazyOptional<net.minecraftforge.items.IItemHandler> inventory = LazyOptional.of(this::dynamicInventory);
    private CompoundTag lastSnapshot = new CompoundTag();
    public BoundEntity(BlockPos pos, BlockState state) { super(ExplorationMod.BOUND.get(), pos, state); }
    public boolean isJar() { return getBlockState().getBlock() == ExplorationMod.block("bound_jar"); }
    public String key() { return color + ":" + network; }
    public SimpleContainer chest() { return level instanceof ServerLevel server && !network.isEmpty() ? BoundNetworks.get(server).chest(key()) : clientInventory; }
    private IEssentiaStorage storage() { return BoundNetworks.get((ServerLevel)level).jar(key()); }
    public void initialize(String network, int color) { this.network=network; this.color=color; setChanged(); sync(); }
    private net.minecraftforge.items.IItemHandler dynamicInventory() {
        return new net.minecraftforge.items.IItemHandler() {
            public int getSlots(){return 27;}
            public net.minecraft.world.item.ItemStack getStackInSlot(int slot){return chest().getItem(slot);}
            public net.minecraft.world.item.ItemStack insertItem(int slot,net.minecraft.world.item.ItemStack stack,boolean simulate){return new InvWrapper(chest()).insertItem(slot,stack,simulate);}
            public net.minecraft.world.item.ItemStack extractItem(int slot,int amount,boolean simulate){return new InvWrapper(chest()).extractItem(slot,amount,simulate);}
            public int getSlotLimit(int slot){return 64;}
            public boolean isItemValid(int slot,net.minecraft.world.item.ItemStack stack){return true;}
        };
    }
    @Override public void onLoad(){super.onLoad();if(level instanceof ServerLevel&&network.isEmpty())initialize(java.util.UUID.randomUUID().toString(),color);}
    public void sync() { if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }
    public void tick() {
        if(level!=null&&level.isClientSide){previousLid=lid;lid=net.minecraft.util.Mth.clamp(lid+(open?.1F:-.1F),0,1);return;}
        if (!(level instanceof ServerLevel)) return;
        if(!isJar()&&level.getGameTime()%5==0){
            boolean next=((ServerLevel)level).getServer().getPlayerList().getPlayers().stream().anyMatch(p->p.containerMenu instanceof net.minecraft.world.inventory.ChestMenu menu&&menu.getContainer()==chest());
            if(next!=open){open=next;sync();level.playSound(null,worldPosition,open?net.minecraft.sounds.SoundEvents.CHEST_OPEN:net.minecraft.sounds.SoundEvents.CHEST_CLOSE,net.minecraft.sounds.SoundSource.BLOCKS,.5F,.9F+level.random.nextFloat()*.1F);}
        }
        if(isJar()&&level.getGameTime()%5==0&&amount()<64&&level.hasChunkAt(worldPosition.above())
            &&level.getBlockEntity(worldPosition.above()) instanceof tc4revival.content.EssentiaTransport source
            &&source.connectable(Direction.DOWN)&&source.canOutput(Direction.DOWN)&&source.suction(Direction.DOWN)<32&&source.minimumSuction()<=32){
            var aspect=source.essentiaType(Direction.DOWN);
            if(aspect!=null&&insert(aspect,1,true)>0)insert(aspect,source.takeEssentia(aspect,1,Direction.DOWN),false);
        }
        if(level.getGameTime()%10!=0)return;
        var snapshot = getUpdateTag(); if (!snapshot.equals(lastSnapshot)) { lastSnapshot=snapshot; sync(); }
    }
    @Override public AspectAmounts contents() { return !isJar() ? new AspectAmounts() : level instanceof ServerLevel && !network.isEmpty() ? storage().contents() : clientContents.copy(); }
    @Override public AspectAmounts revealedAspects() { return contents(); }
    @Override public int totalCapacity() { return isJar()?64:0; }
    @Override public int capacity(tc4revival.aspect.Aspect aspect) { return isJar() && accepts(aspect)?64:0; }
    @Override public int insert(tc4revival.aspect.Aspect aspect,int amount,boolean simulate) {
        if (!(level instanceof ServerLevel) || !isJar() || network.isEmpty()) return 0;
        int changed=storage().insert(aspect,amount,simulate); if(changed>0&&!simulate)sync(); return changed;
    }
    @Override public int extract(tc4revival.aspect.Aspect aspect,int amount,boolean simulate) {
        if (!(level instanceof ServerLevel) || !isJar() || network.isEmpty()) return 0;
        int changed=storage().extract(aspect,amount,simulate); if(changed>0&&!simulate)sync(); return changed;
    }
    @Override public tc4revival.aspect.Aspect aspect() { return contents().view().keySet().stream().findFirst().orElse(null); }
    @Override public int amount() { return (int)contents().total(); }
    @Override public int capacity() { return 64; }
    @Override public boolean accepts(tc4revival.aspect.Aspect a) { return a!=null && (amount()==0 || a.equals(aspect())); }
    @Override public boolean contains(tc4revival.aspect.Aspect a,int n) { return n>=0 && contents().amount(a)>=n; }
    @Override public boolean takeFromContainer(tc4revival.aspect.Aspect a,int n) { return n>=0 && extract(a,n,true)==n && extract(a,n,false)==n; }
    @Override public int addToContainer(tc4revival.aspect.Aspect a,int n) { return n-insert(a,n,false); }
    @Override public boolean acceptsPhials() { return isJar(); }
    @Override public boolean connectable(Direction face) { return isJar() && face==Direction.UP; }
    @Override public boolean canInput(Direction face) { return connectable(face); }
    @Override public boolean canOutput(Direction face) { return connectable(face); }
    @Override public tc4revival.aspect.Aspect suctionType(Direction face) { return aspect(); }
    @Override public int suction(Direction face) { return connectable(face)&&amount()<64?32:0; }
    @Override public int minimumSuction() { return 32; }
    @Override public tc4revival.aspect.Aspect essentiaType(Direction face) { return aspect(); }
    @Override public int essentiaAmount(Direction face) { return amount(); }
    @Override public int takeEssentia(tc4revival.aspect.Aspect a,int n,Direction face) { return canOutput(face)?extract(a,n,false):0; }
    @Override public int addEssentia(tc4revival.aspect.Aspect a,int n,Direction face) { return canInput(face)?insert(a,n,false):0; }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putString("network",network); tag.putInt("color",color); }
    @Override public void load(CompoundTag tag) { super.load(tag); network=tag.getString("network"); color=tag.getInt("color"); open=tag.getBoolean("open"); if(tag.contains("contents"))clientContents=AspectNbt.read(tag.getCompound("contents"),TCAspects.catalog()).amounts(); }
    @Override public CompoundTag getUpdateTag() { var tag=super.getUpdateTag(); tag.putBoolean("open",open); tag.putString("network",network); tag.putInt("color",color); var contents=new CompoundTag(); AspectNbt.write(contents,contents()); tag.put("contents",contents); return tag; }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability,Direction side) { return !isJar()&&capability==ForgeCapabilities.ITEM_HANDLER?inventory.cast():super.getCapability(capability,side); }
    @Override public void invalidateCaps() { super.invalidateCaps(); inventory.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); inventory=LazyOptional.of(this::dynamicInventory); }
}
