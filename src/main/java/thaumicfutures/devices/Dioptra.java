package thaumicfutures.devices;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.*;
import tc4revival.content.ShardType;

public class Dioptra extends SyncedDevice {
    private ItemStack shard=ItemStack.EMPTY;
    private NodeSurvey survey;
    private List<NodeReading> nodes=List.of();
    private int surveyed;
    private ListTag lastSent=new ListTag();
    public Dioptra(BlockPos pos,BlockState state) { super(Devices.DIOPTRA_TILE.get(),pos,state); }
    public static ShardType shardType(ItemStack stack) {
        var id=ForgeRegistries.ITEMS.getKey(stack.getItem());
        for(var type:ShardType.values())if(type.id().equals(String.valueOf(id)))return type;
        return null;
    }
    public ItemStack shard() { return shard.copy(); }
    public List<NodeReading> nodes() { return nodes; }
    public int surveyedChunks() { return surveyed; }
    public boolean active() { return !shard.isEmpty() && !getBlockState().getValue(DioptraBlock.POWERED); }
    public Aspect filter() {
        var type=shardType(shard);if(type==null || type==ShardType.BALANCED)return null;
        return TCAspects.catalog().require(switch(type){case AIR->"aer";case FIRE->"ignis";case WATER->"aqua";case EARTH->"terra";case ORDER->"ordo";case ENTROPY->"perditio";default->throw new IllegalStateException();});
    }
    public ItemStack replaceShard(ItemStack replacement) {
        var old=shard;shard=replacement.copy();if(!shard.isEmpty())shard.setCount(1);
        nodes=List.of();lastSent=new ListTag();sync();return old;
    }
    public void tick() {
        if(!(level instanceof ServerLevel server))return;
        if(!active()) {
            if(!nodes.isEmpty()) { nodes=List.of();lastSent=new ListTag();sync();level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock()); }
            return;
        }
        if(survey==null)survey=new NodeSurvey();survey.tick(server,worldPosition);
        if(level.getGameTime()%20!=0)return;
        nodes=survey.nodes().stream().filter(n->n.contains(filter())).toList();
        var next=nodeTags();int count=survey.surveyedChunks();
        if(!next.equals(lastSent)||surveyed!=count) {
            lastSent=next;surveyed=count;sync();level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock());
        }
    }
    private ListTag nodeTags() { var tag=new ListTag();for(var node:nodes)tag.add(node.save());return tag; }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag);tag.put("Shard",shard.save(new CompoundTag())); }
    @Override public void load(CompoundTag tag) {
        super.load(tag);shard=ItemStack.of(tag.getCompound("Shard"));if(shardType(shard)==null)shard=ItemStack.EMPTY;else shard.setCount(1);
        if(tag.contains("Nodes")) { var list=new ArrayList<NodeReading>();for(var raw:tag.getList("Nodes",Tag.TAG_COMPOUND)) { var row=(CompoundTag)raw;list.add(NodeReading.read(row,row.getBoolean("Live"))); }nodes=List.copyOf(list);surveyed=tag.getInt("Surveyed"); }
    }
    @Override public CompoundTag getUpdateTag() { var tag=super.getUpdateTag();tag.put("Nodes",nodeTags());tag.putInt("Surveyed",surveyed);return tag; }
    @Override public AABB getRenderBoundingBox() { return new AABB(worldPosition).inflate(.5,2,.5); }
}
