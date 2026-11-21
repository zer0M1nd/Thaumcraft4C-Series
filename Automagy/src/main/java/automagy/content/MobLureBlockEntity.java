package automagy.content;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Mob;
import java.util.*;
public final class MobLureBlockEntity extends InventoryBlockEntity {
 private static final Map<Level,Set<BlockPos>> LOADED=new WeakHashMap<>();
 public long nextAmbient;
 private int boost,top,bottom,strength,vertical=60,charm;
 public MobLureBlockEntity(BlockPos pos,BlockState state){super(Content.MOB_LURE_TILE.get(),pos,state,0);top=bottom=pos.getY();}
 public boolean enabled(){return getBlockState().getValue(MobLureBlock.ACTIVE);}public int strength(){return strength;}public double range(){return boost>0?32.5:8.5;}
 public boolean affects(Mob mob,double radius){double dx=worldPosition.getX()+.5-mob.getX(),dz=worldPosition.getZ()+.5-mob.getZ();return enabled()&&automagy.logic.MobLureRules.inRange(dx,dz,mob.getY(),bottom,top,radius);}
 private boolean structure(){top=bottom=worldPosition.getY();while(top+1<level.getMaxBuildHeight()&&level.getBlockState(new BlockPos(worldPosition.getX(),top+1,worldPosition.getZ())).is(tc4revival.content.TCArcane.OBSIDIAN_TOTEM.get()))top++;while(bottom>level.getMinBuildHeight()&&level.getBlockState(new BlockPos(worldPosition.getX(),bottom-1,worldPosition.getZ())).is(tc4revival.content.TCArcane.OBSIDIAN_TOTEM.get()))bottom--;strength=top-bottom;return strength>=2;}
 public void toggle(){
  if(level==null||level.isClientSide)return;
  if(enabled()){
   boost=0;level.setBlockAndUpdate(worldPosition,getBlockState().setValue(MobLureBlock.ACTIVE,false));
   level.playSound(null,worldPosition,tc4revival.content.TCSounds.WAND_FAIL.get(),net.minecraft.sounds.SoundSource.BLOCKS,.5f,.4f);
  }else if(structure()){
   level.setBlockAndUpdate(worldPosition,getBlockState().setValue(MobLureBlock.ACTIVE,true));
   level.playSound(null,worldPosition,tc4revival.content.TCSounds.BRAIN.get(),net.minecraft.sounds.SoundSource.BLOCKS,.8f,.2f+level.random.nextFloat()*.3f);
   if(level instanceof net.minecraft.server.level.ServerLevel server){server.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,35,.5,.5,.5,.01);server.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,16,.5,.25,.5,0);}
  }
  vertical=0;sync();
 }
 @Override public void onLoad(){super.onLoad();if(level!=null&&!level.isClientSide)LOADED.computeIfAbsent(level,l->new HashSet<>()).add(worldPosition.immutable());}
 @Override public void setRemoved(){if(level!=null){var positions=LOADED.get(level);if(positions!=null)positions.remove(worldPosition);}super.setRemoved();}
 public static MobLureBlockEntity nearest(Mob mob){var positions=LOADED.get(mob.level());if(positions==null)return null;MobLureBlockEntity result=null;double best=Double.MAX_VALUE;for(var pos:positions){if(!mob.level().hasChunkAt(pos)||!(mob.level().getBlockEntity(pos) instanceof MobLureBlockEntity lure)||!lure.affects(mob,lure.range()))continue;double d=mob.distanceToSqr(pos.getX()+.5,pos.getY(),pos.getZ()+.5);if(d<=lure.range()*lure.range()&&d<best){best=d;result=lure;}}return result;}
 private static boolean eligible(Mob mob){return mob.isAlive()&&!mob.isNoAi()&&!(mob instanceof tc4revival.content.GolemEntity)&&!(mob instanceof net.minecraft.world.entity.boss.wither.WitherBoss)&&!mob.isMultipartEntity()&&(mob instanceof net.minecraft.world.entity.PathfinderMob||mob instanceof net.minecraft.world.entity.monster.Enemy||mob instanceof net.minecraft.world.entity.animal.WaterAnimal||mob instanceof net.minecraft.world.entity.ambient.AmbientCreature);}
 public static void tick(Level level,BlockPos pos,BlockState state,MobLureBlockEntity tile){if(level.isClientSide||!tile.enabled())return;if(++tile.vertical>=60){tile.vertical=0;if(!tile.structure()){tile.toggle();return;}tile.sync();}int previous=tile.boost;if(tile.boost>0)tile.boost--;if(--tile.charm<=0){tile.charm=2+level.random.nextInt(2);if(tile.boost==0&&tc4revival.aspect.TCAspects.catalog().find("fames").map(a->tc4revival.content.EssentiaSources.drain(tile,a,null,8,false).isPresent()).orElse(false))tile.boost=400;double range=tile.range();var bounds=new net.minecraft.world.phys.AABB(pos.getX()+.5-range,tile.bottom,pos.getZ()+.5-range,pos.getX()+.5+range,tile.top+1,pos.getZ()+.5+range);for(var mob:level.getEntitiesOfClass(Mob.class,bounds,MobLureBlockEntity::eligible))CharmWalkGoal.offer(mob,tile);}if(previous!=tile.boost){tile.setChanged();if(previous==0||tile.boost==0)tile.sync();}}
 @Override protected void saveAdditional(net.minecraft.nbt.CompoundTag tag){super.saveAdditional(tag);tag.putInt("Boost",boost);tag.putInt("Top",top);tag.putInt("Bottom",bottom);}
 @Override public void load(net.minecraft.nbt.CompoundTag tag){super.load(tag);boost=Math.max(0,Math.min(400,tag.getInt("Boost")));top=tag.getInt("Top");bottom=tag.getInt("Bottom");vertical=60;}
}
