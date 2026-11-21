package gadomancymodern.knowledge;
import gadomancymodern.Gadomancy;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
public class KnowledgeBookBlock extends BaseEntityBlock implements KnowledgeProvider {
    public KnowledgeBookBlock(){super(Properties.of().strength(0).noOcclusion().lightLevel(state->8).sound(SoundType.WOOD));}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new KnowledgeBookEntity(pos,state);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){return box(1,2,1,15,8,15);}
    @Override public boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){return level.getBlockState(pos.below()).is(tc4revival.content.TCArcane.ARCANE_PEDESTAL.get())&&level.isEmptyBlock(pos.above());}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return context.getClickedFace()==Direction.UP&&canSurvive(defaultBlockState(),context.getLevel(),context.getClickedPos())?defaultBlockState():null;}
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block changed,BlockPos source,boolean moving){if(!level.isClientSide&&!canSurvive(state,level,pos))level.destroyBlock(pos,true);}
    @Override public InteractionResult use(BlockState state,Level world,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){if(player instanceof ServerPlayer server)tc4revival.research.ResearchBookOpening.open(server,hand,ignored->world.getBlockState(pos).is(this)&&player.distanceToSqr(pos.getCenter())<=64,ignored->false);return InteractionResult.sidedSuccess(world.isClientSide);}
    @Override public boolean hasAnalogOutputSignal(BlockState state){return true;}
    @Override public int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos){return level.getBlockEntity(pos) instanceof KnowledgeBookEntity book&&book.researching?book.cognition>0?15:1:0;}
    @Override public int providedKnowledge(Level level,BlockPos pos){return 2;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,Gadomancy.KNOWLEDGE_BOOK_BE.get(),KnowledgeBookEntity::tick);}
}
