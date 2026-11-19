package automagy.content;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class RequisitionTomeBlock extends BaseEntityBlock {
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING=net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty POWERED=net.minecraft.world.level.block.state.properties.BooleanProperty.create("powered");
    public RequisitionTomeBlock(){super(Properties.of().strength(3,10).noOcclusion().sound(SoundType.STONE));registerDefaultState(stateDefinition.any().setValue(FACING,net.minecraft.core.Direction.NORTH).setValue(POWERED,false));}
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,POWERED);}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof RequisitionTomeBlockEntity tome){
            // Legacy activation opens a usable terminal before interpreting the held item.
            // Mirror configuration and deposits remain available through its menu.
            if(tome.powered()&&tome.core()!=null&&player instanceof net.minecraft.server.level.ServerPlayer server){
                net.minecraftforge.network.NetworkHooks.openScreen(server,new SimpleMenuProvider((id,inventory,p)->new InventariumMenu(id,inventory,pos),Component.translatable("gui.automagy.requisition.title")),pos);
                return InteractionResult.CONSUME;
            }
            if(player.getItemInHand(hand).getItem() instanceof CrystalEyeItem){tome.installMirror(player,hand);
            }else if(player.getItemInHand(hand).getItem() instanceof tc4revival.content.CastingWandItem){
                if(player.isShiftKeyDown()&&player.mayBuild()){tome.removeMirrors(player);return InteractionResult.CONSUME;}
                if(player.mayBuild()){boolean linked=tome.linkedCore()!=null||InventariumBlockEntity.nearest(level,pos,true).map(core->core.linkMemory(pos)).orElse(false);player.displayClientMessage(Component.translatable(linked?"message.automagy.scribe.linked":"message.automagy.scribe.no_core"),true);}
            }else if(!player.getItemInHand(hand).isEmpty()&&player.mayBuild()&&tome.mirror(0)!=null){
                player.setItemInHand(hand,tome.deposit(player.getItemInHand(hand)));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof RequisitionTomeBlockEntity tome)tome.removed();
        super.onRemove(state,level,pos,next,moving);
    }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return box(0,0,0,16,16,16);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public int getLightBlock(BlockState state,BlockGetter level,BlockPos pos){return 0;}
    @Override public boolean propagatesSkylightDown(BlockState state,BlockGetter level,BlockPos pos){return true;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new RequisitionTomeBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,Content.REQUISITION_TOME_TILE.get(),RequisitionTomeBlockEntity::tick);}
}
