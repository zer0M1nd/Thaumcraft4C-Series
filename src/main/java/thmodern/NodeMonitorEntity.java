package thmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.content.ScannableNode;

public final class NodeMonitorEntity extends BlockEntity {
 public float previousRotation,rotation;
 public NodeMonitorEntity(BlockPos pos,BlockState state){super(Horizons.NODE_MONITOR_ENTITY.get(),pos,state);}
 public void tick(){
  if(level==null)return;
  if(level.isClientSide){previousRotation=rotation;if(!getBlockState().getValue(NodeMonitorBlock.ACTIVE))rotation+=1;return;}
  var state=getBlockState();var target=worldPosition.relative(state.getValue(NodeMonitorBlock.FACING).getOpposite());
  if(!level.hasChunkAt(target))return; // Unloaded support is not a removed node.
  if(!(level.getBlockEntity(target) instanceof ScannableNode node)){level.destroyBlock(worldPosition,true);return;}
  boolean critical=node.aspects().view().values().stream().anyMatch(n->n<=1);
  if(critical!=state.getValue(NodeMonitorBlock.ACTIVE)){level.setBlock(worldPosition,state.setValue(NodeMonitorBlock.ACTIVE,critical),3);level.updateNeighborsAt(worldPosition,state.getBlock());setChanged();}
 }
}
