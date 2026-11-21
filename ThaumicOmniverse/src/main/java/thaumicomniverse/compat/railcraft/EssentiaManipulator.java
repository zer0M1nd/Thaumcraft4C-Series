package thaumicomniverse.compat.railcraft;

import mods.railcraft.world.level.block.entity.manipulator.ManipulatorBlockEntity;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import tc4revival.aspect.*;
import tc4revival.content.*;
import thaumcraft.api.aspects.IEssentiaStorage;
import thaumcraft.api.aspects.EssentiaStorageApi;
import thaumicomniverse.RevivalFeatures;
import java.util.Optional;

/** Railcraft's cart filters and redstone departure state machine, with integer essentia instead of fluid. */
public class EssentiaManipulator extends ManipulatorBlockEntity implements IEssentiaStorage,EssentiaTransport,EssentiaSource,RevealedAspectContainer {
    public final RailEssentiaTank tank=new RailEssentiaTank(256,this::changed);
    public EssentiaManipulator(BlockPos pos,BlockState state){super(ArcaneRail.MANIPULATOR.get(),pos,state);}
    public boolean loader(){return ((EssentiaManipulatorBlock)getBlockState().getBlock()).loader;}
    private boolean enabled(){return RevivalFeatures.Group.RAILCRAFT.enabled();}
    private void changed(){setChanged();if(level!=null&&!level.isClientSide){syncToClient();EssentiaStorageApi.changed(this);}}
    public static void serverTick(Level level,BlockPos pos,BlockState state,EssentiaManipulator block){if(block.enabled())ManipulatorBlockEntity.serverTick(level,pos,state,block);}
    @Override public Optional<AbstractMinecart> findCart(){
        if(!loader())return super.findCart();
        return level.getEntitiesOfClass(AbstractMinecart.class,new AABB(worldPosition.below()).expandTowards(0,-1,0).deflate(.1),cart->!cart.isRemoved()).stream().findFirst();
    }
    @Override protected void upkeep(){
        if(!(level instanceof ServerLevel server)||level.getGameTime()%5!=0)return;
        for(var side:Direction.values()){
            if(!connectable(side))continue;
            var pos=worldPosition.relative(side);if(!server.hasChunkAt(pos))continue;
            var other=EssentiaStorageApi.at(server,pos,side.getOpposite());
            if(loader()){
                for(var aspect:other.contents().aspects()){
                    int accepted=tank.insert(aspect,Math.min(8,other.contents().amount(aspect)),true);
                    if(accepted>0){tank.insert(aspect,other.extract(aspect,accepted,false),false);break;}
                }
                if(server.getBlockEntity(pos) instanceof EssentiaTransport transport&&transport.connectable(side.getOpposite())&&transport.canOutput(side.getOpposite())&&transport.suction(side.getOpposite())<128&&transport.minimumSuction()<=128){
                    var aspect=transport.essentiaType(side.getOpposite());if(aspect!=null&&tank.insert(aspect,1,true)>0)tank.insert(aspect,transport.takeEssentia(aspect,1,side.getOpposite()),false);
                }
            }else if(tank.aspect()!=null){
                var result=EssentiaStorageApi.transfer(tank,other,tank.aspect(),8);
                if(result.remainder()!=0)throw new IllegalStateException("Rail reservoir could not restore a rejected transfer");
            }
        }
    }
    @Override protected void processCart(AbstractMinecart cart){
        if(!(cart instanceof IEssentiaStorage storage))return;
        IEssentiaStorage source=loader()?tank:storage,destination=loader()?storage:tank;
        for(var aspect:source.contents().aspects()){
            var transfer=EssentiaStorageApi.transfer(source,destination,aspect,4);
            if(transfer.remainder()!=0)throw new IllegalStateException("Rail reservoir transfer rejected its remainder");
            if(transfer.inserted()>0){setProcessing(true);return;}
        }
    }
    @Override protected boolean hasWorkForCart(AbstractMinecart cart){
        if(!(cart instanceof IEssentiaStorage storage)||getRedstoneMode()==RedstoneMode.IMMEDIATE)return false;
        if(!loader())return !storage.contents().isEmpty();
        if(getRedstoneMode()==RedstoneMode.PARTIAL)return storage.contents().isEmpty()&&tank.amount()>0;
        var type=tank.aspect();if(type==null)type=storage.contents().aspects().stream().findFirst().orElse(null);
        return type!=null&&storage.insert(type,1,true)>0;
    }
    @Override public Component getDisplayName(){return Component.translatable(loader()?"block.thaumicomniverse.essentia_loader":"block.thaumicomniverse.essentia_unloader");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new EssentiaManipulatorMenu(id,inventory,this);}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.put("Essentia",tank.save());}
    @Override public void load(CompoundTag tag){super.load(tag);tank.load(tag.getCompound("Essentia"));}
    @Override public void writeToBuf(FriendlyByteBuf data){super.writeToBuf(data);data.writeNbt(tank.save());}
    @Override public void readFromBuf(FriendlyByteBuf data){super.readFromBuf(data);var tag=data.readNbt();if(tag!=null)tank.load(tag);}
    @Override public AspectAmounts revealedAspects(){return tank.contents();}
    @Override public AspectAmounts contents(){return tank.contents();}
    @Override public int capacity(Aspect aspect){return tank.capacity(aspect);}
    @Override public int insert(Aspect aspect,int count,boolean simulate){return enabled()?tank.insert(aspect,count,simulate):0;}
    @Override public int extract(Aspect aspect,int count,boolean simulate){return tank.extract(aspect,count,simulate);}
    @Override public boolean contains(Aspect aspect,int count){return enabled()&&count>=0&&tank.extract(aspect,count,true)==count;}
    @Override public boolean takeFromContainer(Aspect aspect,int count){if(!contains(aspect,count))return false;tank.extract(aspect,count,false);return true;}
    @Override public int availableAmount(Aspect aspect){return enabled()?tank.extract(aspect,Integer.MAX_VALUE,true):0;}
    @Override public boolean connectable(Direction face){return face!=getFacing();}
    @Override public boolean canInput(Direction face){return enabled()&&loader()&&connectable(face);}
    @Override public boolean canOutput(Direction face){return enabled()&&!loader()&&connectable(face);}
    @Override public Aspect suctionType(Direction face){return tank.aspect();}
    @Override public int suction(Direction face){return canInput(face)&&tank.amount()<tank.limit()?128:0;}
    @Override public int minimumSuction(){return 0;}
    @Override public Aspect essentiaType(Direction face){return tank.aspect();}
    @Override public int essentiaAmount(Direction face){return tank.amount();}
    @Override public int takeEssentia(Aspect aspect,int count,Direction face){return canOutput(face)?tank.extract(aspect,count,false):0;}
    @Override public int addEssentia(Aspect aspect,int count,Direction face){return canInput(face)?tank.insert(aspect,count,false):0;}
}
