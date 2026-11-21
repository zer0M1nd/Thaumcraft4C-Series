package thaumicomniverse.compat.create.airship;

import java.util.UUID;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

public final class AirshipBlocks {
    private static final java.util.Map<NodeTile,Boolean> loadedNodes=new java.util.WeakHashMap<>();
    private AirshipBlocks(){}
    public static final class CoreBlock extends HorizontalDirectionalBlock {
        public CoreBlock(){super(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3,6).noOcclusion());registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
        @Override public BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
        @Override public BlockState mirror(BlockState s,Mirror m){return s.rotate(m.getRotation(s.getValue(FACING)));}
    }
    public static final class CasingBlock extends Block {
        public CasingBlock(){super(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3,6));}
        @Override public void onPlace(BlockState s,Level level,BlockPos pos,BlockState old,boolean moving){
            super.onPlace(s,level,pos,old,moving);
            if(level instanceof ServerLevel sl)for(var node:FlightData.get(sl.getServer()).graph.nodes())
                if(node.station()&&node.dimension().equals(sl.dimension().location())&&node.anchor().equals(pos)){
                    level.setBlock(pos,AirshipContent.CORE.get().defaultBlockState().setValue(CoreBlock.FACING,node.facing()),3);return;
                }
            if(!level.isClientSide)for(var tile:java.util.List.copyOf(loadedNodes.keySet()))
                if(tile.getLevel()==level&&!tile.isRemoved()&&tile.station()&&tile.anchor.equals(pos)){tile.ensureCore();break;}
        }
    }
    public static class NodeBlock extends BaseEntityBlock implements IWrenchable {
        public final boolean station;
        public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
        public NodeBlock(boolean station){super(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3,6).noOcclusion());this.station=station;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
        @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new NodeTile(p,s);}
        @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
            if(!(l.getBlockEntity(p) instanceof NodeTile tile))return InteractionResult.PASS;
            if(player instanceof ServerPlayer sp){
                if(AllItems.WRENCH.isIn(player.getItemInHand(hand))&&!tile.confirmed){
                    var tag=player.getPersistentData();tag.putLong("OmniverseAirshipAnchorSelection",p.asLong());tag.putString("OmniverseAirshipAnchorDimension",l.dimension().location().toString());
                    sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("airship.anchor.select_block"),true);
                }else {
                    if(AllItems.WRENCH.isIn(player.getItemInHand(hand))&&tile.confirmed){player.getPersistentData().putUUID("OmniverseAirshipBoundNode",tile.nodeId);sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("airship.signal.node_selected"),true);}
                    AirshipNetwork.open(sp,tile,player.isShiftKeyDown());
                }
            }
            return InteractionResult.sidedSuccess(l.isClientSide);
        }
        @Override public InteractionResult onWrenched(BlockState state,UseOnContext context){return use(state,context.getLevel(),context.getClickedPos(),context.getPlayer(),context.getHand(),new BlockHitResult(context.getClickLocation(),context.getClickedFace(),context.getClickedPos(),false));}
        @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){
            if(!s.is(next.getBlock())&&l instanceof ServerLevel sl&&l.getBlockEntity(p) instanceof NodeTile tile&&tile.confirmed)AirshipManager.removeNode(sl.getServer(),tile.nodeId);
            super.onRemove(s,l,p,next,moving);
        }
    }
    public static class NodeTile extends BlockEntity {
        public UUID nodeId=UUID.randomUUID();
        public BlockPos anchor;
        public boolean confirmed,assembling;
        public Direction facing;
        public String name="";
        public double passRadius=3,departureRadius=3;
        public com.simibubi.create.content.decoration.slidingDoor.DoorControl doorControl=com.simibubi.create.content.decoration.slidingDoor.DoorControl.ALL;
        public NodeTile(BlockPos p,BlockState s){super(AirshipContent.NODE_TILE.get(),p,s);anchor=p.above(2);facing=s.getValue(NodeBlock.FACING);}
        public boolean station(){return ((NodeBlock)getBlockState().getBlock()).station;}
        @Override public void onLoad(){
            super.onLoad();
            if(level instanceof ServerLevel){loadedNodes.put(this,true);ensureCore();}
            if(level instanceof ServerLevel sl&&confirmed){var data=FlightData.get(sl.getServer());var saved=data.graph.node(nodeId);
                if(saved==null){
                    // A stale/copied tile must not crash world loading or replace an existing route node.
                    boolean occupied=data.graph.nodes().stream().anyMatch(n->n.block().equals(worldPosition)&&n.dimension().equals(sl.dimension().location()));
                    if(occupied){confirmed=false;assembling=false;changed();}
                    else {data.graph.add(node());data.setDirty();}
                }
                else if(!saved.block().equals(worldPosition)||!saved.dimension().equals(sl.dimension().location())){nodeId=UUID.randomUUID();confirmed=false;assembling=false;changed();}
                else {name=saved.name();passRadius=saved.passRadius();departureRadius=saved.departureRadius();}
            }
        }
        @Override public void setRemoved(){loadedNodes.remove(this);super.setRemoved();}
        public FlightGraph.Node node(){return new FlightGraph.Node(nodeId,level.dimension().location(),worldPosition,anchor,facing,station(),name,passRadius,departureRadius);}
        public boolean assemblyAvailable(){return assembling||station()&&level!=null&&level.hasChunkAt(anchor)&&(level.getBlockState(anchor).is(AirshipContent.CORE.get())||level.getBlockState(anchor).is(AirshipContent.CASING.get()));}
        public void ensureCore(){if(level instanceof ServerLevel&&station()&&level.hasChunkAt(anchor)&&level.getBlockState(anchor).is(AirshipContent.CASING.get()))level.setBlock(anchor,AirshipContent.CORE.get().defaultBlockState().setValue(CoreBlock.FACING,facing),3);}
        public void changed(){setChanged();if(level!=null){ensureCore();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}}
        @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);n.putUUID("NodeId",nodeId);n.putLong("Anchor",anchor.asLong());n.putBoolean("Confirmed",confirmed);n.putBoolean("Assembling",assembling);n.putInt("Facing",facing.get3DDataValue());n.putString("Name",name);n.putDouble("PassRadius",passRadius);n.putDouble("DepartureRadius",departureRadius);n.putInt("DoorControl",doorControl.ordinal());}
        @Override public void load(CompoundTag n){super.load(n);if(n.hasUUID("NodeId"))nodeId=n.getUUID("NodeId");anchor=n.contains("Anchor")?BlockPos.of(n.getLong("Anchor")):worldPosition.above(2);confirmed=n.getBoolean("Confirmed");assembling=n.getBoolean("Assembling");facing=Direction.from3DDataValue(n.getInt("Facing"));if(!facing.getAxis().isHorizontal())facing=Direction.NORTH;name=n.getString("Name");passRadius=radius(n,"PassRadius",3);departureRadius=radius(n,"DepartureRadius",3);var modes=com.simibubi.create.content.decoration.slidingDoor.DoorControl.values();doorControl=n.contains("DoorControl")?modes[Math.floorMod(n.getInt("DoorControl"),modes.length)]:com.simibubi.create.content.decoration.slidingDoor.DoorControl.ALL;}
        private static double radius(CompoundTag n,String key,double fallback){double v=n.contains(key)?n.getDouble(key):fallback;return Double.isFinite(v)&&v>=0&&v<=128?v:fallback;}
        @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
        @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    }
    public static void selectAnchor(PlayerInteractEvent.RightClickBlock e){
        var player=e.getEntity();if(!(player instanceof ServerPlayer sp)||!AllItems.WRENCH.isIn(e.getItemStack()))return;
        var saved=player.getPersistentData();if(!saved.contains("OmniverseAirshipAnchorSelection"))return;
        if(!sp.level().dimension().location().toString().equals(saved.getString("OmniverseAirshipAnchorDimension"))){saved.remove("OmniverseAirshipAnchorSelection");return;}
        var source=BlockPos.of(saved.getLong("OmniverseAirshipAnchorSelection"));if(source.equals(e.getPos()))return;
        saved.remove("OmniverseAirshipAnchorSelection");saved.remove("OmniverseAirshipAnchorDimension");
        if(sp.level().hasChunkAt(source)&&sp.level().getBlockEntity(source) instanceof NodeTile tile&&!tile.confirmed&&source.closerThan(e.getPos(),257)
                &&sp.level().mayInteract(sp,source)){
            tile.anchor=e.getPos().immutable();tile.changed();AirshipNetwork.open(sp,tile,false);e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}
