package wtmodern;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.Container;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/** Every vanished block owns its recoverable snapshot, including containers and unloaded chunks. */
public class VanishingBlock extends BaseEntityBlock {
    public VanishingBlock(){super(Properties.copy(Blocks.BARRIER).noCollission().noOcclusion().noLootTable().isViewBlocking((s,l,p)->false).isSuffocating((s,l,p)->false));}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new Snapshot(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,Content.SNAPSHOT.get(),Snapshot::tick);}
    public static boolean hide(ServerLevel level,BlockPos pos,long until){
        var state=level.getBlockState(pos);if(state.isAir()||state.is(Content.VANISH.get()))return false;
        var old=level.getBlockEntity(pos);var nbt=old==null?null:old.saveWithFullMetadata();
        // Prevent onRemove dropping a container's contents; the saved snapshot remains the sole owner.
        if(old instanceof Container c)c.clearContent();
        level.removeBlockEntity(pos);level.setBlock(pos,Content.VANISH.get().defaultBlockState(),2|16);
        if(level.getBlockEntity(pos) instanceof Snapshot s){s.original=state;s.tile=nbt;s.until=until;s.setChanged();return true;}
        level.setBlock(pos,state,3);if(nbt!=null&&level.getBlockEntity(pos)!=null)level.getBlockEntity(pos).load(nbt);
        return false;
    }
    public static class Snapshot extends BlockEntity {
        private BlockState original=Blocks.AIR.defaultBlockState();private CompoundTag tile;private long until;
        public Snapshot(BlockPos pos,BlockState state){super(Content.SNAPSHOT.get(),pos,state);}
        public static void tick(Level level,BlockPos pos,BlockState state,Snapshot snapshot){if(level.getGameTime()>=snapshot.until)snapshot.restore();}
        public void restore(){if(level==null)return;level.setBlock(worldPosition,original,2|16);if(tile!=null&&level.getBlockEntity(worldPosition)!=null){level.getBlockEntity(worldPosition).load(tile);level.getBlockEntity(worldPosition).setChanged();}level.sendBlockUpdated(worldPosition,Content.VANISH.get().defaultBlockState(),original,3);}
        @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.put("original",NbtUtils.writeBlockState(original));if(tile!=null)tag.put("tile",tile);tag.putLong("until",until);}
        @Override public void load(CompoundTag tag){super.load(tag);original=NbtUtils.readBlockState(net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(),tag.getCompound("original"));tile=tag.contains("tile",Tag.TAG_COMPOUND)?tag.getCompound("tile"):null;until=tag.getLong("until");}
    }
}
