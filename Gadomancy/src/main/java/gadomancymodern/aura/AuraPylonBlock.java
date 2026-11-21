package gadomancymodern.aura;
import gadomancymodern.Gadomancy;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
public class AuraPylonBlock extends BaseEntityBlock {
    public final boolean cap;
    public AuraPylonBlock(boolean cap){super(Properties.of().strength(10,50).noOcclusion().sound(SoundType.METAL).lightLevel(state->cap?10:0));this.cap=cap;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new AuraPylonEntity(pos,state);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return box(1,0,1,15,16,15);}
    @Override public boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){return !cap||level.getBlockState(pos.below()).is(Gadomancy.PYLON.get());}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context){return canSurvive(defaultBlockState(),context.getLevel(),context.getClickedPos())?defaultBlockState():null;}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){if(!cap){var next=pos.above();while(level.getBlockState(next).is(Gadomancy.PYLON.get()))next=next.above();if(level.getBlockState(next).is(Gadomancy.PYLON_CAP.get()))return Gadomancy.PYLON_CAP.get().use(level.getBlockState(next),level,next,player,hand,hit);return InteractionResult.PASS;}if(level.isClientSide)return InteractionResult.SUCCESS;return form(level,pos,player,hand)?InteractionResult.SUCCESS:InteractionResult.PASS;}
    public static boolean form(Level level,BlockPos top,Player player,InteractionHand hand){var wand=player.getItemInHand(hand);if(!(wand.getItem() instanceof tc4revival.content.CastingWandItem casting))return false;var knowledge=tc4revival.research.KnowledgeSavedData.get(level.getServer()).snapshot(player.getUUID());if(knowledge.isEmpty()||!knowledge.get().researchMarkers().contains("gadomancy:aura_pylon"))return false;for(int i=0;i<4;i++)if(!(level.getBlockEntity(top.below(3-i)) instanceof AuraPylonEntity tile)||tile.formed||tile.cap()!=(i==3))return false;var cost=new java.util.LinkedHashMap<tc4revival.aspect.Aspect,Integer>();for(var primal:thaumcraft.api.aspects.AspectSelection.select(tc4revival.aspect.TCAspects.catalog(),thaumcraft.api.aspects.AspectSelection.STRUCTURE_COST))cost.put(primal,150);if(!casting.consumeForPlayer(wand,player,cost,true,true,true))return false;for(int i=0;i<4;i++){var tile=(AuraPylonEntity)level.getBlockEntity(top.below(3-i));tile.index=i;tile.formed=true;tile.changed();level.blockEvent(tile.getBlockPos(),tile.getBlockState().getBlock(),4,0);}return true;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,Gadomancy.PYLON_BE.get(),AuraPylonEntity::tick);}
}
