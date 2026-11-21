package wgmodern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.client.model.data.*;

/** Connected walls share one signal query per tick. Chunk unloading does not retain dead block entities. */
public class EtherealWall extends BaseEntityBlock {
    public static final BooleanProperty PASSABLE=BooleanProperty.create("passable");
    public EtherealWall(){super(Properties.copy(Blocks.STONE).strength(4,6).isValidSpawn((state,level,pos,type)->false).noOcclusion());registerDefaultState(stateDefinition.any().setValue(PASSABLE,false));}
    @Override public String getDescriptionId(){return "tile.WG_StoneDevice.etherealWall";}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(PASSABLE);}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return s.getValue(PASSABLE)?Shapes.empty():Shapes.block();}
    @Override public int getLightEmission(BlockState s,BlockGetter l,BlockPos p){return l.getBlockEntity(p) instanceof Tile tile&&tile.camouflage!=null?tile.camouflage.getLightEmission(l,p):0;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Tile(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return l.isClientSide?null:createTickerHelper(type,Content.ETHEREAL_TILE.get(),Tile::tick);}
    @Override public void neighborChanged(BlockState s,Level l,BlockPos p,Block b,BlockPos from,boolean moving){if(l.getBlockEntity(p) instanceof Tile t&&t.network!=null)t.network.valid=false;}
    @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!s.is(next.getBlock())&&l.getBlockEntity(p) instanceof Tile t&&t.network!=null)t.network.valid=false;super.onRemove(s,l,p,next,moving);}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){if(!(l.getBlockEntity(pos) instanceof Tile tile))return InteractionResult.PASS;var held=p.getItemInHand(hand);BlockState camo=null;if(held.getItem() instanceof BlockItem item){if(p.isShiftKeyDown()||item.getBlock() instanceof EtherealWall)return InteractionResult.PASS;camo=item.getBlock().defaultBlockState();if(camo.getRenderShape()!=RenderShape.MODEL||!Block.isShapeFullBlock(camo.getShape(l,pos)))return InteractionResult.PASS;if(camo.hasProperty(BlockStateProperties.AXIS))camo=camo.setValue(BlockStateProperties.AXIS,hit.getDirection().getAxis());}else if(!p.isShiftKeyDown())return InteractionResult.PASS;if(!l.isClientSide)tile.setCamouflage(camo);return InteractionResult.sidedSuccess(l.isClientSide);}
    public static class Network {
        public boolean valid=true;private long checked=Long.MIN_VALUE;private final Set<BlockPos> members=new HashSet<>();
        public void update(Level level){if(checked==level.getGameTime())return;checked=level.getGameTime();boolean powered=false;for(var p:members)if(level.hasChunkAt(p)&&level.hasNeighborSignal(p)){powered=true;break;}for(var p:members)if(level.hasChunkAt(p)){var s=level.getBlockState(p);if(s.getBlock() instanceof EtherealWall&&s.getValue(PASSABLE)!=powered)level.setBlock(p,s.setValue(PASSABLE,powered),2);}}
    }
    public static class Tile extends BlockEntity {
        public static final ModelProperty<BlockState> CAMOUFLAGE=new ModelProperty<>();
        public BlockState camouflage;private Network network;
        public Tile(BlockPos p,BlockState s){super(Content.ETHEREAL_TILE.get(),p,s);}
        public static void tick(Level l,BlockPos p,BlockState s,Tile t){if(t.network==null||!t.network.valid)t.rebuild();t.network.update(l);}
        private void rebuild(){var next=new Network();var queue=new ArrayDeque<BlockPos>();queue.add(worldPosition);while(!queue.isEmpty()){var p=queue.removeFirst();if(!level.hasChunkAt(p)||next.members.contains(p)||!(level.getBlockEntity(p) instanceof Tile tile))continue;next.members.add(p);tile.network=next;for(var d:Direction.values())queue.add(p.relative(d));}network=next;}
        @Override public void onLoad(){super.onLoad();if(level!=null)for(var d:Direction.values())if(level.hasChunkAt(worldPosition.relative(d))&&level.getBlockEntity(worldPosition.relative(d)) instanceof Tile tile&&tile.network!=null)tile.network.valid=false;}
        @Override public void setRemoved(){if(network!=null)network.valid=false;super.setRemoved();}
        public void setCamouflage(BlockState state){camouflage=state;setChanged();requestModelDataUpdate();if(level!=null){level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);level.getChunkSource().getLightEngine().checkBlock(worldPosition);}}
        @Override public ModelData getModelData(){return camouflage==null?ModelData.EMPTY:ModelData.builder().with(CAMOUFLAGE,camouflage).build();}
        @Override public void load(CompoundTag tag){super.load(tag);camouflage=tag.contains("camouflage")?NbtUtils.readBlockState(net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(),tag.getCompound("camouflage")):null;if(camouflage!=null&&(camouflage.getBlock() instanceof EtherealWall||camouflage.isAir()))camouflage=null;requestModelDataUpdate();}
        @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);if(camouflage!=null)tag.put("camouflage",NbtUtils.writeBlockState(camouflage));}
        @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
        @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    }
}
