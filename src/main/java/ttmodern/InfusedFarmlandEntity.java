package ttmodern;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class InfusedFarmlandEntity extends BlockEntity implements tc4revival.content.RevealedAspectContainer {
    private final Map<String,Integer> memory=new LinkedHashMap<>();
    public InfusedFarmlandEntity(BlockPos pos,BlockState state) { super(Tinkerer.INFUSED_FARMLAND_ENTITY.get(),pos,state); }
    public Map<String,Integer> memory() { return Collections.unmodifiableMap(memory); }
    public void fertilize(String aspect) { memory.merge(aspect,1,Integer::sum);CropTraits.cap(memory,20,level.random::nextInt);setChanged();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3); }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag);var values=new CompoundTag();memory.forEach(values::putInt);tag.put("Memory",values); }
    @Override public void load(CompoundTag tag) { super.load(tag);memory.clear();var values=tag.getCompound("Memory");for(String key:values.getAllKeys())if(tc4revival.aspect.TCAspects.catalog().find(key).isPresent()&&values.getInt(key)>0)memory.put(key,Math.min(20,values.getInt(key)));CropTraits.cap(memory,20,bound->0); }
    @Override public tc4revival.aspect.AspectAmounts revealedAspects() { var result=new tc4revival.aspect.AspectAmounts();memory.forEach((key,value)->result.add(tc4revival.aspect.TCAspects.catalog().require(key),value));return result; }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
}
