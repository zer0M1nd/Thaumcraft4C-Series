package thmodern.plane;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class GatewayPortalEntity extends BlockEntity {
 private UUID link;private boolean inside;
 public GatewayPortalEntity(BlockPos pos,BlockState state){super(GatewayBlocks.PORTAL_ENTITY.get(),pos,state);}
 public UUID link(){return link;}public boolean inside(){return inside;}
 public void bind(UUID id,boolean inside){this.link=id;this.inside=inside;setChanged();}
 public void tick(){if(level!=null&&level.isClientSide){if(level.getGameTime()%250==0&&!level.getBlockState(worldPosition.below()).is(GatewayBlocks.PORTAL.get())){var side=getBlockState().getValue(GatewayPortalBlock.AXIS)==Direction.Axis.X?Direction.EAST:Direction.SOUTH;if(level.getBlockState(worldPosition.relative(side)).is(GatewayBlocks.PORTAL.get())&&level.getBlockState(worldPosition.relative(side.getOpposite())).is(GatewayBlocks.PORTAL.get()))level.playLocalSound(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,tc4revival.content.TCSounds.EVIL_PORTAL.get(),net.minecraft.sounds.SoundSource.BLOCKS,1,1,false);}return;}if(!(level instanceof ServerLevel server)||server.getGameTime()%20!=0)return;if(link==null||GatewayLinks.get(server.getServer()).get(link).isEmpty())server.setBlockAndUpdate(worldPosition,inside?PlanarBlocks.VOID.get().defaultBlockState():Blocks.AIR.defaultBlockState());}
 public void touch(ServerPlayer player){if(link!=null&&level instanceof ServerLevel server)GatewayRuntime.travel(server,player,link,inside);}
 @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);if(link!=null)tag.putUUID("Link",link);tag.putBoolean("Inside",inside);}
 @Override public void load(CompoundTag tag){super.load(tag);link=tag.hasUUID("Link")?tag.getUUID("Link"):null;inside=tag.getBoolean("Inside");}
}
