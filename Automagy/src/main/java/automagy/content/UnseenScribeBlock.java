package automagy.content;

import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

public final class UnseenScribeBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public UnseenScribeBlock(){super(Properties.of().strength(2.5f).noOcclusion().sound(SoundType.WOOD));registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(hit.getDirection()==Direction.UP&&player.getItemInHand(hand).is(Content.SCRIBE_POINTER.get().asItem())&&level.isEmptyBlock(pos.above()))return InteractionResult.PASS;
        if(level.getBlockEntity(pos) instanceof UnseenScribeBlockEntity scribe&&scribe.powered()&&scribe.linkedCore()!=null){
            if(!level.hasChunkAt(scribe.linkedCore())){
                if(!level.isClientSide)player.displayClientMessage(Component.translatable("Automagy.chat.inventarium.chunkUnloaded"),false);
            }else if(player instanceof net.minecraft.server.level.ServerPlayer server)
                net.minecraftforge.network.NetworkHooks.openScreen(server,new SimpleMenuProvider((id,inventory,p)->new InventariumMenu(id,inventory,pos),Component.translatable("gui.automagy.inventarium.title")),pos);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if(!level.isClientSide&&player.mayBuild()&&player.getItemInHand(hand).getItem() instanceof tc4revival.content.CastingWandItem&&level.getBlockEntity(pos) instanceof UnseenScribeBlockEntity scribe){
            boolean linked=scribe.linkedCore()!=null||InventariumBlockEntity.nearest(level,pos).map(core->core.link(pos)).orElse(false);
            player.displayClientMessage(Component.translatable(linked?"message.automagy.scribe.linked":"message.automagy.scribe.no_core"),true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof UnseenScribeBlockEntity scribe&&scribe.linkedCore()!=null&&level.hasChunkAt(scribe.linkedCore())&&level.getBlockEntity(scribe.linkedCore()) instanceof InventariumBlockEntity core)core.unlink(pos);
        super.onRemove(state,level,pos,next,moving);
    }
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public int getLightBlock(BlockState state,BlockGetter level,BlockPos pos){return 0;}
    @Override public boolean propagatesSkylightDown(BlockState state,BlockGetter level,BlockPos pos){return true;}
    @Override public net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(BlockState state,BlockGetter level,BlockPos pos){return net.minecraft.world.phys.shapes.Shapes.empty();}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new UnseenScribeBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,Content.SCRIBE_TILE.get(),UnseenScribeBlockEntity::tick);}
}
