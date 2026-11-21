package automagy.content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
public final class NethermindBlock extends BaseEntityBlock implements thaumcraft.api.wands.IWandable {
 public static final BooleanProperty ACTIVE=BooleanProperty.create("active");
 public NethermindBlock(){super(Properties.of().strength(2.5f,2000).sound(SoundType.STONE));registerDefaultState(stateDefinition.any().setValue(ACTIVE,false));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(ACTIVE);}
 @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new NethermindBlockEntity(pos,state);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,Content.NETHERMIND_TILE.get(),NethermindBlockEntity::tick);}
 @Override public net.minecraft.world.InteractionResult onWandUseFirst(net.minecraft.world.item.ItemStack wand,net.minecraft.world.item.context.UseOnContext context){var player=context.getPlayer();if(player==null||!player.mayBuild())return net.minecraft.world.InteractionResult.PASS;var level=context.getLevel();if(player instanceof net.minecraft.server.level.ServerPlayer server&&level.getBlockEntity(context.getClickedPos()) instanceof NethermindBlockEntity tile){if(tile.enabled()){if(tile.age()>10)tile.disable();}else tile.enable(server,wand);}return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);}
 @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random){
  if(!(level.getBlockEntity(pos) instanceof NethermindBlockEntity tile)||!tile.conjuring())return;
  int choice=random.nextInt(20);
  var particle=choice<5?net.minecraft.core.particles.ParticleTypes.SMOKE:choice<8?net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE:choice<16?net.minecraft.core.particles.ParticleTypes.LAVA:net.minecraft.core.particles.ParticleTypes.FLAME;
  level.addParticle(particle,pos.getX()+.25+random.nextFloat()*.5,pos.getY()+1.1,pos.getZ()+.25+random.nextFloat()*.5,0,0,0);
  if(tile.visualLureDepth()==0&&random.nextBoolean())level.addParticle(net.minecraft.core.particles.ParticleTypes.DRIPPING_LAVA,pos.getX()+.6,pos.getY()-.02,pos.getZ()+.6,0,0,0);
 }
 @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock())&&!level.isClientSide&&level.getBlockEntity(pos) instanceof NethermindBlockEntity tile)tile.cleanup();super.onRemove(state,level,pos,next,moving);}
}
