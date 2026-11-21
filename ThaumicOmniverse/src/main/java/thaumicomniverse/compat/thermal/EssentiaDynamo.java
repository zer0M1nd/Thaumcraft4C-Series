package thaumicomniverse.compat.thermal;

import cofh.thermal.lib.common.block.entity.DynamoBlockEntity;
import cofh.thermal.core.common.config.ThermalCoreConfig;
import cofh.thermal.core.util.managers.dynamo.StirlingFuelManager;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.*;
import tc4revival.content.*;
import thaumcraft.api.aspects.IEssentiaStorage;
import thaumicomniverse.expansion.EssentiaFuelCatalog;

/** Native Thermal generator: its normal augment, redstone and output contracts stay in charge. */
public class EssentiaDynamo extends DynamoBlockEntity implements IEssentiaStorage,EssentiaTransport,RevealedAspectContainer {
    private AspectAmounts essentia=new AspectAmounts();
    private int ticks;
    public EssentiaDynamo(BlockPos pos,BlockState state){
        super(ThermalContent.ENTITY.get(),pos,state);
        addAugmentSlots(ThermalCoreConfig.dynamoAugments);initHandlers();
    }
    private boolean enabled(){return thaumicomniverse.RevivalFeatures.Group.THERMAL.enabled();}
    @Override protected int getBaseProcessTick(){return StirlingFuelManager.instance().getBasePower();}
    private Aspect nextFuel(){return essentia.alphabetical().stream().filter(a->EssentiaFuelCatalog.dynamoEnergy(a.tag())>0).findFirst().orElse(null);}
    @Override protected boolean canProcessStart(){return enabled()&&nextFuel()!=null;}
    @Override protected void processStart(){
        var aspect=nextFuel();if(aspect==null)return;
        fuelMax=(int)Math.max(1,Math.min(Integer.MAX_VALUE,Math.round(EssentiaFuelCatalog.dynamoEnergy(aspect.tag())*(double)getEnergyMod())));
        fuel=fuelMax;processTick=Math.min(baseProcessTick,fuelMax);extract(aspect,1,false);
    }
    @Override protected int calcEnergy(){return Math.min(fuel,super.calcEnergy());}
    @Override public void tickServer(){
        if(!enabled()){transferRF();return;}
        super.tickServer();
        if(++ticks%5!=0||essentia.total()>=64)return;
        for(var side:Direction.values()){
            if(side==getFacing()||!level.hasChunkAt(worldPosition.relative(side)))continue;
            if(!(level.getBlockEntity(worldPosition.relative(side)) instanceof EssentiaTransport source)||!source.canOutput(side.getOpposite()))continue;
            var aspect=source.essentiaType(side.getOpposite());
            if(aspect!=null&&source.suction(side.getOpposite())<64&&source.minimumSuction()<=64&&insert(aspect,1,true)>0){
                int taken=source.takeEssentia(aspect,1,side.getOpposite());if(taken>0){insert(aspect,taken,false);break;}
            }
        }
    }
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new EssentiaDynamoMenu(id,level,worldPosition,inventory,player);}
    @Override public AspectAmounts contents(){return essentia.copy();}
    @Override public AspectAmounts revealedAspects(){return contents();}
    @Override public int totalCapacity(){return 64;}
    @Override public int capacity(Aspect aspect){return 64-(int)essentia.total()+essentia.amount(aspect);}
    private void changedEssentia(){setChanged();thaumcraft.api.aspects.EssentiaStorageApi.changed(this);if(level!=null&&!level.isClientSide)cofh.core.common.network.packet.client.TileStatePacket.sendToClient(this);}
    @Override public int insert(Aspect aspect,int amount,boolean simulate){
        if(!enabled()||aspect==null||amount<=0)return 0;
        int accepted=Math.min(amount,Math.max(0,64-(int)essentia.total()));
        if(accepted>0&&!simulate){essentia.add(aspect,accepted);changedEssentia();}
        return accepted;
    }
    @Override public int extract(Aspect aspect,int amount,boolean simulate){
        int taken=aspect==null?0:Math.min(Math.max(0,amount),essentia.amount(aspect));
        if(taken>0&&!simulate){essentia.reduce(aspect,taken);if(essentia.amount(aspect)==0)essentia.remove(aspect);changedEssentia();}
        return taken;
    }
    @Override public boolean connectable(Direction side){return side!=getFacing();}
    @Override public boolean canInput(Direction side){return connectable(side);}
    @Override public boolean canOutput(Direction side){return connectable(side);}
    @Override public Aspect suctionType(Direction side){return null;}
    @Override public int suction(Direction side){return enabled()&&connectable(side)&&essentia.total()<64?64:0;}
    @Override public int minimumSuction(){return 0;}
    @Override public Aspect essentiaType(Direction side){return essentia.isEmpty()?null:essentia.alphabetical().get(0);}
    @Override public int essentiaAmount(Direction side){var a=essentiaType(side);return a==null?0:essentia.amount(a);}
    @Override public int takeEssentia(Aspect a,int n,Direction side){return canOutput(side)?extract(a,n,false):0;}
    @Override public int addEssentia(Aspect a,int n,Direction side){return canInput(side)?insert(a,n,false):0;}
    @Override public void saveAdditional(CompoundTag tag){super.saveAdditional(tag);var stored=new CompoundTag();AspectNbt.write(stored,essentia);tag.put("Essentia",stored);}
    @Override public void load(CompoundTag tag){super.load(tag);readEssentia(tag.getCompound("Essentia"));}
    private void readEssentia(CompoundTag tag){
        var raw=AspectNbt.read(tag,TCAspects.catalog()).amounts();essentia=new AspectAmounts();int remaining=64;
        for(var aspect:raw.alphabetical()){int n=Math.min(remaining,Math.max(0,raw.amount(aspect)));if(n>0){essentia.add(aspect,n);remaining-=n;}}
    }
    @Override public FriendlyByteBuf getGuiPacket(FriendlyByteBuf buffer){super.getGuiPacket(buffer);var tag=new CompoundTag();AspectNbt.write(tag,essentia);buffer.writeNbt(tag);return buffer;}
    @Override public void handleGuiPacket(FriendlyByteBuf buffer){super.handleGuiPacket(buffer);var tag=buffer.readNbt();if(tag!=null)readEssentia(tag);}
    @Override public FriendlyByteBuf getStatePacket(FriendlyByteBuf buffer){super.getStatePacket(buffer);var tag=new CompoundTag();AspectNbt.write(tag,essentia);buffer.writeNbt(tag);return buffer;}
    @Override public void handleStatePacket(FriendlyByteBuf buffer){super.handleStatePacket(buffer);var tag=buffer.readNbt();if(tag!=null)readEssentia(tag);}
}
