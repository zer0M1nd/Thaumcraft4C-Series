package automagy.content;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.core.*;
public final class NetherHeartItem extends Item implements thaumcraft.api.IWarpingGear {
 public NetherHeartItem(){super(new Properties().stacksTo(1));}
 @Override public int getWarp(ItemStack stack,Player player){return 1+Math.max(0,Math.min(5,stack.hasTag()?stack.getTag().getInt("WarpExtra"):0));}
 @Override public int getUseDuration(ItemStack stack){return 520;}
 @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.BLOCK;}
 @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level,Player player,net.minecraft.world.InteractionHand hand){return ItemUtils.startUsingInstantly(level,player,hand);}
 @Override public void onUseTick(Level level,LivingEntity user,ItemStack stack,int remaining){if(!(user instanceof Player player)||level.isClientSide)return;
  if(remaining==460||remaining==275)level.playSound(null,player.blockPosition(),tc4revival.content.TCSounds.HEARTBEAT.get(),net.minecraft.sounds.SoundSource.PLAYERS,remaining==460?.4f:1,.5f);
  if(remaining<400){if(remaining%90==0)level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.BLAZE_AMBIENT,net.minecraft.sounds.SoundSource.PLAYERS,.1f,.9f);
   if(remaining==1){for(int i=0;i<40;i++)ignite(level,player,4);int warp=getWarp(stack,player);if(warp<6){stack.getOrCreateTag().putInt("WarpExtra",warp);player.addEffect(new net.minecraft.world.effect.MobEffectInstance(tc4revival.content.TCEffects.SUN_SCORNED.get(),800,1));}level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.GHAST_SHOOT,net.minecraft.sounds.SoundSource.PLAYERS,1,.5f);}
   else if(remaining%35==0){if(level.random.nextInt(3)==0)ignite(level,player,3);else level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.FIRE_AMBIENT,net.minecraft.sounds.SoundSource.PLAYERS,.4f,.5f+level.random.nextFloat()/2);}
  }
  if(remaining%15==0&&level instanceof net.minecraft.server.level.ServerLevel server){var p=player.position().add(player.getLookAngle()).add(0,player.getBbHeight()*.75,0);server.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,p.x,p.y,p.z,8,.1,.1,.1,.01);}
 }
 private static void ignite(Level level,Player player,int radius){if(!player.mayBuild())return;var origin=player.blockPosition();for(int i=0;i<200;i++){var p=origin.offset(level.random.nextInt(radius*2+1)-radius,level.random.nextInt(radius*2+1)-radius,level.random.nextInt(radius*2+1)-radius);if(level.isOutsideBuildHeight(p)||!level.hasChunkAt(p)||!level.isEmptyBlock(p))continue;boolean supported=false;for(var side:Direction.values()){var adjacent=p.relative(side);if(level.hasChunkAt(adjacent)&&level.getBlockState(adjacent).isFaceSturdy(level,adjacent,side.getOpposite())){supported=true;break;}}if(supported){level.setBlockAndUpdate(p,net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());return;}}}
}
