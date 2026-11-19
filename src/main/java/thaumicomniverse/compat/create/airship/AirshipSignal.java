package thaumicomniverse.compat.create.airship;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

/** A whole route is one section. Wrench a node, then use this signal to bind or cycle its routes. */
public final class AirshipSignal extends BaseEntityBlock {
    public static final IntegerProperty STATUS=IntegerProperty.create("status",0,2);
    public AirshipSignal(){super(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3));registerDefaultState(stateDefinition.any().setValue(STATUS,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(STATUS);}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Tile(p,s);}
    @Override public boolean isSignalSource(BlockState s){return true;}
    @Override public int getSignal(BlockState s,BlockGetter l,BlockPos p,Direction d){return s.getValue(STATUS)==1?0:15;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,AirshipContent.SIGNAL_TILE.get(),Tile::tick);}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(l instanceof ServerLevel sl&&l.getBlockEntity(p) instanceof Tile tile){
            var data=FlightData.get(sl.getServer());var tag=player.getPersistentData();
            if(tag.hasUUID("OmniverseAirshipBoundNode")){
                var node=data.graph.node(tag.getUUID("OmniverseAirshipBoundNode"));
                if(node!=null&&node.dimension().equals(l.dimension().location())&&node.block().distManhattan(p)<=32){
                    var exits=data.graph.exits(node.id(),null);
                    if(!exits.isEmpty()){int index=-1;for(int i=0;i<exits.size();i++)if(exits.get(i).id().equals(tile.route))index=i;
                        tile.node=node.id();tile.route=exits.get((index+1)%exits.size()).id();tile.setChanged();
                        player.displayClientMessage(Component.translatable("airship.signal.bound",node.name(),exits.get((index+1)%exits.size()).sequence()),true);
                    }else player.displayClientMessage(Component.translatable("airship.error.no_exit"),true);
                }else player.displayClientMessage(Component.translatable("airship.error.signal_range"),true);
            }else player.displayClientMessage(Component.translatable("airship.signal.select_node"),true);
        }return InteractionResult.sidedSuccess(l.isClientSide);
    }
    public static final class Tile extends BlockEntity {
        UUID node,route;
        public Tile(BlockPos p,BlockState s){super(AirshipContent.SIGNAL_TILE.get(),p,s);}
        @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);if(node!=null)n.putUUID("Node",node);if(route!=null)n.putUUID("Route",route);}
        @Override public void load(CompoundTag n){super.load(n);node=n.hasUUID("Node")?n.getUUID("Node"):null;route=n.hasUUID("Route")?n.getUUID("Route"):null;}
        static void tick(Level l,BlockPos p,BlockState s,Tile tile){
            if(l.getGameTime()%10!=0)return;var g=FlightData.get(((ServerLevel)l).getServer()).graph;int status=0;var route=g.route(tile.route);
            if(route!=null&&g.node(tile.node)!=null){var dest=g.node(route.other(tile.node));status=g.routeOwner(tile.route)==null&&g.stationOwner(dest.id())==null?1:2;}
            if(s.getValue(STATUS)!=status){l.setBlock(p,s.setValue(STATUS,status),3);l.updateNeighborsAt(p,s.getBlock());}
        }
    }
}
