package automagy.content;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
/** One charm goal per mob, with no static entity references or permanent AI removal. */
public final class CharmWalkGoal extends Goal {
 private final Mob mob;private BlockPos target,blocked;private double radius;private int strength,stuck;private long blockedUntil;private BlockPos previous;private float speed;
 private CharmWalkGoal(Mob mob,MobLureBlockEntity lure){this.mob=mob;setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.LOOK));assign(lure);}
 private void assign(MobLureBlockEntity lure){target=lure.getBlockPos().immutable();radius=lure.range()+1;strength=lure.strength();}
 private MobLureBlockEntity source(){return mob.level().hasChunkAt(target)&&mob.level().getBlockEntity(target) instanceof MobLureBlockEntity lure?lure:null;}
 private boolean allowed(){var lure=source();var victim=mob.getTarget();boolean recent=automagy.logic.MobLureRules.protectsTarget(victim!=null,mob.tickCount,mob.getLastHurtByMobTimestamp(),victim==null?Double.POSITIVE_INFINITY:mob.distanceToSqr(victim));return mob.isAlive()&&lure!=null&&lure.affects(mob,radius)&&!recent&&(!target.equals(blocked)||mob.level().getGameTime()>=blockedUntil);}
 public static void offer(Mob mob,MobLureBlockEntity lure){CharmWalkGoal goal=null;for(var wrapped:mob.goalSelector.getAvailableGoals())if(wrapped.getGoal() instanceof CharmWalkGoal charm){goal=charm;break;}if(goal==null){mob.goalSelector.addGoal(-1,new CharmWalkGoal(mob,lure));return;}if(goal.target.equals(lure.getBlockPos())){goal.radius=lure.range()+1;goal.strength=lure.strength();return;}if(goal.blocked!=null&&goal.blocked.equals(lure.getBlockPos())&&mob.level().getGameTime()<goal.blockedUntil)return;if(goal.strength>=lure.strength()&&goal.allowed())return;if(lure.affects(mob,lure.range()+1)){goal.assign(lure);goal.stuck=0;}}
 @Override public boolean canUse(){return allowed();}@Override public boolean canContinueToUse(){return allowed();}@Override public boolean requiresUpdateEveryTick(){return true;}
 @Override public void start(){speed=0;previous=mob.blockPosition();mob.getNavigation().stop();}
 @Override public void stop(){speed=0;mob.setZza(0);}
 @Override public void tick(){var now=mob.blockPosition();if(previous!=null&&previous.getX()==now.getX()&&previous.getZ()==now.getZ())stuck++;else{stuck=0;previous=now;}if(stuck>140){blocked=target;blockedUntil=mob.level().getGameTime()+100;stuck=0;return;}
  double dx=target.getX()+.5-mob.getX(),dz=target.getZ()+.5-mob.getZ();mob.setYRot((float)(-Math.atan2(dx,dz)*180/Math.PI));mob.yBodyRot=mob.getYRot();mob.getLookControl().setLookAt(target.getX()+.5,target.getY(),target.getZ()+.5);mob.getMoveControl().setWantedPosition(target.getX()+.5,target.getY(),target.getZ()+.5,.1);speed+=(5-speed)*.01f;
  var below=mob.level().getBlockState(mob.blockPosition().below()).getBlock();if(mob.horizontalCollision&&mob.onGround()&&!(below instanceof net.minecraft.world.level.block.SlabBlock)&&!(below instanceof net.minecraft.world.level.block.StairBlock)&&mob.level().noCollision(mob,mob.getBoundingBox().move(0,1,0)))mob.getJumpControl().jump();
  mob.travel(new Vec3(0,0,speed));mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS,20,0));
 }
}
