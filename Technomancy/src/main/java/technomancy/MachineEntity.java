package technomancy;

import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.*;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.*;
import tc4revival.aspect.*;
import tc4revival.content.*;
import thaumcraft.api.aspects.IEssentiaStorage;
import thaumcraft.api.aspects.EssentiaStorageApi;

/** Common persistence and automation contract. Gameplay lives in the subsystem classes. */
public class MachineEntity extends BlockEntity implements MenuProvider,IEssentiaStorage,EssentiaTransport,EssentiaVessel,RevealedAspectContainer {
    public final MachineKind kind;
    public UUID owner;
    public int energy,mana,fuel,progress,redstone,mode;
    public boolean boost,active;
    public long ticks;
    public Aspect filter;
    public AspectAmounts aspects=new AspectAmounts();
    public CompoundTag state=new CompoundTag();
    public final LinkedHashSet<BlockPos> links=new LinkedHashSet<>();
    public final ItemStackHandler inventory;
    public final FluidTank fluid;
    private LazyOptional<IEnergyStorage> energyCap;
    private LazyOptional<IItemHandler> itemCap;
    private LazyOptional<IFluidHandler> fluidCap;
    private boolean dirtyForClient;

    public MachineEntity(BlockPos pos,BlockState block){
        super(TechRegistry.MACHINE.get(),pos,block);kind=((MachineBlock)block.getBlock()).kind;
        inventory=new ItemStackHandler(kind==MachineKind.EXISTENCE_CROP_HARVESTER?9:2){
            @Override protected void onContentsChanged(int slot){changed();}
        };
        redstone=kind.dynamo()||kind==MachineKind.ELDRITCH_CONSUMER||kind==MachineKind.ESSENTIA_FUSOR?1:2;
        if(kind.processor()||kind.jar()||kind==MachineKind.GALVANIZED_BELLOWS||kind==MachineKind.MANA_FABRICATOR||kind==MachineKind.BLOOD_FABRICATOR)redstone=0;
        fluid=new FluidTank(kind==MachineKind.ORDERLY_LAMP||kind==MachineKind.MANA_EXCHANGER?1000:kind==MachineKind.BLOOD_DYNAMO?8000:10000){
            @Override protected void onContentsChanged(){changed();}
            @Override public boolean isFluidValid(FluidStack stack){return MachineLogic.validFluid(MachineEntity.this,stack);}
        };
        initCaps();
    }
    public static MachineEntity create(BlockPos p,BlockState s){
        return ((MachineBlock)s.getBlock()).kind.dependency.equals("botania")&&net.minecraftforge.fml.ModList.get().isLoaded("botania")
            ? technomancy.compat.BotaniaMachine.createBotania(p,s):new MachineEntity(p,s);
    }
    private void initCaps(){
        energyCap=LazyOptional.of(()->new IEnergyStorage(){
            public int receiveEnergy(int n,boolean simulate){int v=Math.min(Math.max(0,n),Math.max(0,kind.energyCapacity-energy));if(!canReceive())return 0;if(!simulate&&v>0){energy+=v;changed();}return v;}
            public int extractEnergy(int n,boolean simulate){int v=Math.min(Math.max(0,n),Math.min(320,energy));if(!canExtract())return 0;if(!simulate&&v>0){energy-=v;changed();}return v;}
            public int getEnergyStored(){return energy;} public int getMaxEnergyStored(){return kind.energyCapacity;}
            public boolean canExtract(){return kind.dynamo();} public boolean canReceive(){return !kind.dynamo()&&kind.energyCapacity>0;}
        });
        itemCap=LazyOptional.of(()->new IItemHandler(){
            public int getSlots(){return inventory.getSlots();}
            public ItemStack getStackInSlot(int slot){return inventory.getStackInSlot(slot);}
            public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return isItemValid(slot,stack)?inventory.insertItem(slot,stack,simulate):stack;}
            public ItemStack extractItem(int slot,int count,boolean simulate){return slot==1||kind==MachineKind.EXISTENCE_CROP_HARVESTER?inventory.extractItem(slot,count,simulate):ItemStack.EMPTY;}
            public int getSlotLimit(int slot){return 64;}
            public boolean isItemValid(int slot,ItemStack stack){return slot==0&&acceptsItem(stack);}
        });
        fluidCap=LazyOptional.of(()->new IFluidHandler(){
            public int getTanks(){return 1;}public FluidStack getFluidInTank(int t){return fluid.getFluid();}public int getTankCapacity(int t){return fluid.getCapacity();}
            public boolean isFluidValid(int t,FluidStack f){return fluid.isFluidValid(f);}
            public int fill(FluidStack f,FluidAction action){return kind==MachineKind.BLOOD_DYNAMO||kind==MachineKind.MANA_EXCHANGER&&mode%2==1?fluid.fill(f,action):0;}
            public FluidStack drain(int n,FluidAction action){return kind==MachineKind.BLOOD_DYNAMO||kind==MachineKind.MANA_EXCHANGER&&mode%2==1?FluidStack.EMPTY:fluid.drain(n,action);}
            public FluidStack drain(FluidStack f,FluidAction action){return kind==MachineKind.BLOOD_DYNAMO||kind==MachineKind.MANA_EXCHANGER&&mode%2==1?FluidStack.EMPTY:fluid.drain(f,action);}
        });
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,@Nullable Direction side){
        if(isRemoved())return LazyOptional.empty();
        if(cap==ForgeCapabilities.ENERGY&&kind.energyCapacity>0&&energySide(side))return energyCap.cast();
        if(cap==ForgeCapabilities.ITEM_HANDLER&&(kind.processor()||kind==MachineKind.ADVANCED_DECONSTRUCTION_TABLE||kind==MachineKind.EXISTENCE_CROP_HARVESTER))return itemCap.cast();
        if(cap==ForgeCapabilities.FLUID_HANDLER&&(kind==MachineKind.ORDERLY_LAMP||kind==MachineKind.BLOOD_DYNAMO&&(side==null||side!=facing())||kind==MachineKind.BLOOD_FABRICATOR||kind==MachineKind.MANA_EXCHANGER&&side!=Direction.UP))return fluidCap.cast();
        return super.getCapability(cap,side);
    }
    @Override public void invalidateCaps(){super.invalidateCaps();energyCap.invalidate();itemCap.invalidate();fluidCap.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();initCaps();}
    private boolean energySide(Direction side){
        if(side==null)return true;
        if(kind.dynamo())return side==facing();
        if(kind==MachineKind.MANA_EXCHANGER)return side!=Direction.UP;
        if(kind==MachineKind.BLOOD_FABRICATOR||kind==MachineKind.DYNAMIC_EXISTENCE_BURNER)return side==Direction.DOWN;
        return true;
    }
    public Direction facing(){return getBlockState().getValue(MachineBlock.FACING);}
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(){
        if(kind.catalyst()&&state.getString("ritual").startsWith("black_hole"))return new net.minecraft.world.phys.AABB(worldPosition).inflate(9);
        return kind==MachineKind.NODE_FABRICATOR?new net.minecraft.world.phys.AABB(worldPosition).inflate(6):super.getRenderBoundingBox();
    }
    public ServerLevel server(){return (ServerLevel)level;}
    public boolean runs(){
        boolean powered=level.hasNeighborSignal(worldPosition);
        if(kind==MachineKind.NODE_FABRICATOR&&facing().getAxis()!=Direction.Axis.Y)
            powered|=NodeStructure.cells(worldPosition,facing()).stream().anyMatch(p->level.hasChunkAt(p)&&level.hasNeighborSignal(p));
        return kind.enabled()&&(boost&&(kind==MachineKind.ITEM_COIL||kind==MachineKind.ESSENTIA_COIL)||redstone==0||(redstone==1)==powered);
    }
    public boolean pay(int cost){if(cost<0||energy<cost)return false;energy-=cost;changed();return true;}
    public void changed(){setChanged();dirtyForClient=true;}
    public void tick(){
        if(level==null||level.isClientSide)return;
        ticks++;
        boolean wasActive=active;
        if(kind.enabled()){
            MachineLogic.tick(this);
            if(kind.catalyst())RitualLogic.tick(this);
            if(kind.name().contains("EXISTENCE"))ExistenceLogic.tick(this);
        }
        if(wasActive!=active)changed();
        if(ticks%5==0&&dirtyForClient){
            dirtyForClient=false;level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
            if(getBlockState().getValue(MachineBlock.ACTIVE)!=active)level.setBlock(worldPosition,getBlockState().setValue(MachineBlock.ACTIVE,active),3);
            if(kind==MachineKind.MANA_EXCHANGER&&getBlockState().getValue(MachineBlock.REVERSED)!=(mode%2==1))level.setBlock(worldPosition,getBlockState().setValue(MachineBlock.REVERSED,mode%2==1),3);
        }
    }
    public boolean acceptsItem(ItemStack stack){
        if(kind.processor())return !OreProcessing.output(stack,kind,level).isEmpty();
        if(kind==MachineKind.ADVANCED_DECONSTRUCTION_TABLE)return !tc4revival.aspect.item.ItemStackAspects.resolve(stack).orElseGet(AspectAmounts::new).isEmpty();
        return false;
    }
    public void activate(Player player){
        if(player!=null&&owner==null)owner=player.getUUID();
        if(kind==MachineKind.NODE_FABRICATOR)MachineLogic.beginNode(this);
        else if(kind.catalyst())RitualLogic.activate(this,player);
    }
    public boolean interact(Player player,InteractionHand hand,Direction face){return MachineLogic.interact(this,player,hand,face);}
    public void dropContents(){
        for(int i=0;i<inventory.getSlots();i++){var stack=inventory.getStackInSlot(i);if(!stack.isEmpty())Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,stack.copy());}
        if(boost)Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,new ItemStack(TechRegistry.item("potency_gem")));
        var escrow=ItemStack.of(state.getCompound("itemEscrow"));if(!escrow.isEmpty())Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,escrow);
    }
    public Aspect aspect(){return filter!=null?filter:aspects.aspects().stream().findFirst().orElse(null);}
    public int amount(){return kind==MachineKind.CREATIVE_JAR&&filter!=null?640:(int)aspects.total();}
    public int capacity(){return kind.essentiaCapacity;}
    public boolean acceptsPhials(){return kind.essentiaCapacity>0&&kind!=MachineKind.ELDRITCH_CONSUMER&&kind!=MachineKind.ENERGY_CONDENSER&&kind!=MachineKind.ESSENTIA_FUSOR&&kind!=MachineKind.CREATIVE_JAR;}
    public boolean accepts(Aspect a){
        if(a==null||kind.essentiaCapacity==0)return false;
        if(kind==MachineKind.IGNIS_INCINERATOR)return a.tag().equals("ignis");
        if(kind==MachineKind.ORDERLY_LAMP)return a.tag().equals("ordo");
        if(kind==MachineKind.NODE_FABRICATOR&&!NodeFabrication.accepts(this,a))return false;
        if(kind==MachineKind.ELDRITCH_CONSUMER||kind==MachineKind.ENERGY_CONDENSER||kind==MachineKind.CREATIVE_JAR||kind==MachineKind.ESSENTIA_FUSOR)return false;
        return (filter==null||filter.equals(a))&&(aspects.isEmpty()||aspects.amount(a)>0);
    }
    public boolean contains(Aspect a,int n){return n>=0&&contents().amount(a)>=n;}
    public boolean takeFromContainer(Aspect a,int n){if(!contains(a,n))return false;return extract(a,n,false)==n;}
    public int addToContainer(Aspect a,int n){return n-insert(a,n,false);}
    public AspectAmounts contents(){return kind==MachineKind.CREATIVE_JAR&&filter!=null?new AspectAmounts().add(filter,640):aspects.copy();}
    public AspectAmounts revealedAspects(){return contents();}
    public int totalCapacity(){return capacity();}
    public int capacity(Aspect a){
        if(kind==MachineKind.ESSENTIA_FUSOR)return aspects.amount(a)+MachineLogic.fusionTransfer(this,a,Integer.MAX_VALUE,true,true);
        return accepts(a)?aspects.amount(a)+Math.max(0,capacity()-(int)aspects.total()):aspects.amount(a);
    }
    public int insert(Aspect a,int n,boolean simulate){
        if(n<0)throw new IllegalArgumentException("Negative essentia");
        if(kind==MachineKind.ESSENTIA_FUSOR)return MachineLogic.fusionTransfer(this,a,n,simulate,true);
        int v=accepts(a)?Math.min(n,Math.max(0,capacity()-(int)aspects.total())):0;
        if(v>0&&!simulate){aspects.add(a,v);changed();EssentiaStorageApi.changed(this);}return v;
    }
    public int extract(Aspect a,int n,boolean simulate){
        if(n<0)throw new IllegalArgumentException("Negative essentia");
        if(kind==MachineKind.ESSENTIA_FUSOR)return MachineLogic.fusionTransfer(this,a,n,simulate,false);
        int v=Math.min(n,contents().amount(a));
        if(v>0&&!simulate&&kind!=MachineKind.CREATIVE_JAR){aspects.remove(a,v);changed();EssentiaStorageApi.changed(this);}return v;
    }
    public boolean connectable(Direction d){if(kind==MachineKind.ESSENTIA_COIL)return d==facing();return kind.essentiaCapacity>0&&(d==null||((kind!=MachineKind.QUANTUM_JAR&&kind!=MachineKind.CREATIVE_JAR)||d==Direction.UP)&&(kind!=MachineKind.NODE_FABRICATOR||d!=facing()));}
    public boolean canInput(Direction d){return connectable(d)&&kind!=MachineKind.ESSENTIA_COIL&&kind!=MachineKind.CREATIVE_JAR&&kind!=MachineKind.ELDRITCH_CONSUMER&&kind!=MachineKind.ENERGY_CONDENSER&&(kind!=MachineKind.ESSENTIA_FUSOR||d!=null&&state.getInt("side"+d.ordinal())==1);}
    public boolean canOutput(Direction d){if(kind==MachineKind.ESSENTIA_COIL)return d==facing();return connectable(d)&&(kind.jar()||kind==MachineKind.NODE_FABRICATOR||kind==MachineKind.ENERGY_CONDENSER||kind==MachineKind.ELDRITCH_CONSUMER||(kind==MachineKind.ESSENTIA_FUSOR&&d!=null&&state.getInt("side"+d.ordinal())==2));}
    public Aspect suctionType(Direction d){return kind==MachineKind.ESSENTIA_FUSOR?MachineLogic.sideAspect(this,d):kind==MachineKind.ORDERLY_LAMP?MachineLogic.aspect("ordo"):kind==MachineKind.IGNIS_INCINERATOR?MachineLogic.aspect("ignis"):aspect();}
    public int suction(Direction d){return canInput(d)&&amount()<capacity()?kind==MachineKind.QUANTUM_JAR?(filter==null?48:56)+amount()/50:kind==MachineKind.NODE_FABRICATOR?48:128:0;}
    public int minimumSuction(){return kind==MachineKind.QUANTUM_JAR?(filter==null?48:56)+amount()/50:kind==MachineKind.NODE_FABRICATOR?32:0;}
    public Aspect essentiaType(Direction d){return kind==MachineKind.ESSENTIA_FUSOR?MachineLogic.sideAspect(this,d):aspect();}
    public int essentiaAmount(Direction d){if(kind==MachineKind.ESSENTIA_COIL)return links.isEmpty()?0:1;return kind==MachineKind.ESSENTIA_FUSOR?(d==null?amount():state.getInt("amount"+d.ordinal())):d==null||connectable(d)?amount():0;}
    public int takeEssentia(Aspect a,int n,Direction d){if(kind==MachineKind.ESSENTIA_COIL)return CoilLogic.supply(this,a,n,d);return canOutput(d)?kind==MachineKind.ESSENTIA_FUSOR?MachineLogic.sideExtract(this,a,n,d):extract(a,n,false):0;}
    public int addEssentia(Aspect a,int n,Direction d){return canInput(d)?kind==MachineKind.ESSENTIA_FUSOR?MachineLogic.sideInsert(this,a,n,d):insert(a,n,false):0;}
    @Override public Component getDisplayName(){return getBlockState().getBlock().getName();}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new MachineMenu(id,inv,this);}
    @Override protected void saveAdditional(CompoundTag tag){
        super.saveAdditional(tag);tag.putInt("schema",1);tag.putInt("energy",energy);tag.putInt("mana",mana);tag.putInt("fuel",fuel);tag.putInt("progress",progress);
        tag.putInt("redstone",redstone);tag.putInt("mode",mode);tag.putBoolean("boost",boost);tag.putBoolean("active",active);
        if(owner!=null)tag.putUUID("owner",owner);if(filter!=null)tag.putString("filter",filter.tag());
        var aspectData=new CompoundTag();AspectNbt.write(aspectData,aspects);tag.put("essentia",aspectData);
        tag.put("inventory",inventory.serializeNBT());tag.put("fluid",fluid.writeToNBT(new CompoundTag()));tag.put("state",state.copy());
        tag.putLongArray("links",links.stream().mapToLong(BlockPos::asLong).toArray());
    }
    @Override public void load(CompoundTag tag){
        super.load(tag);energy=Math.max(0,tag.getInt("energy"));mana=Math.max(0,tag.getInt("mana"));fuel=Math.max(0,tag.getInt("fuel"));progress=Math.max(0,tag.getInt("progress"));
        redstone=tag.contains("redstone")?Math.floorMod(tag.getInt("redstone"),3):redstone;mode=Math.max(0,tag.getInt("mode"));boost=tag.getBoolean("boost");active=tag.getBoolean("active");
        owner=tag.hasUUID("owner")?tag.getUUID("owner"):null;filter=MachineLogic.aspect(tag.getString("filter"));
        aspects=AspectNbt.read(tag.getCompound("essentia"),TCAspects.catalog()).amounts();
        if(tag.contains("inventory",Tag.TAG_COMPOUND)){
            var saved=tag.getCompound("inventory").copy();saved.putInt("Size",kind==MachineKind.EXISTENCE_CROP_HARVESTER?9:2);inventory.deserializeNBT(saved);
        }
        fluid.readFromNBT(tag.getCompound("fluid"));state=tag.getCompound("state").copy();
        links.clear();for(long pos:tag.getLongArray("links"))links.add(BlockPos.of(pos));
    }
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public void onDataPacket(Connection c,ClientboundBlockEntityDataPacket packet){if(packet.getTag()!=null)load(packet.getTag());}
}
