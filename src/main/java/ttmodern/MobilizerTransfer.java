package ttmodern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Four-cell transaction: reconstruct the passenger before detaching inventories; notify only after commit. */
public final class MobilizerTransfer {
    public static final net.minecraft.tags.TagKey<Block> IMMOBILE=net.minecraft.tags.BlockTags.create(net.minecraft.resources.ResourceLocation.parse(Tinkerer.ID+":mobilizer_immovable"));
    private MobilizerTransfer() {}
    private record Snapshot(BlockState state,CompoundTag data) {
        static Snapshot read(ServerLevel world,BlockPos pos) { var entity=world.getBlockEntity(pos);return new Snapshot(world.getBlockState(pos),entity==null?null:entity.saveWithFullMetadata()); }
        Snapshot at(BlockPos pos) { if(data==null)return this;var nbt=data.copy();nbt.putInt("x",pos.getX());nbt.putInt("y",pos.getY());nbt.putInt("z",pos.getZ());return new Snapshot(state,nbt); }
        BlockEntity prepare(BlockPos pos) { if(data==null)return null;var nbt=data.copy();nbt.putInt("x",pos.getX());nbt.putInt("y",pos.getY());nbt.putInt("z",pos.getZ());var entity=BlockEntity.loadStatic(pos,state,nbt);if(entity==null||!entity.getType().isValid(state))throw new IllegalArgumentException("Unmovable block entity");return entity; }
    }
    public static boolean move(ServerLevel world,BlockPos from,BlockPos to,ServerPlayer actor) { return move(world,from,to,actor,(pos,state)->world.setBlock(pos,state,2|16)); }
    static boolean move(ServerLevel world,BlockPos from,BlockPos to,ServerPlayer actor,java.util.function.BiPredicate<BlockPos,BlockState> writer) {
        var positions=List.of(from,from.above(),to,to.above());if(from.getY()!=to.getY()||from.distManhattan(to)!=1)return false;
        for(var pos:positions)if(world.isOutsideBuildHeight(pos)||!world.hasChunkAt(pos)||!world.getWorldBorder().isWithinBounds(pos)||!world.mayInteract(actor,pos))return false;
        if(!world.getBlockState(from).is(Tinkerer.MOBILIZER.get())||!world.isEmptyBlock(to)||!world.isEmptyBlock(to.above()))return false;
        var passenger=world.getBlockState(from.above());
        if(passenger.getDestroySpeed(world,from.above())<0||passenger.is(IMMOBILE)||passenger.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)||passenger.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.BED_PART)||world.getBlockEntity(from.above()) instanceof tc4revival.content.AuraNodeBlockEntity)return false;
        var before=positions.stream().map(p->Snapshot.read(world,p)).toList();
        for(int i=0;i<2;i++)if(!before.get(i).state.isAir()&&net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(world,net.minecraft.world.level.GameType.SURVIVAL,actor,positions.get(i))<0)return false;
        for(int i=0;i<4;i++)if(!before.get(i).equals(Snapshot.read(world,positions.get(i))))return false;
        var after=List.of(before.get(2).at(from),before.get(3).at(from.above()),before.get(0).at(to),before.get(1).at(to.above()));var prepared=new ArrayList<BlockEntity>();
        try{for(int i=0;i<4;i++)prepared.add(after.get(i).prepare(positions.get(i)));}catch(RuntimeException invalid){return false;}
        var placed=List.of(net.minecraftforge.common.util.BlockSnapshot.create(world.dimension(),world,to),net.minecraftforge.common.util.BlockSnapshot.create(world.dimension(),world,to.above()));
        try {
            positions.forEach(world::removeBlockEntity);
            // Install destination support before placing the passenger; suppress intermediate neighbor changes.
            for(int i:new int[]{2,3,1,0})install(world,positions.get(i),after.get(i).state,prepared.get(i),writer);
            if(!passenger.isAir()&&!passenger.canSurvive(world,to.above()))throw new IllegalStateException("Passenger cannot survive destination");
            if(net.minecraftforge.event.ForgeEventFactory.onMultiBlockPlace(actor,placed,Direction.UP))throw new IllegalStateException("Protected destination");
            for(int i=0;i<4;i++)if(!after.get(i).equals(Snapshot.read(world,positions.get(i))))throw new IllegalStateException("Move hook changed payload");
        } catch(RuntimeException failure) {
            positions.forEach(world::removeBlockEntity);for(int i=0;i<4;i++)install(world,positions.get(i),before.get(i).state,before.get(i).prepare(positions.get(i)),(p,s)->world.setBlock(p,s,2|16));return false;
        }
        var box=net.minecraft.world.level.levelgen.structure.BoundingBox.fromCorners(from,from.above());var offset=to.subtract(from);
        world.getBlockTicks().copyArea(box,offset);world.getFluidTicks().copyArea(box,offset);world.getBlockTicks().clearArea(box);world.getFluidTicks().clearArea(box);
        for(int i=0;i<4;i++){var pos=positions.get(i);world.sendBlockUpdated(pos,before.get(i).state,after.get(i).state,3);world.updateNeighborsAt(pos,after.get(i).state.getBlock());world.updateNeighbourForOutputSignal(pos,after.get(i).state.getBlock());}
        return true;
    }
    private static void install(ServerLevel world,BlockPos pos,BlockState state,BlockEntity entity,java.util.function.BiPredicate<BlockPos,BlockState> writer) { if(!world.getBlockState(pos).equals(state)&&!writer.test(pos,state))throw new IllegalStateException("Move rejected");if(entity!=null){world.removeBlockEntity(pos);world.setBlockEntity(entity);entity.setChanged();} }
}
