package ttmodern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class InfusedCropEntity extends BlockEntity implements tc4revival.content.RevealedAspectContainer {
    private String aspect="aer";
    private boolean fertilized;
    private final Map<String,Integer> traits=new LinkedHashMap<>();
    public InfusedCropEntity(BlockPos pos,BlockState state) { super(Tinkerer.INFUSED_CROP_ENTITY.get(),pos,state); }
    public String aspect() { return aspect; }
    public Map<String,Integer> traits() { return Collections.unmodifiableMap(traits); }
    public int trait(String key) { return traits.getOrDefault(key,0); }
    public void initialize(String next,Map<String,Integer> genes) { aspect=tc4revival.aspect.TCAspects.catalog().find(next).isPresent()?next:"aer";traits.clear();genes.forEach((key,value)->{if(CropTraits.PRIMALS.contains(key)&&value>0)traits.put(key,Math.min(50,value));});CropTraits.cap(traits,50,bound->0);if(level!=null&&getBlockState().hasProperty(InfusedCropBlock.FORM))level.setBlock(worldPosition,getBlockState().setValue(InfusedCropBlock.FORM,CropTraits.texture(aspect)),3);changed(); }
    private void changed() { setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3); }
    public void tick() {
        if(!(level instanceof ServerLevel server))return;
        var state=getBlockState();int age=state.getValue(InfusedCropBlock.AGE);
        if(age<7&&server.getRawBrightness(worldPosition.above(),0)>=9&&server.random.nextInt(CropTraits.growthBound(trait("aqua")))==0)server.setBlock(worldPosition,state.setValue(InfusedCropBlock.AGE,age+1),3);
        if(CropTraits.PRIMALS.contains(aspect)||server.random.nextInt(CropTraits.exchangeBound(trait("aer")))!=0)return;
        for(var direction:Direction.values()) {
            var pos=worldPosition.relative(direction);if(!server.hasChunkAt(pos)||!(server.getBlockEntity(pos) instanceof InfusedCropEntity donor))continue;
            if(CropTraits.PRIMALS.contains(donor.aspect)) { if(trait(donor.aspect)>=5)continue;traits.merge(donor.aspect,1,Integer::sum); }
            else { if(donor.traits.isEmpty())return;var keys=new ArrayList<>(donor.traits.keySet());String key=keys.get(server.random.nextInt(keys.size()));if(donor.trait(key)<trait(key))return;traits.merge(key,1,Integer::sum);donor.traits.computeIfPresent(key,(k,v)->v<=1?null:v-1);donor.changed(); }
            CropTraits.cap(traits,50,server.random::nextInt);changed();return;
        }
    }
    public String offspring(ServerLevel world) {
        if(!(world.getBlockEntity(worldPosition.below()) instanceof InfusedFarmlandEntity soil))return aspect;
        var catalog=tc4revival.aspect.TCAspects.catalog();
        for(var memory:soil.memory().entrySet())if(world.random.nextInt(10)<(trait("ignis")+1)*memory.getValue()*memory.getValue()) { var result=catalog.combine(catalog.require(aspect),catalog.require(memory.getKey()));if(result.isPresent())return result.get().tag(); }
        return aspect;
    }
    public void fertilize(ServerLevel world) {
        if(fertilized)return;fertilized=true;
        if(!(world.getBlockEntity(worldPosition.below()) instanceof InfusedFarmlandEntity soil))return;
        for(int count=0;count<64;count++){soil.fertilize(aspect);if(world.random.nextInt(55)>=trait("terra"))break;}
    }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag);tag.putString("Aspect",aspect);var values=new CompoundTag();traits.forEach(values::putInt);tag.put("Traits",values); }
    @Override public void load(CompoundTag tag) { super.load(tag);String value=tag.getString("Aspect");aspect=tc4revival.aspect.TCAspects.catalog().find(value).isPresent()?value:"aer";traits.clear();var genes=tag.getCompound("Traits");for(String key:CropTraits.PRIMALS)if(genes.getInt(key)>0)traits.put(key,Math.min(50,genes.getInt(key)));CropTraits.cap(traits,50,bound->0); }
    @Override public tc4revival.aspect.AspectAmounts revealedAspects() { return new tc4revival.aspect.AspectAmounts().add(tc4revival.aspect.TCAspects.catalog().require(aspect),1); }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
}
