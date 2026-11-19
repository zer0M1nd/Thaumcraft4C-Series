package automagy.content;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
public final class BoilerBlock extends BaseEntityBlock {
 public static final BooleanProperty LIT=BlockStateProperties.LIT;
 // The legacy special-render block is non-opaque: retain adjacent faces and light the OBJ.
 public BoilerBlock(){super(Properties.of().strength(3.5f).sound(SoundType.METAL).noOcclusion().lightLevel(s->s.getValue(LIT)?12:0));registerDefaultState(stateDefinition.any().setValue(LIT,false));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(LIT);}
 @Override public RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
 @Override public int getLightBlock(BlockState state,BlockGetter level,BlockPos pos){return 0;}
 @Override public boolean propagatesSkylightDown(BlockState state,BlockGetter level,BlockPos pos){return true;}
 @Override public net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(BlockState state,BlockGetter level,BlockPos pos){return net.minecraft.world.phys.shapes.Shapes.empty();}
 @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random){
  if(!state.getValue(LIT))return;
  double y=pos.getY()+.2+random.nextFloat()*5/16.0,offset=random.nextFloat()*.5-.25;
  for(Direction side:Direction.Plane.HORIZONTAL){
   double x=pos.getX()+.5+side.getStepX()*.52+(side.getStepX()==0?offset:0);
   double z=pos.getZ()+.5+side.getStepZ()*.52+(side.getStepZ()==0?offset:0);
   level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,x,y,z,0,0,0);
   level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,x,y,z,0,0,0);
  }
 }
 @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new BoilerBlockEntity(p,s);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return createTickerHelper(t,Content.BOILER_TILE.get(),l.isClientSide?BoilerBlockEntity::clientTick:BoilerBlockEntity::tick);}
 @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
  if(!player.mayBuild())return InteractionResult.FAIL;
  if(!l.isClientSide&&l.getBlockEntity(p) instanceof BoilerBlockEntity tile){
   if(!net.minecraftforge.fluids.FluidUtil.interactWithFluidHandler(player,hand,l,p,hit.getDirection()))
    net.minecraftforge.network.NetworkHooks.openScreen((net.minecraft.server.level.ServerPlayer)player,new SimpleMenuProvider((id,inv,who)->new BoilerMenu(id,inv,p),net.minecraft.network.chat.Component.translatable("block.automagy.boiler")),p);
  }return InteractionResult.sidedSuccess(l.isClientSide);
 }
 @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!s.is(next.getBlock())&&l.getBlockEntity(p) instanceof BoilerBlockEntity tile)tile.dropContents();super.onRemove(s,l,p,next,moving);}
}
