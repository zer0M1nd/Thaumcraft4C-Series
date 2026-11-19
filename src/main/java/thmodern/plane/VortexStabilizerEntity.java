package thmodern.plane;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.content.AuraNodeBlockEntity;
import tc4revival.content.AuraNodeType;
import thaumcraft.api.nodes.NodeApi;
import thmodern.Horizons;
public final class VortexStabilizerEntity extends BlockEntity {
 private BlockEntity target;
 public VortexStabilizerEntity(BlockPos pos,BlockState state){super(Horizons.VORTEX_STABILIZER_ENTITY.get(),pos,state);}
 private ResourceLocation source(){return ResourceLocation.fromNamespaceAndPath(Horizons.ID,"stabilizer/"+level.dimension().location().getNamespace()+"/"+level.dimension().location().getPath()+"/"+Long.toUnsignedString(worldPosition.asLong()));}
 public void release(){if(level==null||level.isClientSide)return;if(target instanceof AuraNodeBlockEntity node&&!node.isRemoved()&&level.hasChunkAt(node.getBlockPos())&&level.getBlockEntity(node.getBlockPos())==node)NodeApi.removeInfluence(node,source());else if(target instanceof StabilizedRift rift)rift.removeStabilizer(source().toString());target=null;}
 @Override public void setRemoved(){release();super.setRemoved();}
 public void tick(){if(level==null||level.isClientSide||level.getGameTime()%5!=0)return;var direction=getBlockState().getValue(VortexStabilizerBlock.FACING);BlockEntity found=null;var end=worldPosition.relative(direction,10).getCenter();if(level.hasNeighborSignal(worldPosition))for(int distance=1;distance<=10;distance++){
  var pos=worldPosition.relative(direction,distance);if(!level.hasChunkAt(pos)){end=pos.getCenter();break;}var candidate=level.getBlockEntity(pos);if(candidate instanceof AuraNodeBlockEntity||candidate instanceof StabilizedRift){found=candidate;end=pos.getCenter();break;}if(!level.getBlockState(pos).getShape(level,pos).isEmpty()){end=pos.getCenter();break;}
 }
 if(found!=target){release();target=found;}
 if(target instanceof AuraNodeBlockEntity node)NodeApi.influence(node,source(),new NodeApi.Influence(AuraNodeType.NORMAL,false,null,1,1,100),12);else if(target instanceof StabilizedRift rift)rift.renewStabilizer(source().toString(),level.getGameTime());
 if(level.hasNeighborSignal(worldPosition)&&level instanceof net.minecraft.server.level.ServerLevel server)thaumcraft.api.fx.LegacyEffects.emit(level,"sonic",worldPosition.getCenter(),end,0x00ffff,1,12,1,0,0);
 }
}
