package thmodern.plane;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thmodern.Horizons;
public final class KeystoneSlotEntity extends BlockEntity {
 private ItemStack key=ItemStack.EMPTY;private UUID link;private boolean xAligned;
 public KeystoneSlotEntity(BlockPos pos,BlockState state){super(GatewayBlocks.SLOT_ENTITY.get(),pos,state);}
 public ItemStack key(){return key.copy();}public UUID link(){return link;}
 public boolean xAligned(){return xAligned;}
 void connected(UUID id,boolean xAligned){link=id;this.xAligned=xAligned;sync();}
 private void sync(){setChanged();if(level!=null&&!level.isClientSide&&getBlockState().getValue(KeystoneSlotBlock.HAS_KEY)!=!key.isEmpty())level.setBlock(worldPosition,getBlockState().setValue(KeystoneSlotBlock.HAS_KEY,!key.isEmpty()),3);if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
 public boolean interact(Player player,InteractionHand hand){if(!(level instanceof ServerLevel server))return false;var held=player.getItemInHand(hand);if(!key.isEmpty()&&held.isEmpty()){close();player.setItemInHand(hand,key.copy());key=ItemStack.EMPTY;sync();return true;}if(key.isEmpty()&&held.is(Horizons.KEYSTONE.get())&&KeystoneItem.resolve(held,server).isPresent()){key=held.copyWithCount(1);held.shrink(1);sync();return true;}return false;}
 public boolean open(Player player,ItemStack wand){return level instanceof ServerLevel server&&link==null&&!key.isEmpty()&&GatewayRuntime.open(server,this,player,wand);}
 public void close(){if(level instanceof ServerLevel server&&link!=null){GatewayRuntime.close(server.getServer(),link);link=null;sync();}}
 public void tick(){if(!(level instanceof ServerLevel server)||link==null||server.getGameTime()%10!=0)return;var binding=GatewayLinks.get(server.getServer()).get(link).orElse(null);if(binding==null){link=null;sync();return;}if(GatewayRuntime.loadedFrame(server,binding)&&!GatewayRuntime.validOpenFrame(server,binding))close();}
 public void breakSlot(){if(!(level instanceof ServerLevel server))return;close();if(!key.isEmpty()){PlaneRefunds.get(server.getServer()).enqueue(UUID.randomUUID(),server.dimension().location(),worldPosition,key);key=ItemStack.EMPTY;sync();}}
 @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putBoolean("XAligned",xAligned);tag.put("Keystone",key.save(new CompoundTag()));if(link!=null)tag.putUUID("Link",link);}
 @Override public void load(CompoundTag tag){super.load(tag);xAligned=tag.getBoolean("XAligned");key=ItemStack.of(tag.getCompound("Keystone"));link=tag.hasUUID("Link")?tag.getUUID("Link"):null;}
 @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(){return new net.minecraft.world.phys.AABB(worldPosition.offset(-2,-4,-2),worldPosition.offset(3,1,3));}
 @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
 @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
