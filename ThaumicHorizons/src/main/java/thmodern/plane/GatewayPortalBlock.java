package thmodern.plane;
import net.minecraft.core.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
public final class GatewayPortalBlock extends BaseEntityBlock {
 public static final EnumProperty<Direction.Axis> AXIS=BlockStateProperties.HORIZONTAL_AXIS;
 public GatewayPortalBlock(){super(Properties.of().strength(-1,60000).noCollission().noOcclusion().noLootTable().lightLevel(s->11));registerDefaultState(stateDefinition.any().setValue(AXIS,Direction.Axis.X));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(AXIS);}
 @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new GatewayPortalEntity(pos,state);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,GatewayBlocks.PORTAL_ENTITY.get(),(l,p,s,e)->e.tick());}
 @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random){for(int i=0;i<4;i++){double x=pos.getX()+random.nextDouble(),y=pos.getY()+random.nextDouble(),z=pos.getZ()+random.nextDouble(),vx=(random.nextDouble()-.5)*.5,vy=(random.nextDouble()-.5)*.5,vz=(random.nextDouble()-.5)*.5;int sign=random.nextBoolean()?1:-1;if(state.getValue(AXIS)==Direction.Axis.X){z=pos.getZ()+.5+.25*sign;vz=random.nextDouble()*2*sign;}else{x=pos.getX()+.5+.25*sign;vx=random.nextDouble()*2*sign;}level.addParticle(net.minecraft.core.particles.ParticleTypes.PORTAL,x,y,z,vx,vy,vz);}}
 @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity){if(level.getBlockEntity(pos) instanceof GatewayPortalEntity portal&&entity instanceof net.minecraft.server.level.ServerPlayer player)portal.touch(player);}
}
