package automagy.content;

import automagy.api.*;
import automagy.logic.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import tc4revival.aspect.*;
import tc4revival.content.*;

public final class VisReaderBlockEntity extends InventoryBlockEntity implements MenuProvider,RemoteComparatorHost,AmplifiedSignal,AspectReadingProvider,RevealedAspectContainer {
    public final ReaderChannel[] channels={new ReaderChannel(1,0,6,false),new ReaderChannel(0,0,6,false),new ReaderChannel(0,0,6,false),new ReaderChannel(0,0,6,false)};
    private final int[] power=new int[6];
    private AspectReading observed=AspectReading.MISSING;
    public VisReaderBlockEntity(BlockPos pos,BlockState state){super(Content.VIS_READER_TILE.get(),pos,state,4);}
    @Override protected int slotLimit(int slot){return 1;}
    @Override protected boolean accepts(int slot,ItemStack stack){return stack.getItem() instanceof JarLabelItem label&&!label.storedAspects(stack).isEmpty();}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){
        return cap==ForgeCapabilities.ITEM_HANDLER?LazyOptional.empty():super.getCapability(cap,side);
    }
    public net.minecraftforge.items.IItemHandlerModifiable labels(){return inventory;}
    public AspectReading read(Set<BlockPos> visited){
        if(!(level instanceof ServerLevel server))return observed;
        BlockPos target=worldPosition.above();
        if(level.getBlockEntity(target) instanceof RemoteComparatorBlockEntity comparator){
            var linked=comparator.target();if(linked.isEmpty())return AspectReading.MISSING;target=linked.get();
        }
        return AspectReadings.at(server,target,visited);
    }
    @Override public AspectReading aspectReading(){return read(new HashSet<>(Set.of(worldPosition)));}
    @Override public AspectAmounts revealedAspects(){
        var result=new AspectAmounts();
        aspectReading().contents().forEach((key,n)->TCAspects.catalog().find(key).ifPresent(aspect->result.add(aspect,n)));
        return result;
    }
    @Override public int amplifiedSignal(Direction out){return out==Direction.UP?0:power[out.ordinal()];}
    @Override public boolean hasComparatorSignal(){return Arrays.stream(power).anyMatch(n->n>0);}
    public int channelSignal(int index){return channels[index].evaluate(observed,filter(index));}
    private String filter(int index){
        var item=stored(index);
        if(item.getItem() instanceof JarLabelItem label){var values=label.storedAspects(item);if(!values.isEmpty())return values.aspects().get(0).tag();}
        return "";
    }
    public static void tick(Level level,BlockPos pos,BlockState state,VisReaderBlockEntity tile){
        if(level.getGameTime()%5==Math.floorMod(pos.asLong(),5))tile.refresh();
    }
    public void refresh(){
        if(level==null||level.isClientSide)return;
        var next=aspectReading();boolean observationChanged=!next.equals(observed);observed=next;
        int[] outputs=new int[6];
        for(int i=0;i<4;i++){
            var channel=channels[i];int signal=channel.evaluate(observed,filter(i));
            if(channel.direction()==6){for(Direction dir:Direction.values())if(dir!=Direction.UP)outputs[dir.ordinal()]=Math.max(outputs[dir.ordinal()],signal);}
            else outputs[channel.direction()]=Math.max(outputs[channel.direction()],signal);
        }
        boolean signalsChanged=!Arrays.equals(outputs,power);System.arraycopy(outputs,0,power,0,6);
        if(observationChanged||signalsChanged)sync();
        if(signalsChanged){level.updateNeighborsAt(worldPosition,getBlockState().getBlock());
            for(Direction dir:Direction.values())level.updateNeighborsAt(worldPosition.relative(dir),getBlockState().getBlock());}
    }
    @Override protected void inventoryChanged(){super.inventoryChanged();if(channels!=null)refresh();}
    @Override public Component getDisplayName(){return Component.translatable("block.automagy.vis_reader");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new VisReaderMenu(id,inventory,this);}
    @Override protected void saveAdditional(CompoundTag tag){
        super.saveAdditional(tag);var configs=new ListTag();
        for(var channel:channels){var c=new CompoundTag();c.putInt("Mode",channel.mode());c.putInt("Capacity",channel.capacity());c.putInt("Direction",channel.direction());c.putBoolean("Inverse",channel.inverted());configs.add(c);}
        tag.put("Channels",configs);tag.putIntArray("Signals",power);
    }
    @Override public void load(CompoundTag tag){
        super.load(tag);var configs=tag.getList("Channels",Tag.TAG_COMPOUND);
        for(int i=0;i<Math.min(4,configs.size());i++){
            var c=configs.getCompound(i);int dir=c.getInt("Direction");if(dir<0||dir>6||dir==1)dir=6;
            channels[i]=new ReaderChannel(Math.max(0,Math.min(4,c.getInt("Mode"))),Math.max(0,Math.min(Integer.MAX_VALUE/2,c.getInt("Capacity"))),dir,c.getBoolean("Inverse"));
        }
        Arrays.fill(power,0);int[] signals=tag.getIntArray("Signals");for(int i=0;i<Math.min(6,signals.length);i++)power[i]=Math.max(0,Math.min(99,signals[i]));
        if(tag.contains("Observed")){
            var values=new TreeMap<String,Integer>();var source=tag.getCompound("Observed");
            for(String key:source.getAllKeys())if(TCAspects.catalog().find(key).isPresent())values.put(key,Math.max(0,source.getInt(key)));
            observed=new AspectReading(values,Map.of(),0,0,false,tag.getBoolean("ObservedValid"));
        }
    }
    @Override public CompoundTag getUpdateTag(){
        var tag=super.getUpdateTag();var values=new CompoundTag();observed.contents().forEach(values::putInt);
        tag.put("Observed",values);tag.putBoolean("ObservedValid",observed.valid());return tag;
    }
}
