package automagy.content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
public final class DimensionLureBlock extends BaseEntityBlock implements thaumcraft.api.wands.IWandable {
 public static final BooleanProperty ACTIVE=BooleanProperty.create("active");
 public DimensionLureBlock(){super(Properties.of().strength(2.5f,2000).sound(SoundType.STONE));registerDefaultState(stateDefinition.any().setValue(ACTIVE,false));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(ACTIVE);}
 @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new DimensionLureBlockEntity(pos,state);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,Content.DIMENSION_LURE_TILE.get(),DimensionLureBlockEntity::tick);}
 @Override public net.minecraft.world.InteractionResult onWandUseFirst(net.minecraft.world.item.ItemStack wand,net.minecraft.world.item.context.UseOnContext context){var player=context.getPlayer();if(player==null||!player.mayBuild())return net.minecraft.world.InteractionResult.PASS;var level=context.getLevel();if(!level.isClientSide&&level.getBlockEntity(context.getClickedPos()) instanceof DimensionLureBlockEntity tile)tile.enable();return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);}
 @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof DimensionLureBlockEntity tile)tile.disable(false);super.onRemove(state,level,pos,next,moving);}
 @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random){
  if(!(level.getBlockEntity(pos) instanceof DimensionLureBlockEntity tile)||!tile.enabled())return;
  long now=level.getGameTime();
  if(tile.lastAmbient==Long.MIN_VALUE){tile.lastAmbient=now-900;return;}
  long elapsed=now-tile.lastAmbient;
  if(elapsed>=240&&(tile.conjuring()?(elapsed>=340||random.nextInt(5)==0):(elapsed>=1000&&random.nextInt(10)==0))){
   tile.lastAmbient=now;level.playLocalSound(pos.getX()+.5,tile.midpoint(),pos.getZ()+.5,net.minecraft.sounds.SoundEvents.PORTAL_AMBIENT,net.minecraft.sounds.SoundSource.BLOCKS,1,.3f+random.nextFloat()*.3f,false);
  }
 }
}
