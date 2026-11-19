package automagy.content;

import automagy.api.*;
import automagy.logic.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.*;

public final class EssentiaLocusBlockEntity extends BlockEntity implements AspectReadingProvider,tc4revival.content.RevealedAspectContainer,thaumcraft.api.aspects.IAspectSource {
    private final PreferredSource<BlockPos,Aspect> preferred=new PreferredSource<>();
    private boolean canSupply(){
        return level instanceof ServerLevel server&&server.getServer().isSameThread()&&valid&&capacity.loaded()
            &&level.hasChunkAt(worldPosition.below())&&level.getBlockState(worldPosition.below()).is(Content.LOCUS_AGGREGATOR.get())
            &&EssentiaLocusStructure.validate(new EssentiaLocusEnvironment(level,worldPosition),false)==EssentiaLocusStructure.Validity.VALID;
    }
    @Override public boolean contains(Aspect aspect,int amount){
        if(aspect==null||amount<0||!canSupply())return false;
        return EssentiaLocusContainers.observe((ServerLevel)level,jars.stream().filter(pos->level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof tc4revival.content.EssentiaSource).toList()).contents().getOrDefault(aspect.tag(),0)>=amount;
    }
    @Override public java.util.List<? extends tc4revival.content.EssentiaSource> availabilitySources(){
        if(!canSupply())return java.util.List.of();
        return jars.stream().filter(level::hasChunkAt).map(level::getBlockEntity)
                .filter(tc4revival.content.EssentiaSource.class::isInstance).map(tc4revival.content.EssentiaSource.class::cast).toList();
    }
    @Override public boolean takeFromContainer(Aspect aspect,int amount){
        if(aspect==null||amount!=1||!canSupply())return false;
        var source=preferred.take(aspect,jars,(pos,a)->{
            if(!EssentiaLocusContainers.readable((ServerLevel)level,pos))return false;
            var tile=level.getBlockEntity(pos);
            return tile instanceof tc4revival.content.EssentiaSource reservoir&&reservoir.takeFromContainer(a,1);
        });
        if(source.isEmpty())return false;
        var pos=source.get();if(level.getBlockEntity(pos)!=null){level.getBlockEntity(pos).setChanged();thaumcraft.api.aspects.EssentiaStorageApi.changed(level.getBlockEntity(pos));}
        observed=EssentiaLocusContainers.observe((ServerLevel)level,jars);sync();
        thaumcraft.api.fx.LegacyEffects.emit(level,"sparkle_path",worldPosition.getCenter().add(0,-1.7,0),worldPosition.getCenter().add(0,1,0),0x3388ee,1,21,2,.25f,0);
        return true;
    }
    private final List<ItemStack> eyes=new ArrayList<>();
    private List<BlockPos> jars=List.of();
    private EssentiaLocusStructure.Capacity capacity=new EssentiaLocusStructure.Capacity(12,1,20,false);
    private AspectReading observed=AspectReading.MISSING;
    private int validity,scan,scanInterval=40;
    private boolean valid;
    public EssentiaLocusBlockEntity(BlockPos pos,BlockState state){super(Content.ESSENTIA_LOCUS_TILE.get(),pos,state);}
    private Optional<GlobalPos> target(ItemStack eye){
        if(level==null||!(eye.getItem() instanceof LocationLink link))return Optional.empty();
        return link.linkedPosition(eye).filter(p->p.dimension().equals(level.dimension())&&!p.pos().equals(worldPosition)&&p.pos().distSqr(worldPosition)<=(double)capacity.distance()*capacity.distance());
    }
    public boolean insert(ItemStack eye){
        refreshCapacity();if(!valid||!capacity.loaded()||eyes.size()>=capacity.eyes()||target(eye).isEmpty())return false;
        eyes.add(eye.copyWithCount(1));scan=0;sync();return true;
    }
    public ItemStack remove(){if(eyes.isEmpty())return ItemStack.EMPTY;var result=eyes.remove(eyes.size()-1);scan=0;sync();return result;}
    public void dropEyes(){if(level!=null&&!level.isClientSide){for(var eye:eyes)Block.popResource(level,worldPosition,eye);eyes.clear();}}
    private void refreshCapacity(){
        if(level==null||level.isClientSide)return;
        var environment=new EssentiaLocusEnvironment(level,worldPosition);
        valid=EssentiaLocusStructure.validate(environment,false)==EssentiaLocusStructure.Validity.VALID;
        var next=EssentiaLocusStructure.capacity(environment);if(!next.loaded()){capacity=next;return;}
        if(!next.equals(capacity))scan=0;capacity=next;
        if(!valid)return;
        for(int i=eyes.size()-1;i>=0;i--)if(i>=capacity.eyes()||target(eyes.get(i)).isEmpty()){Block.popResource(level,worldPosition,eyes.remove(i));scan=0;sync();}
    }
    @Override public AspectReading aspectReading(){return valid&&capacity.loaded()?observed:AspectReading.MISSING;}
    @Override public AspectAmounts revealedAspects(){var values=new AspectAmounts();aspectReading().contents().forEach((key,n)->TCAspects.catalog().find(key).ifPresent(a->values.add(a,n)));return values;}
    public static void tick(Level level,BlockPos pos,BlockState state,EssentiaLocusBlockEntity tile){
        if(!(level instanceof ServerLevel server))return;
        if(tile.validity--<=0){
            tile.validity=60;var result=EssentiaLocusStructure.validate(new EssentiaLocusEnvironment(level,pos),false);
            if(result==EssentiaLocusStructure.Validity.INVALID){level.destroyBlock(pos,true);return;}
            tile.refreshCapacity();
        }
        if(!tile.valid||!tile.capacity.loaded())return;
        if(level.random.nextInt(1000)==0)TCAspects.catalog().find("aqua").ifPresent(aspect->level.addFreshEntity(new tc4revival.content.AspectOrbEntity(level,pos.getX()+.5,pos.getY()-.5,pos.getZ()+.5,aspect,1)));
        if(tile.scan--<=0){
            var seeds=tile.eyes.stream().map(tile::target).flatMap(Optional::stream).toList();
            var found=EssentiaLocusContainers.discover(server,pos,seeds,tile.capacity.distance(),tile.capacity.jars());
            tile.scanInterval=found.equals(tile.jars)?Math.min(200,tile.scanInterval+20):20;tile.jars=found;tile.scan=tile.scanInterval;
        }
        if(level.getGameTime()%2==0){var next=EssentiaLocusContainers.observe(server,tile.jars);if(!next.equals(tile.observed)){tile.observed=next;tile.sync();}}
    }
    private void sync(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);var list=new ListTag();for(var eye:eyes)list.add(eye.save(new CompoundTag()));tag.put("Eyes",list);}
    @Override public void load(CompoundTag tag){
        super.load(tag);eyes.clear();var list=tag.getList("Eyes",Tag.TAG_COMPOUND);for(int i=0;i<Math.min(40,list.size());i++){var eye=ItemStack.of(list.getCompound(i));if(eye.getItem() instanceof LocationLink)eyes.add(eye.copyWithCount(1));}
        if(tag.contains("Observation")){var values=new TreeMap<String,Integer>();var caps=new TreeMap<String,Integer>();var data=tag.getCompound("Observation");for(var key:data.getAllKeys())values.put(key,Math.max(0,data.getInt(key)));var maximum=tag.getCompound("Capacities");for(var key:maximum.getAllKeys())caps.put(key,Math.max(0,maximum.getInt(key)));observed=new AspectReading(values,caps,caps.values().stream().mapToLong(Integer::longValue).sum(),0,true,true);valid=tag.getBoolean("Valid");capacity=new EssentiaLocusStructure.Capacity(tag.getInt("Jars"),tag.getInt("MaxEyes"),tag.getInt("Distance"),valid);}
    }
    @Override public CompoundTag getUpdateTag(){var tag=saveWithoutMetadata();var contents=new CompoundTag();observed.contents().forEach(contents::putInt);tag.put("Observation",contents);var caps=new CompoundTag();observed.capacities().forEach(caps::putInt);tag.put("Capacities",caps);tag.putBoolean("Valid",valid&&capacity.loaded());tag.putInt("Jars",capacity.jars());tag.putInt("MaxEyes",capacity.eyes());tag.putInt("Distance",capacity.distance());return tag;}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
