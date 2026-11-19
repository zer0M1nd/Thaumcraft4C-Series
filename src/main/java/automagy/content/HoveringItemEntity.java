package automagy.content;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Crafting leftovers stay reachable for ten seconds, then become ordinary falling items. */
public final class HoveringItemEntity extends ItemEntity {
 private int remaining=200;
 public HoveringItemEntity(EntityType<? extends HoveringItemEntity> type,Level level){super(type,level);setNoGravity(true);}
 public int hoverTicks(){return remaining;}
 public static HoveringItemEntity toss(Player player,ItemStack stack,boolean first){
  var item=new HoveringItemEntity(Content.HOVERING_ITEM.get(),player.level());item.setItem(stack.copy());
  item.setPos(player.getX(),player.getY()+Math.max(player.getEyeHeight()*.5,.25),player.getZ());item.setPickUpDelay(1);
  var random=player.getRandom();double yaw=Math.toRadians(player.getYRot()),pitch=Math.toRadians(player.getXRot());
  double angle=random.nextFloat()*Math.PI*2,spread=.02*random.nextFloat();
  double x=-Math.sin(yaw)*Math.cos(pitch)*.3+Math.cos(angle)*spread,z=Math.cos(yaw)*Math.cos(pitch)*.3+Math.sin(angle)*spread;
  if(!first){x=(random.nextFloat()-random.nextFloat())*.1;z=(random.nextFloat()-random.nextFloat())*.1;}
  item.setDeltaMovement(x,-Math.sin(pitch)*.3+.1+(random.nextFloat()-random.nextFloat())*.1,z);
  player.level().addFreshEntity(item);return item;
 }
 @Override public void tick(){
  if(hasPickUpDelay()){if(getDeltaMovement().y>0)setDeltaMovement(getDeltaMovement().add(0,-.04,0));}
  else setDeltaMovement(getDeltaMovement().multiply(.8,.8,.8));
  super.tick();
  if(!level().isClientSide&&isAlive()&&--remaining<=0){
   var ordinary=new ItemEntity(level(),getX(),getY(),getZ(),getItem().copy());ordinary.setDeltaMovement(getDeltaMovement());ordinary.setNoPickUpDelay();
   if(level().addFreshEntity(ordinary))discard();
  }
 }
 @Override public void playerTouch(Player player){if(!level().isClientSide)remaining=Math.max(remaining,200);super.playerTouch(player);}
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("HoverTicks",remaining);}
 @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);remaining=tag.contains("HoverTicks")?Math.max(0,Math.min(200,tag.getInt("HoverTicks"))):200;setNoGravity(true);}
}
