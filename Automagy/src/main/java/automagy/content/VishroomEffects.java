package automagy.content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerPlayer;
import tc4revival.content.TCEffects;
public final class VishroomEffects {
 public static void touch(Level level,BlockPos pos,boolean source,Entity entity){
  var data=entity.getPersistentData();long now=level.getGameTime();
  if(entity instanceof LivingEntity living){
   if(now-data.getLong("automagy:VishroomEffect")>=60){data.putLong("automagy:VishroomEffect",now);
    boolean escalate=living.hasEffect(MobEffects.WEAKNESS)&&(!(living instanceof ServerPlayer)||living.hasEffect(TCEffects.BLURRED_VISION.get()));
    if(escalate&&level.random.nextInt(10)==0)living.addEffect(new MobEffectInstance(living instanceof MushroomCow?MobEffects.REGENERATION:MobEffects.POISON,living instanceof MushroomCow?320:80,living instanceof MushroomCow?0:2));
    if(living instanceof ServerPlayer&&living.hasEffect(MobEffects.WEAKNESS))living.addEffect(new MobEffectInstance(TCEffects.BLURRED_VISION.get(),200,1));
    living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,200,1));
    if(source&&living instanceof ServerPlayer player){player.removeEffect(TCEffects.WARP_WARD.get());warpBath(player,pos,now);}
   }
   // One exposure tick even when the cow intersects several source blocks.
   if(source&&living instanceof Cow cow&&!(cow instanceof MushroomCow)&&cow.isAlive()&&data.getLong("automagy:VishroomCowTick")!=now){
    data.putLong("automagy:VishroomCowTick",now);int count=data.getInt("automagy:VishroomCowTime")+1;data.putInt("automagy:VishroomCowTime",count);
    if(count>=1200){var changed=cow.convertTo(EntityType.MOOSHROOM,true);if(changed!=null){changed.setHealth(Math.min(changed.getMaxHealth(),cow.getHealth()));changed.addEffect(new MobEffectInstance(MobEffects.REGENERATION,320));poof(level,pos);if(level.isEmptyBlock(pos.above()))poof(level,pos.above());}}
   }
  }else if(source&&entity instanceof ItemEntity item&&item.getItem().is(Items.GRASS_BLOCK)&&now-data.getLong("automagy:VishroomItemTick")>=20){
   data.putLong("automagy:VishroomItemTick",now);var output=new ItemEntity(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,new ItemStack(Items.MYCELIUM));output.setDeltaMovement(0,.25,0);
   if(level.addFreshEntity(output)){item.getItem().shrink(1);if(item.getItem().isEmpty())item.discard();if(level.random.nextInt(5)==0)poof(level,pos);}
  }
 }
 private static void warpBath(ServerPlayer player,BlockPos pos,long now){
  if(tc4revival.config.TCConfig.SERVER.disableWarp.get())return;
  var data=player.getPersistentData();var storage=tc4revival.research.KnowledgeSavedData.get(player.server);var random=player.level().random;
  boolean[] consumed={false},changed={false};
  storage.update(player.getUUID(),random::nextInt,k->{int warp=k.warp(tc4revival.research.PlayerKnowledge.WarpKind.TEMPORARY)+k.warp(tc4revival.research.PlayerKnowledge.WarpKind.STICKY);int diff=warp-data.getInt("automagy:VishroomWarpSeen");
   if(now-data.getLong("automagy:VishroomWarpTick")<=60&&diff>0){changed[0]=true;for(int i=0;i<diff;i++)if(random.nextInt(8)==0){consumed[0]=true;break;}warp+=diff;if(random.nextInt(4)==0){k.changeWarp(tc4revival.research.PlayerKnowledge.WarpKind.STICKY,1);diff--;}if(diff>0)k.changeWarp(tc4revival.research.PlayerKnowledge.WarpKind.TEMPORARY,diff);player.addEffect(new MobEffectInstance(MobEffects.HUNGER,600,1));}
   data.putInt("automagy:VishroomWarpSeen",warp);data.putLong("automagy:VishroomWarpTick",now);return null;});
  if(changed[0]||random.nextInt(60)==0){if(!changed[0])storage.update(player.getUUID(),random::nextInt,k->{k.setWarpCounter(k.totalWarp());return null;});tc4revival.content.WarpEvents.check(player);}
  if(consumed[0])poof(player.level(),pos);
 }
 static void poof(Level level,BlockPos pos){
  level.setBlockAndUpdate(pos,tc4revival.content.TCArcane.FLUX_GAS.get().defaultBlockState());
  for(var player:level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,new net.minecraft.world.phys.AABB(pos).inflate(.5,.1,.5)))player.addEffect(new MobEffectInstance(MobEffects.CONFUSION,180,1));
  if(level instanceof net.minecraft.server.level.ServerLevel server)server.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,20,.5,.5,.5,.02);
  level.playSound(null,pos,net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,net.minecraft.sounds.SoundSource.BLOCKS,.5f,2.6f);
 }
 static boolean spread(Level level,BlockPos pos,int depth){
  if(depth>3||!level.hasChunkAt(pos))return false;var state=level.getBlockState(pos);var random=level.random;
  if(state.is(SoupFluids.VISHROOM.block.get())){if(state.getFluidState().isSource())return false;int r=random.nextInt(10);var dir=net.minecraft.core.Direction.from3DDataValue(r<6?r:r-4);return spread(level,pos.relative(dir).offset(r<6?0:random.nextInt(3)-1,0,r<6?0:random.nextInt(3)-1),depth+1);}
  boolean plant=state.is(Blocks.GRASS)||state.is(Blocks.TALL_GRASS)||state.is(Blocks.FERN)||state.is(net.minecraft.tags.BlockTags.FLOWERS);
  if(state.is(Blocks.GRASS_BLOCK)){var above=level.getBlockState(pos.above());if(above.is(Blocks.GRASS)||above.is(Blocks.TALL_GRASS)||above.is(net.minecraft.tags.BlockTags.FLOWERS)||above.isAir()&&random.nextInt(8)==0)level.setBlockAndUpdate(pos.above(),(random.nextBoolean()?Blocks.RED_MUSHROOM:Blocks.BROWN_MUSHROOM).defaultBlockState());level.setBlockAndUpdate(pos,Blocks.MYCELIUM.defaultBlockState());return true;}
  if(plant){var mushroom=(random.nextBoolean()?Blocks.RED_MUSHROOM:Blocks.BROWN_MUSHROOM).defaultBlockState();if(mushroom.canSurvive(level,pos)){level.setBlockAndUpdate(pos,mushroom);}else return spread(level,pos.below(),depth+1);}
  return false;
 }
 private VishroomEffects(){}
}
