package thmodern.plane;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
public final class LeviathanBlock extends Block {
 public LeviathanBlock(){super(Properties.of().strength(10,10));}
 @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){
  for(var face:Direction.values())if(random.nextInt(10)==0&&!level.getBlockState(pos.relative(face)).isSolidRender(level,pos.relative(face))){double x=pos.getX()+random.nextDouble(),y=pos.getY()+random.nextDouble(),z=pos.getZ()+random.nextDouble();if(face.getAxis()==Direction.Axis.X)x=pos.getX()+.5+face.getStepX()*.5625;if(face.getAxis()==Direction.Axis.Y)y=pos.getY()+.5+face.getStepY()*.5625;if(face.getAxis()==Direction.Axis.Z)z=pos.getZ()+.5+face.getStepZ()*.5625;level.addParticle(ParticleTypes.SMOKE,x,y,z,0,0,0);}
 }
}
