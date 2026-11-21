package thmodern.alchemy;
import net.minecraft.core.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import thmodern.Horizons;
public final class AlchemiteBlock extends TntBlock {
 public AlchemiteBlock(){super(Properties.of().strength(.7f,1).lightLevel(s->7).sound(SoundType.GRASS));}
 @Override public void onCaughtFire(BlockState state,Level level,BlockPos pos,Direction side,LivingEntity igniter){prime(level,pos,igniter,false);}
 @Override public void wasExploded(Level level,BlockPos pos,Explosion explosion){prime(level,pos,explosion.getIndirectSourceEntity(),true);}
 private void prime(Level level,BlockPos pos,LivingEntity owner,boolean chain){if(level.isClientSide)return;var entity=Horizons.ALCHEMITE_ENTITY.get().create(level);entity.ignite(pos.getX()+.5,pos.getY(),pos.getZ()+.5,owner);if(chain)entity.setFuse(level.random.nextInt(20)+10);if(level.addFreshEntity(entity))level.playSound(null,pos,net.minecraft.sounds.SoundEvents.TNT_PRIMED,net.minecraft.sounds.SoundSource.BLOCKS,1,1);}
}
