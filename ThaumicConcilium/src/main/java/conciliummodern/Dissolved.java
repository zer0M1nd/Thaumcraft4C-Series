package conciliummodern;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import tc4revival.content.*;
public final class Dissolved extends ConciliumHostile {
 public Dissolved(EntityType<? extends Dissolved> type,Level level){super(type,level);}
 public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.FOLLOW_RANGE,32).add(Attributes.MOVEMENT_SPEED,.5).add(Attributes.MAX_HEALTH,60).add(Attributes.ATTACK_DAMAGE,10);}
 @Override protected void registerGoals(){basicGoals(.4,false);}
 @Override protected void targets(){legacyTargets(true,false,true,true);}
 @Override public boolean isInvulnerableTo(DamageSource source){return !(source.is(DamageTypes.FELL_OUT_OF_WORLD)||source.is(DamageTypes.IN_WALL))||super.isInvulnerableTo(source);}
 @Override public boolean doHurtTarget(Entity target){swing(InteractionHand.MAIN_HAND);return target.hurt(damageSources().fellOutOfWorld(),(float)getAttributeValue(Attributes.ATTACK_DAMAGE));}
 @Override public void die(DamageSource source){if(!level().isClientSide)for(int i=0;i<15;i++)thaumcraft.api.fx.LegacyEffects.sparkle(level(),position().add(random.nextDouble()-.5,random.nextDouble()*2,random.nextDouble()-.5),0xa020f0,1,1,0);super.die(source);}
 @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){var wand=player.getItemInHand(hand);if(wand.getItem() instanceof CastingWandItem&&WandFocusItem.attached(wand).is(TCItems.PORTABLE_HOLE_FOCUS.get())){if(!level().isClientSide)hurt(damageSources().fellOutOfWorld(),9000);return InteractionResult.sidedSuccess(level().isClientSide);}return super.mobInteract(player,hand);}
 @Override public void tick(){super.tick();if(!level().isClientSide&&isAlive()&&tickCount%200==0&&getTarget()!=null&&getTarget().isAlive()){var hole=RemainingMobs.UPCOMING_HOLE.get().create(level());if(hole!=null){hole.moveTo(getTarget().position());level().addFreshEntity(hole);}}}
 @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return TCSounds.ELDRITCH_IDLE.get();}
 @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return TCSounds.ELDRITCH_DEATH.get();}
 @Override public int getAmbientSoundInterval(){return 100;}
 @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean hit){int count=random.nextInt(6)+looting;if(count>0)spawnAtLocation(new ItemStack(TCItems.material(MaterialType.VOID_SEED),count));super.dropCustomDeathLoot(source,looting,hit);}
}
