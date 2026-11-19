package automagy.content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
public final class MobLureBlock extends BaseEntityBlock implements thaumcraft.api.wands.IWandable {
 public static final BooleanProperty ACTIVE=BooleanProperty.create("active");
 public MobLureBlock(){super(Properties.of().strength(2.5f).requiresCorrectToolForDrops().sound(SoundType.STONE));registerDefaultState(stateDefinition.any().setValue(ACTIVE,false));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(ACTIVE);}
 @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new MobLureBlockEntity(pos,state);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,Content.MOB_LURE_TILE.get(),MobLureBlockEntity::tick);}
 @Override public net.minecraft.world.InteractionResult onWandUseFirst(net.minecraft.world.item.ItemStack wand,net.minecraft.world.item.context.UseOnContext context){if(context.getPlayer()==null||!context.getPlayer().mayBuild())return net.minecraft.world.InteractionResult.PASS;var level=context.getLevel();if(!level.isClientSide&&level.getBlockEntity(context.getClickedPos()) instanceof MobLureBlockEntity tile)tile.toggle();return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);}
 @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random){
  if(level.getBlockEntity(pos) instanceof MobLureBlockEntity tile&&tile.enabled()&&level.getGameTime()>=tile.nextAmbient){
   tile.nextAmbient=level.getGameTime()+300+random.nextInt(200);
   level.playLocalSound(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,tc4revival.content.TCSounds.BRAIN.get(),net.minecraft.sounds.SoundSource.BLOCKS,.4f,1.4f+random.nextFloat()*.25f,false);
  }
 }
}
