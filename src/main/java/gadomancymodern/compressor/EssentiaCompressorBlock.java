package gadomancymodern.compressor;
import gadomancymodern.Gadomancy;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
public class EssentiaCompressorBlock extends BaseEntityBlock {
    public EssentiaCompressorBlock(){super(Properties.of().strength(10,50).noOcclusion().sound(SoundType.METAL));}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new EssentiaCompressorEntity(pos,state);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){if(!(player.getItemInHand(hand).getItem() instanceof tc4revival.content.CastingWandItem))return InteractionResult.PASS;if(level.isClientSide)return InteractionResult.SUCCESS;return form(level,pos,player,hand)?InteractionResult.SUCCESS:InteractionResult.PASS;}
    public static boolean form(Level level,BlockPos at,Player player,InteractionHand hand){var stack=player.getItemInHand(hand);if(!(stack.getItem() instanceof tc4revival.content.CastingWandItem wand))return false;var knowledge=tc4revival.research.KnowledgeSavedData.get(level.getServer()).snapshot(player.getUUID());if(knowledge.isEmpty()||!knowledge.get().researchMarkers().contains("gadomancy:essentia_compressor"))return false;var base=at;while(level.getBlockEntity(base.below()) instanceof EssentiaCompressorEntity below&&!below.formed())base=base.below();for(int i=0;i<3;i++)if(!(level.getBlockEntity(base.above(i)) instanceof EssentiaCompressorEntity tile)||tile.formed())return false;var costs=new java.util.LinkedHashMap<tc4revival.aspect.Aspect,Integer>();java.util.Map.of("aqua",140,"terra",80,"aer",150,"ordo",20,"perditio",140).forEach((a,n)->costs.put(tc4revival.aspect.TCAspects.catalog().require(a),n));if(!wand.consumeForPlayer(stack,player,costs,true,true,true))return false;var id=java.util.UUID.randomUUID();for(int i=0;i<3;i++){var tile=(EssentiaCompressorEntity)level.getBlockEntity(base.above(i));tile.structure=id;tile.index=i;tile.changed();}return true;}
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moved){if(!next.is(this)&&!level.isClientSide&&level.getBlockEntity(pos) instanceof EssentiaCompressorEntity tile&&tile.formed())tile.breakStructure();super.onRemove(state,level,pos,next,moved);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,Gadomancy.COMPRESSOR_BE.get(),EssentiaCompressorEntity::tick);}
}
