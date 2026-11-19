package ttmodern;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class MobilizerEntity extends BlockEntity {
    private BlockPos first,second;
    private Direction direction=Direction.EAST;
    private java.util.UUID owner;
    public MobilizerEntity(BlockPos pos,BlockState state) { super(Tinkerer.MOBILIZER_ENTITY.get(),pos,state); }
    public Direction direction() { return direction; }
    public void owner(java.util.UUID value) { owner=value;setChanged(); }
    public void link(BlockPos a,BlockPos b) {
        if(valid())return;if(!MobilizerRelayEntity.validTrack(a,b)||!between(worldPosition,a,b))return;
        first=a.immutable();second=b.immutable();direction=a.getX()!=b.getX()?Direction.EAST:Direction.NORTH;setChanged();
    }
    private static boolean between(BlockPos pos,BlockPos a,BlockPos b) { return pos.getY()==a.getY()&&pos.distManhattan(a)+pos.distManhattan(b)==a.distManhattan(b)&&!pos.equals(a)&&!pos.equals(b); }
    private boolean valid() {
        return level!=null&&first!=null&&second!=null&&MobilizerRelayEntity.validTrack(first,second)&&between(worldPosition,first,second)&&level.hasChunkAt(first)&&level.hasChunkAt(second)&&level.getBlockEntity(first) instanceof MobilizerRelayEntity a&&a.verify()&&second.equals(a.partner());
    }
    private boolean space(BlockPos pos) { return level.hasChunkAt(pos)&&level.getWorldBorder().isWithinBounds(pos)&&between(pos,first,second)&&level.isEmptyBlock(pos)&&level.isEmptyBlock(pos.above()); }
    public void tick() {
        if(!(level instanceof ServerLevel world)||isRemoved()||world.getBlockEntity(worldPosition)!=this||!valid()||world.hasNeighborSignal(worldPosition))return;
        if(world.getGameTime()%100==0&&!space(worldPosition.relative(direction))){direction=direction.getOpposite();setChanged();}
        if(world.getGameTime()%100==1)move();
    }
    public boolean move() {
        if(!(level instanceof ServerLevel world)||isRemoved()||!valid()||world.hasNeighborSignal(worldPosition))return false;
        var target=worldPosition.relative(direction);if(!space(target))return false;
        var profile=new com.mojang.authlib.GameProfile(owner==null?java.util.UUID.fromString("29d3dcba-404f-4dd3-af16-f5e718a3a803"):owner,"TTMobilizer");
        return MobilizerTransfer.move(world,worldPosition,target,net.minecraftforge.common.util.FakePlayerFactory.get(world,profile));
    }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag);if(first!=null)tag.putLong("First",first.asLong());if(second!=null)tag.putLong("Second",second.asLong());tag.putString("Direction",direction.getName());if(owner!=null)tag.putUUID("Owner",owner); }
    @Override public void load(CompoundTag tag) { super.load(tag);first=tag.contains("First",Tag.TAG_LONG)?BlockPos.of(tag.getLong("First")):null;second=tag.contains("Second",Tag.TAG_LONG)?BlockPos.of(tag.getLong("Second")):null;var read=Direction.byName(tag.getString("Direction"));direction=read!=null&&read.getAxis().isHorizontal()?read:Direction.EAST;owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null; }
}
