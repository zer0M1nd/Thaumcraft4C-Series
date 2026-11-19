package ttmodern;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.*;
import tc4revival.content.PortableHoleFocusItem;

public final class DislocatorEntity extends BlockEntity implements TransvectorLink {
    private BlockPos target;
    private BlockState camouflage;
    private int cooldown;
    private boolean pending;
    public DislocatorEntity(BlockPos pos,BlockState state){super(Tinkerer.DISLOCATOR_ENTITY.get(),pos,state);}
    public BlockPos targetPosition(){return target;}
    public BlockState camouflage(){return camouflage;}
    public boolean link(BlockPos pos){if(level==null||!inRange(pos)||pos.equals(worldPosition)||level.isOutsideBuildHeight(pos)||!level.hasChunkAt(pos))return false;target=pos.immutable();changed();return true;}
    private boolean inRange(BlockPos pos){return Math.abs((long)pos.getX()-worldPosition.getX())<=16&&Math.abs((long)pos.getY()-worldPosition.getY())<=16&&Math.abs((long)pos.getZ()-worldPosition.getZ())<=16;}
    public void camouflage(BlockState state){camouflage=state;changed();if(level!=null)level.setBlock(worldPosition,getBlockState().setValue(DislocatorBlock.CAMOUFLAGED,state!=null),3);}
    private void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public void tick(){if(cooldown>0){cooldown--;setChanged();}if(cooldown==0&&pending){pending=false;receivePulse();}}
    public void receivePulse(){
        if(level==null||level.isClientSide||isRemoved()||target==null||!inRange(target)||!tc4revival.config.TCConfig.SERVER.allowMirrors.get())return;
        var local=worldPosition.relative(getBlockState().getValue(DislocatorBlock.FACING));
        if(target.equals(worldPosition)||target.equals(local)||!level.hasChunkAt(target)||!level.hasChunkAt(local)||level.isOutsideBuildHeight(local)||level.isOutsideBuildHeight(target))return;
        if(cooldown>0){pending=true;setChanged();return;}
        if(!allowed(level,local)||!allowed(level,target)){pending=false;setChanged();return;}
        // Set before callbacks: neighbour updates must not recursively replay this transaction.
        cooldown=10;setChanged();
        var localEntities=level.getEntities((Entity)null,new AABB(local),e->e.isAlive());
        var remoteEntities=level.getEntities((Entity)null,new AABB(target),e->e.isAlive());
        if(!exchange(level,local,target)){cooldown=0;pending=false;setChanged();return;}
        var moved=new java.util.HashSet<java.util.UUID>();
        var delta=Vec3.atLowerCornerOf(target.subtract(local));
        for(var entity:localEntities)if(moved.add(entity.getUUID()))move(entity,delta);
        for(var entity:remoteEntities)if(moved.add(entity.getUUID()))move(entity,delta.scale(-1));
    }
    private static void move(Entity entity,Vec3 delta){
        if(entity instanceof net.minecraft.server.level.ServerPlayer player)player.connection.teleport(entity.getX()+delta.x,entity.getY()+delta.y,entity.getZ()+delta.z,entity.getYRot(),entity.getXRot());
        else entity.teleportTo(entity.getX()+delta.x,entity.getY()+delta.y,entity.getZ()+delta.z);
    }
    private static boolean allowed(Level level,BlockPos pos){
        var state=level.getBlockState(pos);
        return state.getDestroySpeed(level,pos)>=0 && !state.is(PortableHoleFocusItem.BLACKLIST)
            && !tc4revival.config.TCConfig.SERVER.portableHoleBlacklist.get().contains(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString())
            && !(level.getBlockEntity(pos) instanceof tc4revival.content.AuraNodeBlockEntity)
            && !state.is(tc4revival.content.TCArcane.PORTABLE_HOLE.get());
    }
    private record Snapshot(BlockState state,CompoundTag data){
        static Snapshot read(Level level,BlockPos pos){var entity=level.getBlockEntity(pos);return new Snapshot(level.getBlockState(pos),entity==null?null:entity.saveWithFullMetadata());}
        BlockEntity prepare(BlockPos pos){
            if(data==null)return null;
            var saved=data.copy();saved.putInt("x",pos.getX());saved.putInt("y",pos.getY());saved.putInt("z",pos.getZ());
            var entity=BlockEntity.loadStatic(pos,state,saved);
            if(entity==null || !entity.getType().isValid(state))throw new IllegalStateException("Cannot reconstruct moved block entity at "+pos);
            return entity;
        }
    }
    /** Both payloads are reconstructed before mutation; removal cannot spill live inventories. */
    public static boolean exchange(Level level,BlockPos first,BlockPos second){
        return exchange(level,first,second,(pos,state)->level.setBlock(pos,state,2|16));
    }
    static boolean exchange(Level level,BlockPos first,BlockPos second,java.util.function.BiPredicate<BlockPos,BlockState> writer){
        if(level.isClientSide||level.isOutsideBuildHeight(first)||level.isOutsideBuildHeight(second)||first.equals(second)||!level.hasChunkAt(first)||!level.hasChunkAt(second)||!allowed(level,first)||!allowed(level,second))return false;
        var a=Snapshot.read(level,first);var b=Snapshot.read(level,second);
        BlockEntity toFirst,toSecond;
        try{toFirst=b.prepare(first);toSecond=a.prepare(second);}catch(RuntimeException invalid){return false;}
        try {
            level.removeBlockEntity(first);level.removeBlockEntity(second);
            install(level,first,b.state,toFirst,writer);install(level,second,a.state,toSecond,writer);
        }catch(RuntimeException failure){
            level.removeBlockEntity(first);level.removeBlockEntity(second);
            install(level,first,a.state,a.prepare(first));install(level,second,b.state,b.prepare(second));
            return false;
        }
        level.updateNeighborsAt(first,b.state.getBlock());level.updateNeighborsAt(second,a.state.getBlock());
        level.updateNeighbourForOutputSignal(first,b.state.getBlock());level.updateNeighbourForOutputSignal(second,a.state.getBlock());
        return true;
    }
    private static void install(Level level,BlockPos pos,BlockState state,BlockEntity entity){
        install(level,pos,state,entity,(at,value)->level.setBlock(at,value,2|16));
    }
    private static void install(Level level,BlockPos pos,BlockState state,BlockEntity entity,java.util.function.BiPredicate<BlockPos,BlockState> writer){
        if(!level.getBlockState(pos).equals(state)&&!writer.test(pos,state))throw new IllegalStateException("Rejected moved block state at "+pos);
        if(entity!=null){level.removeBlockEntity(pos);level.setBlockEntity(entity);entity.setChanged();}
        level.sendBlockUpdated(pos,state,state,3);
    }
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);if(target!=null)tag.putLong("Target",target.asLong());if(camouflage!=null)tag.put("Camouflage",NbtUtils.writeBlockState(camouflage));tag.putInt("Cooldown",cooldown);tag.putBoolean("Pending",pending);}
    @Override public void load(CompoundTag tag){super.load(tag);target=tag.contains("Target",Tag.TAG_LONG)?BlockPos.of(tag.getLong("Target")):null;camouflage=tag.contains("Camouflage")?NbtUtils.readBlockState(net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(),tag.getCompound("Camouflage")):null;cooldown=Math.max(0,Math.min(10,tag.getInt("Cooldown")));pending=tag.getBoolean("Pending");}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
